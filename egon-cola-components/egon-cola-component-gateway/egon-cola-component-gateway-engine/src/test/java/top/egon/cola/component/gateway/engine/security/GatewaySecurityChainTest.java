package top.egon.cola.component.gateway.engine.security;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
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
import top.egon.cola.component.gateway.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewayIdentityMapper;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.core.security.SecurityFailureMode;
import top.egon.cola.component.gateway.core.security.TrustedIdentity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GatewaySecurityChainTest {

    @Test
    void requiredChainAuthenticatesAuthorizesAndMapsIdentity() {
        GatewaySecurityChain chain = chain(
                Mono.just(new CredentialExtractionResult(
                        List.of(new GatewayCredential(
                                "bearer",
                                "secret",
                                Map.of()
                        )),
                        Set.of("authorization"),
                        null
                )),
                Mono.just(AuthenticationDecision.allow(principal())),
                Mono.just(AuthorizationDecision.allow())
        );

        StepVerifier.create(chain.execute(
                        exchange(),
                        context(),
                        policy(AuthenticationMode.REQUIRED),
                        GatewayProtocol.HTTP
                ))
                .assertNext(result -> {
                    assertEquals(
                            "user-1",
                            result.context().principal().principalId()
                    );
                    assertEquals(
                            "user-1",
                            result.trustedIdentity().httpHeaders().get(
                                    "X-Egon-Gateway-Principal-Id"
                            )
                    );
                    assertEquals(
                            Set.of("authorization"),
                            result.fieldsToRemove()
                    );
                })
                .verifyComplete();
    }

    @Test
    void requiredWithoutCredentialFailsClosed() {
        GatewaySecurityChain chain = chain(
                Mono.just(CredentialExtractionResult.empty()),
                Mono.just(AuthenticationDecision.abstain()),
                Mono.just(AuthorizationDecision.allow())
        );

        StepVerifier.create(chain.execute(
                        exchange(),
                        context(),
                        policy(AuthenticationMode.REQUIRED),
                        GatewayProtocol.HTTP
                ))
                .expectErrorSatisfies(error -> assertEquals(
                        "GATEWAY_AUTHENTICATION_REQUIRED",
                        ((GatewaySecurityException) error).code()
                ))
                .verify();
    }

    @Test
    void optionalWithoutCredentialUsesAnonymousPrincipal() {
        GatewaySecurityChain chain = chain(
                Mono.just(CredentialExtractionResult.empty()),
                Mono.just(AuthenticationDecision.abstain()),
                Mono.just(AuthorizationDecision.allow())
        );

        StepVerifier.create(chain.execute(
                        exchange(),
                        context(),
                        policy(AuthenticationMode.OPTIONAL),
                        GatewayProtocol.HTTP
                ))
                .assertNext(result -> assertFalse(
                        result.context().principal().authenticated()
                ))
                .verifyComplete();
    }

    @Test
    void authorizationDenyAndProviderTimeoutFailClosed() {
        GatewaySecurityChain denied = chain(
                credential(),
                Mono.just(AuthenticationDecision.allow(principal())),
                Mono.just(AuthorizationDecision.deny("denied"))
        );
        StepVerifier.create(denied.execute(
                        exchange(),
                        context(),
                        policy(AuthenticationMode.REQUIRED),
                        GatewayProtocol.HTTP
                ))
                .expectErrorSatisfies(error -> assertEquals(
                        "GATEWAY_AUTHORIZATION_DENIED",
                        ((GatewaySecurityException) error).code()
                ))
                .verify();

        GatewaySecurityChain timedOut = chain(
                Mono.never(),
                Mono.just(AuthenticationDecision.allow(principal())),
                Mono.just(AuthorizationDecision.allow())
        );
        StepVerifier.create(timedOut.execute(
                        exchange(),
                        context(),
                        new GatewaySecurityPolicy(
                                "security",
                                AuthenticationMode.REQUIRED,
                                List.of("extractor"),
                                List.of("auth"),
                                List.of("authorizer"),
                                AuthorizationDecisionMode.ALL_ALLOW,
                                "mapper",
                                Duration.ofMillis(10),
                                SecurityFailureMode.FAIL_CLOSED
                        ),
                        GatewayProtocol.HTTP
                ))
                .expectErrorSatisfies(error -> assertEquals(
                        "GATEWAY_SECURITY_PROVIDER_TIMEOUT",
                        ((GatewaySecurityException) error).code()
                ))
                .verify();
    }

    private GatewaySecurityChain chain(
            Mono<CredentialExtractionResult> extraction,
            Mono<AuthenticationDecision> authentication,
            Mono<AuthorizationDecision> authorization) {
        GatewayCredentialExtractor extractor =
                new GatewayCredentialExtractor() {
                    @Override
                    public String extractorId() {
                        return "extractor";
                    }

                    @Override
                    public String credentialType() {
                        return "bearer";
                    }

                    @Override
                    public org.reactivestreams.Publisher<
                            CredentialExtractionResult> extract(
                            GatewayExchange exchange,
                            GatewaySecurityPolicy policy) {
                        return extraction;
                    }
                };
        GatewayAuthenticationProvider authenticator =
                new GatewayAuthenticationProvider() {
                    @Override
                    public String providerId() {
                        return "auth";
                    }

                    @Override
                    public Set<String> supportedCredentialTypes() {
                        return Set.of("bearer");
                    }

                    @Override
                    public org.reactivestreams.Publisher<
                            AuthenticationDecision> authenticate(
                            GatewayAuthContext context,
                            GatewayCredential credential) {
                        return authentication;
                    }
                };
        GatewayAuthorizationProvider authorizer =
                new GatewayAuthorizationProvider() {
                    @Override
                    public String providerId() {
                        return "authorizer";
                    }

                    @Override
                    public org.reactivestreams.Publisher<
                            AuthorizationDecision> authorize(
                            GatewayAuthContext context) {
                        return authorization;
                    }
                };
        GatewayIdentityMapper mapper = new GatewayIdentityMapper() {
            @Override
            public String mapperId() {
                return "mapper";
            }

            @Override
            public Set<GatewayProtocol> supportedProtocols() {
                return Set.of(GatewayProtocol.HTTP);
            }

            @Override
            public TrustedIdentity map(GatewayAuthContext context) {
                return new TrustedIdentity(
                        Map.of(
                                "X-Egon-Gateway-Principal-Id",
                                context.principal().principalId()
                        ),
                        Map.of()
                );
            }
        };
        return new GatewaySecurityChain(
                new GatewaySecurityCapabilityRegistry(
                        List.of(extractor),
                        List.of(authenticator),
                        List.of(authorizer),
                        List.of(mapper)
                )
        );
    }

    private Mono<CredentialExtractionResult> credential() {
        return Mono.just(new CredentialExtractionResult(
                List.of(new GatewayCredential(
                        "bearer",
                        "secret",
                        Map.of()
                )),
                Set.of(),
                null
        ));
    }

    private GatewaySecurityPolicy policy(AuthenticationMode mode) {
        return new GatewaySecurityPolicy(
                "security",
                mode,
                List.of("extractor"),
                List.of("auth"),
                List.of("authorizer"),
                AuthorizationDecisionMode.ALL_ALLOW,
                "mapper",
                Duration.ofMillis(100),
                SecurityFailureMode.FAIL_CLOSED
        );
    }

    private GatewayAuthContext context() {
        return new GatewayAuthContext(
                AccessZone.PUBLIC,
                GatewayProtocol.HTTP,
                "orders.create",
                "orders",
                "security",
                "/orders",
                "POST",
                Set.of(),
                GatewayPrincipal.anonymous(),
                "127.0.0.1",
                "trace-123456789012",
                "request-1",
                Instant.now().plusSeconds(1),
                "release-1"
        );
    }

    private GatewayPrincipal principal() {
        return new GatewayPrincipal(
                "user-1",
                "USER",
                "tenant-1",
                null,
                true,
                Map.of()
        );
    }

    private GatewayExchange exchange() {
        return new GatewayExchange() {
            @Override
            public top.egon.cola.component.gateway.core.exchange.GatewayRequest
            request() {
                return null;
            }

            @Override
            public top.egon.cola.component.gateway.core.context.GatewayContext
            context() {
                return null;
            }

            @Override
            public top.egon.cola.component.gateway.core.exchange.GatewayResponse
            response() {
                return null;
            }
        };
    }
}
