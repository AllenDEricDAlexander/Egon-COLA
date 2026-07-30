package top.egon.cola.component.gateway.engine.security;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.security.AuthenticationDecision;
import top.egon.cola.component.gateway.core.security.AuthenticationMode;
import top.egon.cola.component.gateway.core.security.AuthorizationDecision;
import top.egon.cola.component.gateway.core.security.AuthorizationDecisionMode;
import top.egon.cola.component.gateway.core.security.CredentialExtractionResult;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.core.security.SecurityDecision;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public final class GatewaySecurityChain {

    private final GatewaySecurityCapabilityRegistry capabilities;

    public GatewaySecurityChain(
            GatewaySecurityCapabilityRegistry capabilities) {
        this.capabilities = Objects.requireNonNull(
                capabilities,
                "capabilities"
        );
    }

    public Mono<GatewaySecurityResult> execute(
            GatewayExchange exchange,
            GatewayAuthContext initialContext,
            GatewaySecurityPolicy policy,
            GatewayProtocol protocol) {
        Objects.requireNonNull(exchange, "exchange");
        Objects.requireNonNull(initialContext, "initialContext");
        Objects.requireNonNull(policy, "policy");
        Duration timeout = effectiveTimeout(
                policy.providerTimeout(),
                initialContext.deadline()
        );
        if (timeout.isZero() || timeout.isNegative()) {
            return Mono.error(GatewaySecurityException.providerTimeout());
        }
        Mono<GatewaySecurityResult> execution = Mono.defer(() -> {
            capabilities.validate(policy, Set.of(protocol));
            return policy.authenticationMode() == AuthenticationMode.NONE
                    ? authorizeAndMap(
                    initialContext.withPrincipal(
                            GatewayPrincipal.anonymous()
                    ),
                    policy,
                    Set.of()
            )
                    : extract(exchange, policy)
                    .flatMap(extraction -> authenticate(
                            initialContext,
                            policy,
                            extraction
                    ));
        });
        return execution.timeout(timeout)
                .onErrorMap(
                        TimeoutException.class,
                        ignored -> GatewaySecurityException.providerTimeout()
                )
                .onErrorMap(
                        error -> !(error instanceof GatewaySecurityException),
                        error -> GatewaySecurityException.providerError()
                );
    }

    private Mono<Extraction> extract(
            GatewayExchange exchange,
            GatewaySecurityPolicy policy) {
        return Flux.fromIterable(policy.credentialExtractorIds())
                .concatMap(id -> Mono.from(capabilities.extractor(id).extract(
                        exchange,
                        policy
                )).switchIfEmpty(Mono.error(
                        GatewaySecurityException.providerError()
                )))
                .collectList()
                .map(this::mergeExtraction);
    }

    private Extraction mergeExtraction(
            List<CredentialExtractionResult> results) {
        List<GatewayCredential> credentials = new ArrayList<>();
        Set<String> removals = new LinkedHashSet<>();
        Set<String> credentialTypes = new LinkedHashSet<>();
        for (CredentialExtractionResult result : results) {
            if (!result.valid()) {
                throw GatewaySecurityException.credentialInvalid();
            }
            for (GatewayCredential credential : result.credentials()) {
                if (!credentialTypes.add(credential.type())) {
                    throw GatewaySecurityException.credentialInvalid();
                }
                credentials.add(credential);
            }
            removals.addAll(result.fieldsToRemove());
        }
        return new Extraction(
                List.copyOf(credentials),
                Set.copyOf(removals)
        );
    }

    private Mono<GatewaySecurityResult> authenticate(
            GatewayAuthContext initialContext,
            GatewaySecurityPolicy policy,
            Extraction extraction) {
        if (extraction.credentials().isEmpty()) {
            if (policy.authenticationMode()
                    == AuthenticationMode.REQUIRED) {
                return Mono.error(
                        GatewaySecurityException.authenticationRequired()
                );
            }
            return authorizeAndMap(
                    initialContext.withPrincipal(
                            GatewayPrincipal.anonymous()
                    ),
                    policy,
                    extraction.fieldsToRemove()
            );
        }
        Set<String> types = extraction.credentials().stream()
                .map(GatewayCredential::type)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        GatewayAuthContext credentialContext = new GatewayAuthContext(
                initialContext.accessZone(),
                initialContext.protocol(),
                initialContext.operationId(),
                initialContext.routeId(),
                initialContext.policyId(),
                initialContext.requestTarget(),
                initialContext.method(),
                types,
                GatewayPrincipal.anonymous(),
                initialContext.remoteAddress(),
                initialContext.traceId(),
                initialContext.requestId(),
                initialContext.deadline(),
                initialContext.releaseId()
        );
        return Flux.fromIterable(extraction.credentials())
                .concatMap(credential -> Flux.fromIterable(
                        policy.authenticationProviderIds()
                ).map(capabilities::authentication)
                        .filter(provider -> provider
                                .supportedCredentialTypes()
                                .contains(credential.type()))
                        .concatMap(provider -> Mono.from(
                                provider.authenticate(
                                        credentialContext,
                                        credential
                                )
                        ).switchIfEmpty(Mono.error(
                                GatewaySecurityException.providerError()
                        ))))
                .collectList()
                .flatMap(decisions -> authenticatedPrincipal(decisions)
                        .flatMap(principal -> authorizeAndMap(
                                credentialContext.withPrincipal(principal),
                                policy,
                                extraction.fieldsToRemove()
                        )));
    }

    private Mono<GatewayPrincipal> authenticatedPrincipal(
            List<AuthenticationDecision> decisions) {
        GatewayPrincipal principal = null;
        for (AuthenticationDecision decision : decisions) {
            if (decision.decision() == SecurityDecision.ERROR) {
                return Mono.error(
                        GatewaySecurityException.providerError()
                );
            }
            if (decision.decision() == SecurityDecision.DENY) {
                return Mono.error(
                        GatewaySecurityException.authenticationFailed()
                );
            }
            if (decision.decision() == SecurityDecision.ALLOW) {
                if (principal != null
                        && !principal.principalId().equals(
                        decision.principal().principalId()
                )) {
                    return Mono.error(
                            GatewaySecurityException.providerError()
                    );
                }
                principal = decision.principal();
            }
        }
        return principal == null
                ? Mono.error(
                GatewaySecurityException.authenticationFailed()
        )
                : Mono.just(principal);
    }

    private Mono<GatewaySecurityResult> authorizeAndMap(
            GatewayAuthContext context,
            GatewaySecurityPolicy policy,
            Set<String> removals) {
        return Flux.fromIterable(policy.authorizationProviderIds())
                .concatMap(id -> Mono.from(
                        capabilities.authorization(id).authorize(context)
                ).switchIfEmpty(Mono.error(
                        GatewaySecurityException.providerError()
                )))
                .collectList()
                .flatMap(decisions -> {
                    validateAuthorization(policy.decisionMode(), decisions);
                    return identity(context, policy)
                            .map(identity -> new GatewaySecurityResult(
                                    context,
                                    identity,
                                    removals
                            ));
                });
    }

    private void validateAuthorization(
            AuthorizationDecisionMode mode,
            List<AuthorizationDecision> decisions) {
        if (decisions.stream().anyMatch(
                decision -> decision.decision() == SecurityDecision.ERROR
        )) {
            throw GatewaySecurityException.providerError();
        }
        if (decisions.stream().anyMatch(
                decision -> decision.decision() == SecurityDecision.DENY
        )) {
            throw GatewaySecurityException.authorizationDenied();
        }
        if (decisions.isEmpty()) {
            return;
        }
        long allows = decisions.stream()
                .filter(decision -> decision.decision()
                        == SecurityDecision.ALLOW)
                .count();
        boolean allowed = mode == AuthorizationDecisionMode.ALL_ALLOW
                ? allows == decisions.size()
                : allows > 0;
        if (!allowed) {
            throw GatewaySecurityException.authorizationDenied();
        }
    }

    private Mono<TrustedIdentity> identity(
            GatewayAuthContext context,
            GatewaySecurityPolicy policy) {
        if (policy.identityMapperId() == null) {
            return Mono.just(TrustedIdentity.empty());
        }
        return Mono.fromCallable(() -> capabilities.identityMapper(
                        policy.identityMapperId()
                ).map(context))
                .switchIfEmpty(Mono.error(
                        GatewaySecurityException.identityMappingFailed()
                ))
                .onErrorMap(
                        error -> !(error instanceof GatewaySecurityException),
                        error -> GatewaySecurityException
                                .identityMappingFailed()
                );
    }

    private Duration effectiveTimeout(
            Duration policyTimeout,
            Instant deadline) {
        Duration remaining = Duration.between(Instant.now(), deadline);
        return remaining.compareTo(policyTimeout) < 0
                ? remaining
                : policyTimeout;
    }

    private record Extraction(
            List<GatewayCredential> credentials,
            Set<String> fieldsToRemove
    ) {
    }
}
