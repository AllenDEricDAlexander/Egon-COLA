package top.egon.cola.platform.idp.gateway.security;

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
import top.egon.cola.component.gateway.core.security.AuthorizationDecisionMode;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.core.security.SecurityDecision;
import top.egon.cola.component.gateway.core.security.SecurityFailureMode;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IdpGatewaySecurityProviderTest {

    private static final Instant NOW = Instant.parse("2026-08-02T08:00:00Z");

    @Test
    void extractsOneBearerAndRemovesEverySpoofableIdentityHeader() {
        var extractor = new IdpBearerCredentialExtractor(
                new IdpReservedHeaderSanitizer());

        StepVerifier.create(Mono.from(extractor.extract(exchange(Map.of(
                                "Authorization", List.of("Bearer exact-token"),
                                "X-Egon-Identity-Sub", List.of("mallory"))),
                        policy())))
                .assertNext(result -> {
                    assertThat(result.valid()).isTrue();
                    assertThat(result.credentials()).singleElement()
                            .extracting(GatewayCredential::tokenReference)
                            .isEqualTo("exact-token");
                    assertThat(result.fieldsToRemove()).contains(
                            "authorization",
                            "x-egon-identity-sub",
                            "x-egon-tenant-id",
                            "x-egon-session-id",
                            "x-egon-client-id",
                            "x-egon-token-id");
                })
                .verifyComplete();
    }

    @Test
    void authenticatesOnlyIdentityClaimsAndMapsFixedTrustedHeaders() {
        var provider = new IdpIdentityAuthenticationProvider(token -> {
            assertThat(token).isEqualTo("signed-token");
            return principal();
        });

        StepVerifier.create(Mono.from(provider.authenticate(
                        context(GatewayPrincipal.anonymous()),
                        new GatewayCredential("bearer", "signed-token", Map.of()))))
                .assertNext(decision -> {
                    assertThat(decision.decision()).isEqualTo(SecurityDecision.ALLOW);
                    assertThat(decision.principal().principalId())
                            .isEqualTo("identity-1");
                    assertThat(decision.principal().tenantId())
                            .isEqualTo("tenant-1");
                    assertThat(decision.principal().attributes())
                            .containsEntry("idp.session-id", "session-1")
                            .containsEntry("idp.client-id", "gateway-client")
                            .containsEntry("idp.token-id", "token-1")
                            .doesNotContainKeys("permissions", "roles", "token");
                })
                .verifyComplete();

        GatewayPrincipal authenticated = new GatewayPrincipal(
                "identity-1", "USER", "tenant-1", null, true, Map.of(
                "idp.session-id", "session-1",
                "idp.client-id", "gateway-client",
                "idp.token-id", "token-1",
                "idp.token-version", "7"));
        var mapped = new IdpTrustedIdentityMapper().map(context(authenticated));
        assertThat(mapped.httpHeaders()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "X-Egon-Identity-Sub", "identity-1",
                "X-Egon-Tenant-Id", "tenant-1",
                "X-Egon-Session-Id", "session-1",
                "X-Egon-Client-Id", "gateway-client",
                "X-Egon-Token-Id", "token-1"));
    }

    private IdentityPrincipal principal() {
        return new IdentityPrincipal(
                "identity-1", "tenant-1", "session-1", "gateway-client",
                "token-1", 7L, Set.of("egon-api"), NOW,
                NOW.plusSeconds(300));
    }

    private GatewaySecurityPolicy policy() {
        return new GatewaySecurityPolicy(
                "security", AuthenticationMode.REQUIRED, List.of("idp-bearer"),
                List.of("idp-jwt"), List.of(),
                AuthorizationDecisionMode.ALL_ALLOW, "idp-identity",
                Duration.ofSeconds(1), SecurityFailureMode.FAIL_CLOSED,
                top.egon.cola.component.gateway.core.security
                        .CredentialForwardingMode.ORIGINAL_BEARER);
    }

    private GatewayAuthContext context(GatewayPrincipal principal) {
        return new GatewayAuthContext(
                AccessZone.PUBLIC, GatewayProtocol.HTTP, "operation-1", "route-1",
                "security", "/orders", "GET", Set.of("bearer"), principal,
                "127.0.0.1", "trace-1", "request-1", NOW.plusSeconds(5),
                "release-1");
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
