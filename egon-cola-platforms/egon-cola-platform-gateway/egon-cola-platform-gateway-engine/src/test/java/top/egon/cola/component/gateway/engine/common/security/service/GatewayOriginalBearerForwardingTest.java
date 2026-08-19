package top.egon.cola.component.gateway.engine.common.security.service;

import top.egon.cola.component.gateway.engine.common.security.domain.GatewaySecurityException;

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
import top.egon.cola.component.gateway.core.security.CredentialForwardingMode;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayAuthorizationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.GatewayCredentialExtractor;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.core.security.SecurityFailureMode;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayOriginalBearerForwardingTest {

    private static final String TOKEN = "header.payload.signature";

    @Test
    void exposesTheOriginalBearerOnlyAfterAuthenticationAndAuthorizationAllow() {
        GatewaySecurityChain chain = chain(AuthorizationDecision.allow());

        StepVerifier.create(chain.execute(
                        exchange(), context(), policy(CredentialForwardingMode.ORIGINAL_BEARER),
                        GatewayProtocol.HTTP))
                .assertNext(result -> {
                    assertNotNull(result.forwardingCredential());
                    assertEquals(TOKEN,
                            result.forwardingCredential().tokenReference());
                    assertFalse(result.toString().contains(TOKEN));
                    assertTrue(result.toString().contains("REDACTED"));
                })
                .verifyComplete();
    }

    @Test
    void defaultNoneNeverExposesTheAuthenticatedBearer() {
        StepVerifier.create(chain(AuthorizationDecision.allow()).execute(
                        exchange(), context(), policy(CredentialForwardingMode.NONE),
                        GatewayProtocol.HTTP))
                .assertNext(result -> assertNull(result.forwardingCredential()))
                .verifyComplete();
    }

    @Test
    void denialNeverProducesAForwardableCredential() {
        StepVerifier.create(chain(AuthorizationDecision.deny("denied")).execute(
                        exchange(), context(), policy(CredentialForwardingMode.ORIGINAL_BEARER),
                        GatewayProtocol.HTTP))
                .expectError(GatewaySecurityException.class)
                .verify();
    }

    @Test
    void optionalAnonymousAndMultipleCredentialsNeverForwardAuthorization() {
        GatewaySecurityChain anonymous = chain(
                CredentialExtractionResult.empty(),
                AuthenticationDecision.abstain(),
                AuthorizationDecision.allow());
        StepVerifier.create(anonymous.execute(
                        exchange(), context(),
                        policy(AuthenticationMode.OPTIONAL,
                                CredentialForwardingMode.ORIGINAL_BEARER),
                        GatewayProtocol.HTTP))
                .assertNext(result -> assertNull(result.forwardingCredential()))
                .verifyComplete();

        GatewaySecurityChain multiple = chain(
                new CredentialExtractionResult(List.of(
                        new GatewayCredential("bearer", TOKEN, Map.of()),
                        new GatewayCredential("api-key", "key-reference", Map.of())),
                        Set.of("authorization", "x-api-key"), null),
                AuthenticationDecision.allow(principal()),
                AuthorizationDecision.allow());
        StepVerifier.create(multiple.execute(
                        exchange(), context(),
                        policy(CredentialForwardingMode.ORIGINAL_BEARER),
                        GatewayProtocol.HTTP))
                .assertNext(result -> assertNull(result.forwardingCredential()))
                .verifyComplete();
    }

    @Test
    void abstainErrorAndAuthenticationFailureCannotReachForwarding() {
        StepVerifier.create(chain(AuthorizationDecision.abstain()).execute(
                        exchange(), context(),
                        policy(CredentialForwardingMode.ORIGINAL_BEARER),
                        GatewayProtocol.HTTP))
                .expectError(GatewaySecurityException.class)
                .verify();
        StepVerifier.create(chain(AuthorizationDecision.error("unavailable")).execute(
                        exchange(), context(),
                        policy(CredentialForwardingMode.ORIGINAL_BEARER),
                        GatewayProtocol.HTTP))
                .expectError(GatewaySecurityException.class)
                .verify();
        GatewaySecurityChain authenticationDenied = chain(
                credential(), AuthenticationDecision.deny("invalid"),
                AuthorizationDecision.allow());
        StepVerifier.create(authenticationDenied.execute(
                        exchange(), context(),
                        policy(CredentialForwardingMode.ORIGINAL_BEARER),
                        GatewayProtocol.HTTP))
                .expectError(GatewaySecurityException.class)
                .verify();
    }

    private GatewaySecurityChain chain(AuthorizationDecision authorization) {
        return chain(
                credential(),
                AuthenticationDecision.allow(principal()),
                authorization);
    }

    private CredentialExtractionResult credential() {
        return new CredentialExtractionResult(
                List.of(new GatewayCredential("bearer", TOKEN, Map.of())),
                Set.of("authorization"), null);
    }

    private GatewayPrincipal principal() {
        return new GatewayPrincipal(
                "9", "USER", "7", null, true, Map.of());
    }

    private GatewaySecurityChain chain(
            CredentialExtractionResult extraction,
            AuthenticationDecision authenticationDecision,
            AuthorizationDecision authorization
    ) {
        GatewayCredentialExtractor extractor = new GatewayCredentialExtractor() {
            public String extractorId() { return "extractor"; }
            public String credentialType() { return "bearer"; }
            public org.reactivestreams.Publisher<CredentialExtractionResult> extract(
                    GatewayExchange exchange, GatewaySecurityPolicy policy) {
                return Mono.just(extraction);
            }
        };
        GatewayAuthenticationProvider authentication = new GatewayAuthenticationProvider() {
            public String providerId() { return "authentication"; }
            public Set<String> supportedCredentialTypes() {
                return Set.of("bearer", "api-key");
            }
            public org.reactivestreams.Publisher<AuthenticationDecision> authenticate(
                    GatewayAuthContext context, GatewayCredential credential) {
                return Mono.just(authenticationDecision);
            }
        };
        GatewayAuthorizationProvider authorizer = new GatewayAuthorizationProvider() {
            public String providerId() { return "authorization"; }
            public org.reactivestreams.Publisher<AuthorizationDecision> authorize(
                    GatewayAuthContext context) { return Mono.just(authorization); }
        };
        return new GatewaySecurityChain(new GatewaySecurityCapabilityRegistry(
                List.of(extractor), List.of(authentication),
                List.of(authorizer), List.of()));
    }

    private GatewaySecurityPolicy policy(CredentialForwardingMode mode) {
        return policy(AuthenticationMode.REQUIRED, mode);
    }

    private GatewaySecurityPolicy policy(
            AuthenticationMode authenticationMode,
            CredentialForwardingMode mode
    ) {
        return new GatewaySecurityPolicy(
                "security", authenticationMode,
                List.of("extractor"), List.of("authentication"),
                List.of("authorization"), AuthorizationDecisionMode.ALL_ALLOW,
                null, Duration.ofSeconds(1), SecurityFailureMode.FAIL_CLOSED, mode);
    }

    private GatewayAuthContext context() {
        return new GatewayAuthContext(
                AccessZone.PUBLIC, GatewayProtocol.HTTP, "operation-1", "route-1",
                "security", "/orders", "GET", Set.of(),
                GatewayPrincipal.anonymous(), "127.0.0.1", "trace-1", "request-1",
                Instant.now().plusSeconds(5), "release-1");
    }

    private GatewayExchange exchange() {
        return new GatewayExchange() {
            public top.egon.cola.component.gateway.core.exchange.GatewayRequest request() {
                return null;
            }
            public top.egon.cola.component.gateway.core.context.GatewayContext context() {
                return null;
            }
            public top.egon.cola.component.gateway.core.exchange.GatewayResponse response() {
                return null;
            }
        };
    }
}
