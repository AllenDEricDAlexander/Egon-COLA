package top.egon.cola.platform.rbac3.gateway.security;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.exchange.DefaultGatewayResponse;
import top.egon.cola.component.gateway.core.exchange.EmptyGatewayBody;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.exchange.GatewayRequest;
import top.egon.cola.component.gateway.core.exchange.ImmutableGatewayHeaders;
import top.egon.cola.component.gateway.core.security.AuthenticationMode;
import top.egon.cola.component.gateway.core.security.AuthorizationDecision;
import top.egon.cola.component.gateway.core.security.AuthorizationDecisionMode;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.core.security.SecurityDecision;
import top.egon.cola.component.gateway.core.security.SecurityFailureMode;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class Rbac3GatewaySecurityProviderTest {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");

    @Test
    void extractsExactlyOneHeaderBearerAndRejectsDuplicates() {
        var extractor = new Rbac3BearerCredentialExtractor(
                new Rbac3ReservedHeaderSanitizer());

        StepVerifier.create(Mono.from(extractor.extract(
                        exchange(Map.of("Authorization", List.of("Bearer exact-token"))),
                        policy())))
                .assertNext(result -> {
                    assertThat(result.valid()).isTrue();
                    assertThat(result.credentials()).singleElement()
                            .extracting(GatewayCredential::tokenReference)
                            .isEqualTo("exact-token");
                    assertThat(result.fieldsToRemove()).contains("authorization");
                }).verifyComplete();

        StepVerifier.create(Mono.from(extractor.extract(
                        exchange(Map.of("authorization", List.of(
                                "Bearer first", "Bearer second"))), policy())))
                .assertNext(result -> assertThat(result.valid()).isFalse())
                .verifyComplete();
    }

    @Test
    void authenticationMapsOnlyVerifiedIdentityAndVersions() {
        Rbac3TokenClaims claims = claims();
        var provider = new Rbac3JwtSessionAuthenticationProvider(
                token -> {
                    assertThat(token).isEqualTo("signed-token");
                    return claims;
                },
                verified -> assertThat(verified).isSameAs(claims));

        StepVerifier.create(Mono.from(provider.authenticate(
                        context(GatewayPrincipal.anonymous()),
                        new GatewayCredential("bearer", "signed-token", Map.of()))))
                .assertNext(decision -> {
                    assertThat(decision.decision()).isEqualTo(SecurityDecision.ALLOW);
                    assertThat(decision.principal().tenantId()).isEqualTo("7");
                    assertThat(decision.principal().principalId()).isEqualTo("9");
                    assertThat(decision.principal().attributes())
                            .containsEntry("rbac3.session-id", "99")
                            .containsEntry("rbac3.auth-version", "3")
                            .containsEntry("rbac3.session-version", "4")
                            .containsEntry("rbac3.policy-version", "5")
                            .doesNotContainKeys("permissions", "roles", "token");
                }).verifyComplete();
    }

    @Test
    void authorizationAndTrustedIdentityRemainCoarseGrained() {
        var authorization = new Rbac3PermissionAuthorizationProvider(
                context -> AuthorizationDecision.allow());
        GatewayPrincipal principal = new GatewayPrincipal(
                "9", "USER", "7", null, true, Map.of(
                "rbac3.session-id", "99",
                "rbac3.auth-version", "3",
                "rbac3.session-version", "4",
                "rbac3.policy-version", "5"));

        StepVerifier.create(Mono.from(authorization.authorize(context(principal))))
                .expectNext(AuthorizationDecision.allow())
                .verifyComplete();

        var identity = new Rbac3TrustedIdentityMapper().map(context(principal));
        assertThat(identity.httpHeaders()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "x-egon-gateway-tenant-id", "7",
                "x-egon-gateway-user-id", "9",
                "x-egon-gateway-session-id", "99",
                "x-egon-gateway-auth-version", "3",
                "x-egon-gateway-session-version", "4",
                "x-egon-gateway-policy-version", "5",
                "x-egon-gateway-trace-id", "trace-1"));
        assertThat(identity.toString()).doesNotContain("permission", "role", "token");
    }

    private GatewaySecurityPolicy policy() {
        return new GatewaySecurityPolicy(
                "security", AuthenticationMode.REQUIRED, List.of("rbac3-bearer"),
                List.of("rbac3-jwt-session"), List.of("rbac3-permission"),
                AuthorizationDecisionMode.ALL_ALLOW, "rbac3-trusted-identity",
                Duration.ofSeconds(1), SecurityFailureMode.FAIL_CLOSED);
    }

    private GatewayAuthContext context(GatewayPrincipal principal) {
        return new GatewayAuthContext(
                AccessZone.PUBLIC, GatewayProtocol.HTTP, "operation-1", "route-1",
                "security", "/orders", "GET", Set.of("bearer"), principal,
                "127.0.0.1", "trace-1", "request-1", NOW.plusSeconds(5),
                "release-1");
    }

    private Rbac3TokenClaims claims() {
        return new Rbac3TokenClaims(
                "issuer", List.of("orders"), "9", "7", "99",
                3, 4, 5, "jti-1", NOW.minusSeconds(1), NOW.minusSeconds(1),
                NOW.plusSeconds(300), "kid-1");
    }

    private GatewayExchange exchange(Map<String, List<String>> headers) {
        GatewayRequest request = new GatewayRequest() {
            public String requestId() { return "request-1"; }
            public String traceId() { return "trace-1"; }
            public GatewayProtocol protocol() { return GatewayProtocol.HTTP; }
            public AccessZone accessZone() { return AccessZone.PUBLIC; }
            public top.egon.cola.component.gateway.core.exchange.GatewayHeaders headers() {
                return new ImmutableGatewayHeaders(headers);
            }
            public top.egon.cola.component.gateway.core.exchange.GatewayBody body() {
                return EmptyGatewayBody.INSTANCE;
            }
        };
        return new GatewayExchange() {
            public GatewayRequest request() { return request; }
            public top.egon.cola.component.gateway.core.context.GatewayContext context() {
                return null;
            }
            public top.egon.cola.component.gateway.core.exchange.GatewayResponse response() {
                return DefaultGatewayResponse.success(EmptyGatewayBody.INSTANCE);
            }
        };
    }
}
