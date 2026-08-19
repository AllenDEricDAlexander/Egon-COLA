package top.egon.cola.component.gateway.engine.common.security.service;

import top.egon.cola.component.gateway.engine.common.security.domain.GatewaySecurityException;
import top.egon.cola.component.gateway.engine.common.security.domain.GatewaySecurityResult;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.security.AuthenticationDecision;
import top.egon.cola.component.gateway.core.security.AuthenticationFailure;
import top.egon.cola.component.gateway.core.security.AuthenticationMode;
import top.egon.cola.component.gateway.core.security.AuthorizationDecision;
import top.egon.cola.component.gateway.core.security.AuthorizationDecisionMode;
import top.egon.cola.component.gateway.core.security.CredentialExtractionResult;
import top.egon.cola.component.gateway.core.security.CredentialForwardingMode;
import top.egon.cola.component.gateway.core.security.CredentialRecoveryResult;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.core.security.SecurityDecision;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * 中文说明：{@code GatewaySecurityChain} 是类型，位于当前 Gateway 模块的相关包中，负责网关安全Chain相关的职责与边界。
 * English summary: {@code GatewaySecurityChain} is a type in the current Gateway module; it owns the gateway security chain-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewaySecurityChain {

    /**
     * 中文说明：保存 capabilities 对应的状态、依赖或配置值；字段类型为 {@code GatewaySecurityCapabilityRegistry}，由 {@code GatewaySecurityChain} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by capabilities; its type is {@code GatewaySecurityCapabilityRegistry}, and {@code GatewaySecurityChain} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewaySecurityChain} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecurityChain}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewaySecurityCapabilityRegistry capabilities;

    /**
     * 中文说明：创建 {@code GatewaySecurityChain} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewaySecurityChain} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param capabilities 参数 capabilities；parameter capabilities。
     */
    public GatewaySecurityChain(
            GatewaySecurityCapabilityRegistry capabilities) {
        this.capabilities = Objects.requireNonNull(
                capabilities,
                "capabilities"
        );
    }

    /**
     * 中文说明：执行 execute 操作；该方法是 {@code GatewaySecurityChain} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the execute operation; this method is the invocation entry point on {@code GatewaySecurityChain} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityChain.execute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param exchange 参数 exchange；parameter exchange。
     * @param initialContext 参数 initialContext；parameter initial context。
     * @param policy 参数 策略；parameter policy。
     * @param protocol 参数 protocol；parameter protocol。
     * @return 返回 execute 的处理结果；returns the result of the operation.
     */
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
                    Set.of(),
                    null,
                    Map.of()
            )
                    : extract(exchange, initialContext, policy)
                    .flatMap(extraction -> authenticate(
                            initialContext,
                            policy,
                            extraction,
                            exchange,
                            false,
                            Map.of()
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

    /**
     * 中文说明：执行 extract 操作；该方法是 {@code GatewaySecurityChain} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the extract operation; this method is the invocation entry point on {@code GatewaySecurityChain} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityChain.extract(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param exchange 参数 exchange；parameter exchange。
     * @param policy 参数 策略；parameter policy。
     * @return 返回 extract 的处理结果；returns the result of the operation.
     */
    private Mono<Extraction> extract(
            GatewayExchange exchange,
            GatewayAuthContext context,
            GatewaySecurityPolicy policy) {
        return Flux.fromIterable(policy.credentialExtractorIds())
                .concatMap(id -> Mono.from(capabilities.extractor(id).extract(
                        exchange,
                        context,
                        policy
                )).switchIfEmpty(Mono.error(
                        GatewaySecurityException.providerError()
                )))
                .collectList()
                .map(this::mergeExtraction);
    }

    /**
     * 中文说明：执行 mergeExtraction 操作；该方法是 {@code GatewaySecurityChain} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the merge extraction operation; this method is the invocation entry point on {@code GatewaySecurityChain} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityChain.mergeExtraction(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param results 参数 results；parameter results。
     * @return 返回 mergeExtraction 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 authenticate 操作；该方法是 {@code GatewaySecurityChain} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authenticate operation; this method is the invocation entry point on {@code GatewaySecurityChain} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityChain.authenticate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param initialContext 参数 initialContext；parameter initial context。
     * @param policy 参数 策略；parameter policy。
     * @param extraction 参数 extraction；parameter extraction。
     * @return 返回 authenticate 的处理结果；returns the result of the operation.
     */
    private Mono<GatewaySecurityResult> authenticate(
            GatewayAuthContext initialContext,
            GatewaySecurityPolicy policy,
            Extraction extraction,
            GatewayExchange exchange,
            boolean recoveryAttempt,
            Map<String, List<String>> responseHeaders) {
        if (extraction.credentials().isEmpty()) {
            if (policy.authenticationMode()
                    == AuthenticationMode.REQUIRED) {
                return recoverOrFail(
                        initialContext,
                        policy,
                        exchange,
                        AuthenticationFailure.MISSING,
                        extraction.fieldsToRemove(),
                        recoveryAttempt
                );
            }
            return authorizeAndMap(
                    initialContext.withPrincipal(
                            GatewayPrincipal.anonymous()
                    ),
                    policy,
                    extraction.fieldsToRemove(),
                    null,
                    responseHeaders
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
                initialContext.releaseId(),
                initialContext.attributes()
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
                        .flatMap(principal -> {
                            GatewayAuthContext authenticated =
                                    credentialContext.withPrincipal(principal);
                            return validateOnlineState(
                                    authenticated, policy, exchange)
                                    .then(authorizeAndMap(
                                            authenticated,
                                            policy,
                                            extraction.fieldsToRemove(),
                                            forwardable(
                                                    policy,
                                                    extraction.credentials()),
                                            responseHeaders
                                    ));
                        }))
                .onErrorResume(
                        AuthenticationFailureException.class,
                        failure -> recoverOrFail(
                                initialContext,
                                policy,
                                exchange,
                                failure.failure,
                                extraction.fieldsToRemove(),
                                recoveryAttempt
                        ));

    }

    private Mono<Void> validateOnlineState(
            GatewayAuthContext context,
            GatewaySecurityPolicy policy,
            GatewayExchange exchange) {
        if (!"USER".equalsIgnoreCase(context.principal().principalType())
                || policy.credentialRecoveryProviderId() == null) {
            return Mono.empty();
        }
        return Mono.from(capabilities.recovery(
                        policy.credentialRecoveryProviderId())
                .validateAuthenticated(context, exchange))
                .switchIfEmpty(Mono.error(
                        GatewaySecurityException.providerError()))
                .flatMap(result -> switch (result.outcome()) {
                    case ACTIVE -> Mono.empty();
                    case INACTIVE -> Mono.error(
                            GatewaySecurityException.authenticationFailed(
                                    result.responseHeaders()));
                    case UNAVAILABLE -> Mono.error(
                            GatewaySecurityException.providerError());
                });
    }

    private Mono<GatewaySecurityResult> recoverOrFail(
            GatewayAuthContext context,
            GatewaySecurityPolicy policy,
            GatewayExchange exchange,
            AuthenticationFailure failure,
            Set<String> fieldsToRemove,
            boolean recoveryAttempt) {
        if (recoveryAttempt
                || policy.credentialRecoveryProviderId() == null
                || (failure != AuthenticationFailure.MISSING
                && failure != AuthenticationFailure.EXPIRED)) {
            return Mono.error(failure == AuthenticationFailure.MISSING
                    ? GatewaySecurityException.authenticationRequired()
                    : GatewaySecurityException.authenticationFailed());
        }
        return Mono.from(capabilities.recovery(
                        policy.credentialRecoveryProviderId()).recover(
                        context,
                        exchange,
                        failure
                ))
                .switchIfEmpty(Mono.just(CredentialRecoveryResult.failed()))
                .flatMap(result -> {
                    if (result.outcome()
                            == CredentialRecoveryResult.Outcome.FAILED) {
                        return Mono.error(GatewaySecurityException.providerError());
                    }
                    if (result.outcome()
                            != CredentialRecoveryResult.Outcome.RECOVERED) {
                        return Mono.error(GatewaySecurityException.authenticationFailed(
                                result.responseHeaders()));
                    }
                    Set<String> removals = new LinkedHashSet<>(fieldsToRemove);
                    removals.addAll(result.fieldsToRemove());
                    return authenticate(
                            context,
                            policy,
                            new Extraction(
                                    List.of(result.credential()),
                                    Set.copyOf(removals)
                            ),
                            exchange,
                            true,
                            result.responseHeaders()
                    );
                })
                .onErrorMap(
                        error -> !(error instanceof GatewaySecurityException),
                        error -> GatewaySecurityException.providerError());
    }

    /**
     * 中文说明：执行 forwardable 操作；该方法是 {@code GatewaySecurityChain} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the forwardable operation; this method is the invocation entry point on {@code GatewaySecurityChain} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityChain.forwardable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     * @param credentials 参数 credentials；parameter credentials。
     * @return 返回 forwardable 的处理结果；returns the result of the operation.
     */
    private GatewayCredential forwardable(
            GatewaySecurityPolicy policy,
            List<GatewayCredential> credentials
    ) {
        if (policy.credentialForwardingMode()
                != CredentialForwardingMode.ORIGINAL_BEARER
                || credentials.size() != 1
                || !"bearer".equalsIgnoreCase(credentials.getFirst().type())) {
            return null;
        }
        return credentials.getFirst();
    }

    /**
     * 中文说明：执行 authenticatedPrincipal 操作；该方法是 {@code GatewaySecurityChain} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authenticated principal operation; this method is the invocation entry point on {@code GatewaySecurityChain} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityChain.authenticatedPrincipal(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param decisions 参数 decisions；parameter decisions。
     * @return 返回 authenticatedPrincipal 的处理结果；returns the result of the operation.
     */
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
                return Mono.error(new AuthenticationFailureException(
                        decision.failure()));
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

    /**
     * 中文说明：执行 authorizeAndMap 操作；该方法是 {@code GatewaySecurityChain} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the authorize and map operation; this method is the invocation entry point on {@code GatewaySecurityChain} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityChain.authorizeAndMap(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @param policy 参数 策略；parameter policy。
     * @param removals 参数 removals；parameter removals。
     * @param forwardingCredential 参数 forwarding凭证；parameter forwarding credential。
     * @return 返回 authorizeAndMap 的处理结果；returns the result of the operation.
     */
    private Mono<GatewaySecurityResult> authorizeAndMap(
            GatewayAuthContext context,
            GatewaySecurityPolicy policy,
            Set<String> removals,
            GatewayCredential forwardingCredential,
            Map<String, List<String>> responseHeaders) {
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
                                    removals,
                                    forwardingCredential,
                                    responseHeaders
                                    , policy.routeSecurityType()
                            ));
                });
    }

    /**
     * 中文说明：执行 validate授权 操作；该方法是 {@code GatewaySecurityChain} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate authorization operation; this method is the invocation entry point on {@code GatewaySecurityChain} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityChain.validateAuthorization(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param mode 参数 mode；parameter mode。
     * @param decisions 参数 decisions；parameter decisions。
     */
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

    /**
     * 中文说明：执行 身份 操作；该方法是 {@code GatewaySecurityChain} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the identity operation; this method is the invocation entry point on {@code GatewaySecurityChain} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityChain.identity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @param policy 参数 策略；parameter policy。
     * @return 返回 身份 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 effective超时 操作；该方法是 {@code GatewaySecurityChain} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the effective timeout operation; this method is the invocation entry point on {@code GatewaySecurityChain} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewaySecurityChain.effectiveTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policyTimeout 参数 策略超时；parameter policy timeout。
     * @param deadline 参数 deadline；parameter deadline。
     * @return 返回 effective超时 的处理结果；returns the result of the operation.
     */
    private Duration effectiveTimeout(
            Duration policyTimeout,
            Instant deadline) {
        Duration remaining = Duration.between(Instant.now(), deadline);
        return remaining.compareTo(policyTimeout) < 0
                ? remaining
                : policyTimeout;
    }

    /**
     * 中文说明：{@code Extraction} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Extraction相关的职责与边界。
     * English summary: {@code Extraction} is an immutable data carrier in the current Gateway module; it owns the extraction-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param credentials 参数 credentials；parameter credentials。
     * @param fieldsToRemove 参数 fieldsToRemove；parameter fields to remove。
     */
    private record Extraction(
            /**
             * 中文说明：保存 credentials 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayCredential>}，由 {@code GatewaySecurityChain.Extraction} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by credentials; its type is {@code List<GatewayCredential>}, and {@code GatewaySecurityChain.Extraction} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewaySecurityChain.Extraction} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecurityChain.Extraction}; do not couple callers to its representation when the owning type exposes an API.
             */
            List<GatewayCredential> credentials,
            /**
             * 中文说明：保存 fieldsToRemove 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code GatewaySecurityChain.Extraction} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by fields to remove; its type is {@code Set<String>}, and {@code GatewaySecurityChain.Extraction} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewaySecurityChain.Extraction} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewaySecurityChain.Extraction}; do not couple callers to its representation when the owning type exposes an API.
             */
            Set<String> fieldsToRemove
    ) {
    }

    private static final class AuthenticationFailureException
            extends RuntimeException {

        private final AuthenticationFailure failure;

        private AuthenticationFailureException(AuthenticationFailure failure) {
            this.failure = failure == null
                    ? AuthenticationFailure.INVALID
                    : failure;
        }
    }
}
