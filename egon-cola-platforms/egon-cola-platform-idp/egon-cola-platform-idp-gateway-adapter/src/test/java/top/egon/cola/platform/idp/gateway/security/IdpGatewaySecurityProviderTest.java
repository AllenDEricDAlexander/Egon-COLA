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
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;

import java.net.URI;
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
        var provider = new IdpIdentityAuthenticationProvider((context, token) -> {
            assertThat(token).isEqualTo("signed-token");
            assertThat(context.attributes())
                    .containsEntry("idp.biz-code", "permission")
                    .containsEntry("idp.app-code", "rbac3")
                    .containsEntry("idp.env", "prod");
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
                            .containsEntry("idp.token-version", "7")
                            .containsEntry("idp.resource-uri",
                                    "https://api.example/prod/permission/rbac3")
                            .containsEntry("idp.issued-at", NOW.toString())
                            .containsEntry(
                                    "idp.expires-at",
                                    NOW.plusSeconds(300).toString()
                            )
                            .doesNotContainKeys("permissions", "roles", "token");
                })
                .verifyComplete();

        GatewayPrincipal authenticated = new GatewayPrincipal(
                "identity-1", "USER", "tenant-1", null, true, Map.of(
                "idp.session-id", "session-1",
                "idp.client-id", "gateway-client",
                "idp.token-id", "token-1",
                "idp.token-version", "7",
                "idp.resource-uri", "https://api.example/prod/permission/rbac3"));
        var mapped = new IdpTrustedIdentityMapper().map(context(authenticated));
        assertThat(mapped.httpHeaders()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "X-Egon-Principal-Type", "USER",
                "X-Egon-Identity-Sub", "identity-1",
                "X-Egon-Tenant-Id", "tenant-1",
                "X-Egon-Session-Id", "session-1",
                "X-Egon-Client-Id", "gateway-client",
                "X-Egon-Token-Id", "token-1",
                "X-Egon-Token-Version", "7",
                "X-Egon-Resource-Uri", "https://api.example/prod/permission/rbac3"));
    }

    @Test
    void mapsServiceIdentityWithIdpScopesAndSourceApplication() {
        ServiceIdentityPrincipal service = new ServiceIdentityPrincipal(
                "finance-service", "tenant-1", "finance-service", "service-token",
                URI.create("https://api.example/prod/permission/rbac3"),
                12L, Set.of("service:authorization:decide", "service:identity:resolve"),
                "finance", "finance-web", "prod", "credential-1",
                NOW, NOW.plusSeconds(300));
        var provider = new IdpIdentityAuthenticationProvider(
                (context, token) -> service);

        GatewayPrincipal principal = Mono.from(provider.authenticate(
                        context(GatewayPrincipal.anonymous()),
                        new GatewayCredential("bearer", "signed-token", Map.of())))
                .block().principal();
        var mapped = new IdpTrustedIdentityMapper().map(context(principal));

        assertThat(principal.principalType()).isEqualTo("SERVICE");
        assertThat(mapped.httpHeaders()).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry("X-Egon-Principal-Type", "SERVICE"),
                Map.entry("X-Egon-Identity-Sub", "finance-service"),
                Map.entry("X-Egon-Tenant-Id", "tenant-1"),
                Map.entry("X-Egon-Client-Id", "finance-service"),
                Map.entry("X-Egon-Token-Id", "service-token"),
                Map.entry("X-Egon-Resource-Uri", "https://api.example/prod/permission/rbac3"),
                Map.entry("X-Egon-Resource-Version", "12"),
                Map.entry("X-Egon-Source-Biz", "finance"),
                Map.entry("X-Egon-Source-App", "finance-web"),
                Map.entry("X-Egon-Source-Env", "prod"),
                Map.entry("X-Egon-Service-Scopes",
                        "service:authorization:decide service:identity:resolve"),
                Map.entry("X-Egon-Credential-Id", "credential-1")));
    }

    private IdentityPrincipal principal() {
        return new IdentityPrincipal(
                "identity-1", "tenant-1", "session-1", "gateway-client",
                "token-1", 7L,
                Set.of("https://api.example/prod/permission/rbac3"), NOW,
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
                "release-1", Map.of(
                        "idp.biz-code", "permission",
                        "idp.app-code", "rbac3",
                        "idp.env", "prod"));
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
