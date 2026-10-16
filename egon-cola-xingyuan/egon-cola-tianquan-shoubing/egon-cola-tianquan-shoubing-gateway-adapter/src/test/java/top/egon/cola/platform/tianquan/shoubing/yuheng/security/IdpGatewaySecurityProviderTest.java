package top.egon.cola.platform.tianquan.shoubing.yuheng.security;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import top.egon.cola.component.yuheng.contract.protocol.AccessZone;
import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;
import top.egon.cola.component.yuheng.core.context.GatewayPrincipal;
import top.egon.cola.component.yuheng.core.exchange.DefaultGatewayResponse;
import top.egon.cola.component.yuheng.core.exchange.EmptyGatewayBody;
import top.egon.cola.component.yuheng.core.exchange.GatewayExchange;
import top.egon.cola.component.yuheng.core.exchange.GatewayRequest;
import top.egon.cola.component.yuheng.core.exchange.ImmutableGatewayHeaders;
import top.egon.cola.component.yuheng.core.security.AuthenticationDecision;
import top.egon.cola.component.yuheng.core.security.AuthenticationFailure;
import top.egon.cola.component.yuheng.core.security.AuthenticationMode;
import top.egon.cola.component.yuheng.core.security.AuthorizationDecisionMode;
import top.egon.cola.component.yuheng.core.security.CredentialRecoveryResult;
import top.egon.cola.component.yuheng.core.security.CredentialForwardingMode;
import top.egon.cola.component.yuheng.core.security.GatewayAuthContext;
import top.egon.cola.component.yuheng.core.security.GatewayCredential;
import top.egon.cola.component.yuheng.core.security.GatewayCredentialOnlineStateResult;
import top.egon.cola.component.yuheng.core.security.GatewaySecurityPolicy;
import top.egon.cola.component.yuheng.core.security.SecurityDecision;
import top.egon.cola.component.yuheng.core.security.SecurityFailureMode;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.core.token.RefreshTokenStatus;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class IdpGatewaySecurityProviderTest {

    private static final Instant NOW = Instant.parse("2026-08-02T08:00:00Z");

    @Test
    void extractsExplicitBearerAndRemovesCookiesAndSpoofableHeaders() {
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
    void ignoresRetiredCookieWhenExplicitBearerIsPresent() {
        var extractor = new IdpUserCookieCredentialExtractor(
                new IdpReservedHeaderSanitizer(),
                "__Host-egon_user_at", Set.of());
        StepVerifier.create(Mono.from(extractor.extract(exchange(Map.of(
                                "Cookie", List.of("__Host-egon_user_at=cookie-token"),
                                "Authorization", List.of("Bearer another-token"))),
                        context(GatewayPrincipal.anonymous()), policy())))
                .assertNext(result -> {
                    assertThat(result.valid()).isTrue();
                    assertThat(result.credentials()).singleElement()
                            .extracting(GatewayCredential::tokenReference).isEqualTo("another-token");
                })
                .verifyComplete();
    }

    @Test
    void cookieOnlyCannotAuthenticateAndExplicitBearerIgnoresLegacyCookies() {
        var extractor = new IdpUserCookieCredentialExtractor(new IdpReservedHeaderSanitizer(),
                "__Host-egon_user_at", Set.of());
        StepVerifier.create(Mono.from(extractor.extract(exchange(Map.of(
                                "Cookie", List.of("__Host-egon_user_at=retired-token"))),
                        context(GatewayPrincipal.anonymous()), policy())))
                .assertNext(result -> assertThat(result.credentials()).isEmpty())
                .verifyComplete();
        StepVerifier.create(Mono.from(extractor.extract(exchange(Map.of(
                                "Cookie", List.of("__Host-egon_user_at=retired-token"),
                                "Authorization", List.of("Bearer current-token"))),
                        context(GatewayPrincipal.anonymous()), policy())))
                .assertNext(result -> {
                    assertThat(result.valid()).isTrue();
                    assertThat(result.credentials()).singleElement()
                            .extracting(GatewayCredential::tokenReference).isEqualTo("current-token");
                })
                .verifyComplete();
    }

    @Test
    void doesNotRefreshMissingOrExpiredUserBearerFromLegacyCookies() {
        AtomicInteger refreshCalls = new AtomicInteger();
        IdpUserCredentialRecoveryProvider provider = new IdpUserCredentialRecoveryProvider(
                token -> {
                    refreshCalls.incrementAndGet();
                    return Mono.just(new IdpRefreshClient.Response(401, Map.of()));
                },
                mock(IdpGatewayJwtVerifier.class),
                new IdpReservedHeaderSanitizer(),
                "__Host-egon_user_rt",
                "__Host-egon_user_at");
        GatewayExchange exchange = exchange(Map.of(
                "Cookie", List.of(
                        "__Host-egon_user_at=legacy-access; __Host-egon_user_rt=legacy-refresh")));

        for (AuthenticationFailure failure : List.of(
                AuthenticationFailure.MISSING,
                AuthenticationFailure.EXPIRED)) {
            StepVerifier.create(Mono.from(provider.recover(
                            context(GatewayPrincipal.anonymous()), exchange, failure)))
                    .assertNext(result -> {
                        assertThat(result.outcome())
                                .isEqualTo(CredentialRecoveryResult.Outcome.NOT_RECOVERABLE);
                        assertThat(result.responseHeaders().get("set-cookie"))
                                .hasSize(2)
                                .allSatisfy(cookie -> assertThat(cookie)
                                        .contains("Max-Age=0"));
                    })
                    .verifyComplete();
        }

        assertThat(refreshCalls).hasValue(0);
    }

    @Test
    void mapsStatelessUserIdentityWithoutSessionAttributes() {
        IdentityPrincipal user = new IdentityPrincipal(
                "identity-1", "tenant-1", "token-1", Set.of("platform"),
                NOW, NOW.plusSeconds(300),
                top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext.password());
        var provider = new IdpIdentityAuthenticationProvider(
                (context, token) -> IdpGatewayJwtVerifier.Verification.valid(user));
        AuthenticationDecision decision = Mono.from(provider.authenticate(
                        context(GatewayPrincipal.anonymous()),
                        new GatewayCredential("bearer", "signed-token", Map.of())))
                .block();
        assertThat(decision.decision()).isEqualTo(SecurityDecision.ALLOW);
        assertThat(decision.principal().attributes())
                .doesNotContainKeys("tianquan-shoubing.session-id", "tianquan-shoubing.token-version", "tianquan-shoubing.resource-uri")
                .containsEntry("tianquan-shoubing.token-id", "token-1")
                .containsEntry("tianquan-shoubing.audience", "platform")
                .doesNotContainKey("tianquan-shoubing.issuer");
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
    void sameTrustedEvidenceSurvivesPrincipalFlowWithoutBecomingIdentityHeaders() {
        var scopes = new java.util.HashSet<String>();
        for (int index = 0; index < 64; index++) { scopes.add("finance:" + index + ":" + "r".repeat(110)); }
        var service = new ServiceIdentityPrincipal("finance-service", "1001", "finance-service", "service-jti",
                URI.create("https://api.example/resource"), 12L, scopes, "finance", "finance-web", "prod", "credential-1", NOW, NOW.plusSeconds(300));
        var proof = new top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO(
                URI.create("https://idp.example/tenants/1001"), service.resourceUri(), null, "a".repeat(64), "b".repeat(64));
        var provider = new IdpIdentityAuthenticationProvider((context, token) -> new IdpGatewayJwtVerifier.Verification(
                service, AuthenticationFailure.NONE, null, scopes, proof));
        var decision = Mono.from(provider.authenticate(context(GatewayPrincipal.anonymous()),
                new GatewayCredential("bearer", "original-bearer", Map.of()))).block();
        assertThat(decision.decision()).isEqualTo(SecurityDecision.ALLOW);
        var principal = decision.principal();
        var evidence = (top.egon.cola.platform.tianquan.shoubing.yuheng.security.domain.GatewayVerifiedIdentityBO)
                principal.trustedAuthenticationEvidence();
        assertThat(evidence.proof()).isSameAs(proof);
        assertThat(principal.attributes()).containsEntry("tianquan-shoubing.issuer", proof.issuer().toString());
        assertThat(evidence.verifiedScopes()).containsExactlyInAnyOrderElementsOf(scopes);
        assertThat(context(GatewayPrincipal.anonymous()).withPrincipal(principal).principal().trustedAuthenticationEvidence()).isSameAs(evidence);
        assertThat(principal.attributes()).doesNotContainKey("tianquan-shoubing.service-scopes");
        var identity = new IdpTrustedIdentityMapper().map(context(principal));
        assertThat(identity.httpHeaders()).doesNotContainKeys("Authorization", "scope", "proof", "trustedAuthenticationEvidence", "X-Egon-Service-Scopes");
        assertThat(identity.httpHeaders().values()).doesNotContain("original-bearer", proof.tokenRef(), proof.clientRef(), proof.issuer().toString());
        assertThat(identity.rpcMetadata().values()).doesNotContain("original-bearer", proof.tokenRef(), proof.clientRef(), proof.issuer().toString());
    }

    @Test
    void mapsEachUserIssuerOnlyFromTheSameVerifiedProof() {
        var user = new IdentityPrincipal("identity-1", "1001", "token-1", Set.of("platform"),
                NOW, NOW.plusSeconds(300),
                top.egon.cola.platform.tianquan.shoubing.contract.AuthenticationContext.password());
        for (String issuer : List.of("https://idp.example/tenants/1001", "https://idp.example/tenants/1002")) {
            var proof = new top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO(
                    URI.create(issuer), URI.create("https://api.example/resource"), null, "a".repeat(64), "b".repeat(64));
            var provider = new IdpIdentityAuthenticationProvider((context, token) ->
                    new IdpGatewayJwtVerifier.Verification(user, AuthenticationFailure.NONE, null, Set.of("billing:read"), proof));
            var decision = Mono.from(provider.authenticate(context(GatewayPrincipal.anonymous()),
                    new GatewayCredential("bearer", "signed-token", Map.of("issuer", "https://attacker.example")))).block();
            assertThat(decision.decision()).isEqualTo(SecurityDecision.ALLOW);
            assertThat(decision.principal().attributes()).containsEntry(
                    IdpIdentityAuthenticationProvider.VERIFIED_ISSUER_ATTRIBUTE, issuer);
        }
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
    void keepsBearerOnlyRequestsActiveWithoutABrowserRefreshCookie() {
        IdpUserOnlineStateProvider provider = new IdpUserOnlineStateProvider(
                token -> Mono.error(new AssertionError("refresh status must not be called")),
                "__Host-egon_user_rt", "__Host-egon_user_at");

        StepVerifier.create(provider.validateAuthenticated(
                        context(new GatewayPrincipal(
                                "identity-1", "USER", "tenant-1", null,
                                true, Map.of())),
                        exchange(Map.of())))
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
                "security", AuthenticationMode.REQUIRED, List.of("tianquan-shoubing-user-cookie"),
                List.of("tianquan-shoubing-jwt"), List.of(), AuthorizationDecisionMode.ALL_ALLOW,
                "tianquan-shoubing-identity", Duration.ofSeconds(1), SecurityFailureMode.FAIL_CLOSED,
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
            public top.egon.cola.component.yuheng.core.exchange.GatewayHeaders headers() {
                return new ImmutableGatewayHeaders(headers);
            }
            public top.egon.cola.component.yuheng.core.exchange.GatewayBody body() {
                return EmptyGatewayBody.INSTANCE;
            }
        };
        return new GatewayExchange() {
            public GatewayRequest request() { return request; }
            public top.egon.cola.component.yuheng.core.context.GatewayContext context() {
                return null;
            }
            public top.egon.cola.component.yuheng.core.exchange.GatewayResponse response() {
                return DefaultGatewayResponse.success(EmptyGatewayBody.INSTANCE);
            }
        };
    }
}
