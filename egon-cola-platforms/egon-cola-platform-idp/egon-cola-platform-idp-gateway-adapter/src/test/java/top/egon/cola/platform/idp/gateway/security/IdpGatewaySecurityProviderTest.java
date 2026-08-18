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
import top.egon.cola.component.gateway.core.security.AuthenticationDecision;
import top.egon.cola.component.gateway.core.security.AuthenticationMode;
import top.egon.cola.component.gateway.core.security.AuthorizationDecisionMode;
import top.egon.cola.component.gateway.core.security.CredentialForwardingMode;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.component.gateway.core.security.GatewayCredentialOnlineStateResult;
import top.egon.cola.component.gateway.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.gateway.core.security.SecurityDecision;
import top.egon.cola.component.gateway.core.security.SecurityFailureMode;
import top.egon.cola.platform.idp.contract.IdentityPrincipal;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.core.token.RefreshTokenStatus;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdpGatewaySecurityProviderTest {

    private static final Instant NOW = Instant.parse("2026-08-02T08:00:00Z");

    @Test
    void extractsCookieOrBearerAndRemovesCookiesAndSpoofableHeaders() {
        var extractor = new IdpUserCookieCredentialExtractor(
                new IdpReservedHeaderSanitizer(),
                "__Host-egon_user_at",
                Set.of("https://console.example"));
        StepVerifier.create(Mono.from(extractor.extract(exchange(Map.of(
                                "Cookie", List.of("__Host-egon_user_at=cookie-token"),
                                "Authorization", List.of("Bearer cookie-token"),
                                "X-Egon-Identity-Sub", List.of("mallory"))),
                        context(GatewayPrincipal.anonymous()), policy())))
                .assertNext(result -> {
                    assertThat(result.valid()).isTrue();
                    assertThat(result.credentials()).singleElement()
                            .extracting(GatewayCredential::tokenReference)
                            .isEqualTo("cookie-token");
                    assertThat(result.fieldsToRemove()).contains(
                            "authorization", "cookie", "x-egon-subject-token",
                            "x-egon-identity-sub");
                })
                .verifyComplete();
    }

    @Test
    void rejectsConflictingCookieAndBearer() {
        var extractor = new IdpUserCookieCredentialExtractor(
                new IdpReservedHeaderSanitizer(),
                "__Host-egon_user_at", Set.of());
        StepVerifier.create(Mono.from(extractor.extract(exchange(Map.of(
                                "Cookie", List.of("__Host-egon_user_at=cookie-token"),
                                "Authorization", List.of("Bearer another-token"))),
                        context(GatewayPrincipal.anonymous()), policy())))
                .assertNext(result -> assertThat(result.valid()).isFalse())
                .verifyComplete();
    }

    @Test
    void mapsStatelessUserIdentityWithoutSessionAttributes() {
        IdentityPrincipal user = new IdentityPrincipal(
                "identity-1", "tenant-1", "token-1", Set.of("platform"),
                NOW, NOW.plusSeconds(300),
                top.egon.cola.platform.idp.contract.AuthenticationContext.password());
        var provider = new IdpIdentityAuthenticationProvider(
                (context, token) -> IdpGatewayJwtVerifier.Verification.valid(user));
        AuthenticationDecision decision = Mono.from(provider.authenticate(
                        context(GatewayPrincipal.anonymous()),
                        new GatewayCredential("bearer", "signed-token", Map.of())))
                .block();
        assertThat(decision.decision()).isEqualTo(SecurityDecision.ALLOW);
        assertThat(decision.principal().attributes())
                .doesNotContainKeys("idp.session-id", "idp.token-version", "idp.resource-uri")
                .containsEntry("idp.token-id", "token-1")
                .containsEntry("idp.audience", "platform");
        assertThatThrownBy(() -> new IdpTrustedIdentityMapper()
                .map(context(decision.principal())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("USER identity headers");
    }

    @Test
    void mapsServiceIdentityWithMachineAttributes() {
        ServiceIdentityPrincipal service = new ServiceIdentityPrincipal(
                "finance-service", "tenant-1", "finance-service", "service-token",
                URI.create("https://api.example/resource"), 12L,
                Set.of("service:authorization:decide"), "finance", "finance-web",
                "prod", "credential-1", NOW, NOW.plusSeconds(300));
        var provider = new IdpIdentityAuthenticationProvider(
                (context, token) -> IdpGatewayJwtVerifier.Verification.valid(service));
        GatewayPrincipal principal = Mono.from(provider.authenticate(
                        context(GatewayPrincipal.anonymous()),
                        new GatewayCredential("bearer", "signed-token", Map.of())))
                .block().principal();
        assertThat(principal.principalType()).isEqualTo("SERVICE");
        assertThat(new IdpTrustedIdentityMapper().map(context(principal)).httpHeaders())
                .containsEntry("X-Egon-Resource-Version", "12")
                .containsEntry("X-Egon-Client-Id", "finance-service");
    }

    @Test
    void validatesRefreshTokenOnlineStateAndMatchesAuthenticatedUser() {
        IdpRefreshTokenStatusClient client = token -> Mono.just(
                new IdpRefreshTokenStatusClient.Response(
                        200,
                        new RefreshTokenStatus(
                                "identity-1", "tenant-1", Instant.now().plusSeconds(300))));
        IdpUserOnlineStateProvider provider = new IdpUserOnlineStateProvider(
                client, "__Host-egon_user_rt", "__Host-egon_user_at");

        StepVerifier.create(provider.validateAuthenticated(
                        context(new GatewayPrincipal(
                                "identity-1", "USER", "tenant-1", null,
                                true, Map.of())),
                        exchange(Map.of(
                                "Cookie", List.of("__Host-egon_user_rt=refresh-token")))))
                .assertNext(result -> assertThat(result.outcome())
                        .isEqualTo(GatewayCredentialOnlineStateResult.Outcome.ACTIVE))
                .verifyComplete();
    }

    @Test
    void revokedOrMismatchedRefreshTokenExpiresBothCookies() {
        IdpRefreshTokenStatusClient revoked = token -> Mono.just(
                new IdpRefreshTokenStatusClient.Response(401, null));
        IdpUserOnlineStateProvider provider = new IdpUserOnlineStateProvider(
                revoked, "__Host-egon_user_rt", "__Host-egon_user_at");

        StepVerifier.create(provider.validateAuthenticated(
                        context(new GatewayPrincipal(
                                "identity-1", "USER", "tenant-1", null,
                                true, Map.of())),
                        exchange(Map.of(
                                "Cookie", List.of("__Host-egon_user_rt=refresh-token")))))
                .assertNext(result -> {
                    assertThat(result.outcome())
                            .isEqualTo(GatewayCredentialOnlineStateResult.Outcome.INACTIVE);
                    assertThat(result.responseHeaders().get("set-cookie"))
                            .hasSize(2);
                })
                .verifyComplete();

        IdpRefreshTokenStatusClient mismatched = token -> Mono.just(
                new IdpRefreshTokenStatusClient.Response(
                        200,
                        new RefreshTokenStatus(
                                "other-user", "tenant-1", Instant.now().plusSeconds(300))));
        IdpUserOnlineStateProvider mismatchProvider = new IdpUserOnlineStateProvider(
                mismatched, "__Host-egon_user_rt", "__Host-egon_user_at");
        StepVerifier.create(mismatchProvider.validateAuthenticated(
                        context(new GatewayPrincipal(
                                "identity-1", "USER", "tenant-1", null,
                                true, Map.of())),
                        exchange(Map.of(
                                "Cookie", List.of("__Host-egon_user_rt=refresh-token")))))
                .assertNext(result -> assertThat(result.outcome())
                        .isEqualTo(GatewayCredentialOnlineStateResult.Outcome.INACTIVE))
                .verifyComplete();
    }

    private GatewaySecurityPolicy policy() {
        return new GatewaySecurityPolicy(
                "security", AuthenticationMode.REQUIRED, List.of("idp-user-cookie"),
                List.of("idp-jwt"), List.of(), AuthorizationDecisionMode.ALL_ALLOW,
                "idp-identity", Duration.ofSeconds(1), SecurityFailureMode.FAIL_CLOSED,
                CredentialForwardingMode.ORIGINAL_BEARER);
    }

    private GatewayAuthContext context(GatewayPrincipal principal) {
        return new GatewayAuthContext(
                AccessZone.PUBLIC, GatewayProtocol.HTTP, "operation-1", "route-1",
                "security", "/orders", "GET", Set.of("bearer"), principal,
                "127.0.0.1", "trace-1", "request-1", NOW.plusSeconds(5),
                "release-1", Map.of());
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
