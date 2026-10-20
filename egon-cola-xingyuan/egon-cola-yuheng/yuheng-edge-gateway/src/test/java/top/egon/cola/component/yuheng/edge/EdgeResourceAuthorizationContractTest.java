package top.egon.cola.component.yuheng.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.edge.config.EdgeOAuthProperties;
import top.egon.cola.component.yuheng.edge.controller.EdgeSessionController;
import top.egon.cola.component.yuheng.edge.config.EdgeSecurityConfiguration;
import top.egon.cola.component.yuheng.edge.config.domain.bo.EdgeRegistrationResourceBindingBO;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeAuthorizedClientRepository;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeSessionBindingComponent;
import top.egon.cola.component.yuheng.edge.service.impl.EdgeTokenCipher;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EdgeResourceAuthorizationContractTest {
    private static final URI ISSUER = URI.create("https://idp.example/tenants/1001");
    private static final URI RESOURCE_A = URI.create("https://api.example/a");
    private static final URI RESOURCE_B = URI.create("https://api.example/b");
    private static final Instant NOW = Instant.parse("2026-10-12T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    @TempDir Path temporary;
    private EdgeOAuthProperties properties;
    private EdgeAuthorizedClientRepository clients;
    private InMemoryReactiveClientRegistrationRepository registrations;
    Fixture fixture;

    @BeforeEach
    void setupDeclaredResourcesAndRealEncryptedRepository() throws Exception {
        fixture = new Fixture(temporary);
        properties = fixture.properties; clients = fixture.clients; registrations = fixture.registrations;
    }

    @AfterEach void closeFixture() { fixture.close(); }

    @Test
    void nativeCodePkceRequestUsesEachResourcesOwnSingletonAudience() {
        var first = begin("app-a"); var second = begin("app-b");
        assertThat(first.getResponse().getStatusCode().value()).isEqualTo(302);
        assertThat(second.getResponse().getStatusCode().value()).isEqualTo(302);
        var firstQuery = org.springframework.web.util.UriComponentsBuilder.fromUri(first.getResponse().getHeaders().getLocation()).build().getQueryParams();
        var secondQuery = org.springframework.web.util.UriComponentsBuilder.fromUri(second.getResponse().getHeaders().getLocation()).build().getQueryParams();
        assertThat(firstQuery.getFirst("resource")).isEqualTo(org.springframework.web.util.UriUtils.encodeQueryParam(RESOURCE_A.toASCIIString(), java.nio.charset.StandardCharsets.UTF_8));
        assertThat(secondQuery.getFirst("resource")).isEqualTo(org.springframework.web.util.UriUtils.encodeQueryParam(RESOURCE_B.toASCIIString(), java.nio.charset.StandardCharsets.UTF_8));
        assertThat(firstQuery.getFirst("code_challenge_method")).isEqualTo("S256");
        assertThat(secondQuery.getFirst("code_challenge_method")).isEqualTo("S256");
        assertThat(firstQuery.getFirst("state")).isNotEqualTo(secondQuery.getFirst("state"));
    }

    @Test
    void nativeRequestMustCarryTheServerBindingBeforeTheCodeCallback() {
        var started = begin("app-a");
        var location = started.getResponse().getHeaders().getLocation();
        String state = org.springframework.web.util.UriComponentsBuilder.fromUri(location).build().getQueryParams().getFirst("state");
        WebSession session = started.getSession().block(Duration.ofSeconds(5));
        var callback = MockServerWebExchange.builder(MockServerHttpRequest.get("https://edge.example/login/oauth2/code/app-a").queryParam("state", org.springframework.web.util.UriUtils.decode(state, java.nio.charset.StandardCharsets.UTF_8))).session(session).build();
        var requests = new org.springframework.security.oauth2.client.web.server.WebSessionOAuth2ServerAuthorizationRequestRepository();
        var pending = requests.loadAuthorizationRequest(callback).block(Duration.ofSeconds(5));
        assertThat(pending).isNotNull();
        assertThat((String) pending.getAttribute("egon.bff.bindingId")).isNotBlank();
        assertThat((Number) pending.getAttribute("egon.bff.generation")).isNotNull();
        assertThat((String) pending.getAttribute("egon.bff.resourceAlias")).isEqualTo("app-a");
        assertThat((String) pending.getAttribute("egon.bff.registrationId")).isEqualTo("app-a");
    }

    @Test
    void realRepositoryCannotStoreAnIdTokenAsAResourceAccessToken() throws Exception {
        var registration = registration("app-a", "app:a:read");
        var key = new RSAKeyGenerator(2048).keyID("id-key").generate();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID("id-key").build(),
                JWTClaimsSet.parse(Map.of("iss", ISSUER.toASCIIString(), "sub", "user-1", "aud", List.of(registration.getClientId()),
                        "iat", NOW.minusSeconds(1).getEpochSecond(), "exp", NOW.plusSeconds(300).getEpochSecond())));
        jwt.sign(new RSASSASigner(key));
        var user = new DefaultOAuth2User(Set.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("sub", "user-1"), "sub");
        var authentication = new OAuth2AuthenticationToken(user, user.getAuthorities(), "app-a");
        var access = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, jwt.serialize(), NOW.minusSeconds(1), NOW.plusSeconds(300), registration.getScopes());
        var client = new OAuth2AuthorizedClient(registration, "user-1", access, new OAuth2RefreshToken("controlled-refresh", NOW));
        ServerWebExchange exchange = fixture.pendingExchange("app-a", fixture.newSession());
        assertThatThrownBy(() -> clients.saveAuthorizedClient(client, authentication, exchange).block(Duration.ofSeconds(5)))
                .isInstanceOf(RuntimeException.class);
        assertThat(fixture.cached).isEmpty();
        assertThat(fixture.intents).hasSize(1);
    }

    @Test
    void declaredResourceAndLifecycleModelsHaveRealBoundaryConstraints() {
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            var valid = resource(RESOURCE_A, "app-a", "app:a:read", "/a");
            assertThat(validator.validate(valid)).isEmpty();
            valid.setRequestPaths(List.of("/**")); assertThat(validator.validate(valid)).isNotEmpty();
            valid.setRequestPaths(List.of("/api/../**")); assertThat(validator.validate(valid)).isNotEmpty();
        }
    }

    private MockServerWebExchange begin(String registration) { return fixture.begin(registration, fixture.newSession()); }

    @Test
    void firstVerifiedTokenCreatesCanonicalAndSameIssuerResourceAppendDoesNotResetTime() throws Exception {
        var session = fixture.newSession();
        var first = fixture.pendingExchange("app-a", session);
        var original = fixture.bindings.load(first).block().getOriginalCreatedAt();
        fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), fixture.authentication("app-a", "user-1"), first).block();
        var second = fixture.pendingExchange("app-b", session);
        fixture.clients.saveAuthorizedClient(fixture.client("app-b", "user-1"), fixture.authentication("app-b", "user-1"), second).block();
        var binding = fixture.bindings.load(second).block();
        assertThat(binding.getIssuer()).isEqualTo(ISSUER);
        assertThat(binding.getTenantId()).isEqualTo("1001");
        assertThat(binding.getSubject()).isEqualTo("user-1");
        assertThat(binding.getOriginalCreatedAt()).isEqualTo(original);
        assertThat(binding.getGeneration()).isEqualTo(1);
        assertThat(fixture.clients.loadAll(fixture.authentication("app-b", "user-1"), second).block()).hasSize(2);
        assertThat(fixture.cached.values()).flatExtracting(Map::values).allSatisfy(value -> assertThat(value).startsWith("ENC:v1:").doesNotContain("controlled-refresh"));
    }

    @Test
    void canonicalIssuerAndSubjectCannotBeReplacedByAnotherResourceToken() throws Exception {
        var session = fixture.newSession(); var first = fixture.pendingExchange("app-a", session);
        fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), fixture.authentication("app-a", "user-1"), first).block();
        var second = fixture.pendingExchange("app-b", session);
        assertThatThrownBy(() -> fixture.clients.saveAuthorizedClient(fixture.client("app-b", "user-2"), fixture.authentication("app-b", "user-2"), second).block())
                .isInstanceOf(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class);
        assertThat(fixture.bindings.load(second).block().getSubject()).isEqualTo("user-1");
        assertThat(fixture.intents).hasSize(1);
        var wrongIssuer = fixture.begin("app-a-b", session);
        assertThat(wrongIssuer.getResponse().getStatusCode().value()).isEqualTo(400);
        assertThat(wrongIssuer.getResponse().getHeaders().getLocation()).isNull();
    }

    @Test
    void newBindingAllowsExplicitBAndEveryAliasThenSelectsCanonicalB() throws Exception {
        var session = fixture.newSession(); var callback = fixture.pendingExchange("app-a-b", session);
        fixture.clients.saveAuthorizedClient(fixture.client("app-a-b", "user-1"), fixture.authentication("app-a-b", "user-1"), callback).block();
        var next = fixture.begin("app-b", session);
        assertThat(next.getResponse().getStatusCode().value()).isEqualTo(302);
        assertThat(next.getResponse().getHeaders().getLocation().toASCIIString()).startsWith(Fixture.ISSUER_B + "/oauth2/authorize").contains("client-app-b-b");
        assertThat(fixture.bindings.load(next).block().getTenantId()).isEqualTo("1002");
    }

    @Test
    void lifecycleTerminalGenerationAndExpiredLeaseFencePreventLateSave() throws Exception {
        var session = fixture.newSession(); var exchange = fixture.pendingExchange("app-a", session);
        var initial = fixture.bindings.load(exchange).block(); var old = fixture.bindings.acquire(initial).block();
        var valid = (top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Valid<top.egon.cola.platform.tianquan.shoubing.contract.IdentityPrincipal>)
                fixture.bindings.verifyClient(fixture.client("app-a", "user-1"), fixture.authentication("app-a", "user-1"), "fixture").block();
        var desiredBeforeExpiry = fixture.bindings.canonical(old, valid.principal(), valid.proof()).block();
        fixture.leaseDeadline.put(old.bindingId(), 0L);
        var replacement = fixture.bindings.acquire(fixture.bindings.load(exchange).block()).block();
        assertThatThrownBy(() -> fixture.bindings.writeClient(old, desiredBeforeExpiry, "a".repeat(64), "ENC:v1:stale-owner").block())
                .isInstanceOf(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class).hasMessageContaining("write fence rejected");
        assertThat(fixture.cached).isEmpty();
        assertThatThrownBy(() -> fixture.bindings.canonical(old, valid.principal(), valid.proof()).block()).isInstanceOf(RuntimeException.class);
        fixture.bindings.close(replacement).block(); fixture.bindings.clearClients(replacement).block(); fixture.bindings.release(replacement).block();
        assertThatThrownBy(() -> fixture.bindings.load(exchange).block()).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), fixture.authentication("app-a", "user-1"), exchange).block()).isInstanceOf(RuntimeException.class);
        assertThat(fixture.intents).hasSize(1); assertThat(fixture.cached.values()).allSatisfy(map -> assertThat(map.size()).isZero());
    }

    @Test
    void fullExternalServiceUsesFixedResourceStateAndRevocationWithoutOauthClientLookup() throws Exception {
        var token = fixture.token("app-a", "SERVICE", "client-app-a", Map.of());
        assertThat(fixture.bindings.verifyExternal("app-a", token, "fixture").block()).isInstanceOf(top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Valid.class);
        assertThat(fixture.resourceReads).hasValue(1); assertThat(fixture.snapshots).hasValue(1);
        assertThat(fixture.bindings.verifyExternal("app-a", fixture.token("app-a", "SERVICE", "client-app-a", Map.of("resource_version", 8L)), "fixture").block())
                .isInstanceOf(top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Invalid.class);
        fixture.resourceStatus = top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus.DISABLED;
        assertThat(fixture.bindings.verifyExternal("app-a", token, "fixture").block()).isInstanceOf(top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Invalid.class);
        fixture.resourceStatus = top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus.ACTIVE;
        fixture.snapshot.set(List.of("R", "JWT_REVOKED"));
        assertThat(fixture.bindings.verifyExternal("app-a", token, "fixture").block()).isInstanceOf(top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Invalid.class);
        fixture.snapshot.set(List.of("U", "OAUTH_SECURITY_READY_ABSENT"));
        assertThat(fixture.bindings.verifyExternal("app-a", token, "fixture").block()).isInstanceOf(top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Unavailable.class);
    }

    @Test
    void externalPlatformServiceHasNoTenantAndKeepsOriginalBearerAfterFullVerification() throws Exception {
        Map<String, Object> claims = new LinkedHashMap<>(); claims.put("iss", "https://idp.example/platform");
        claims.put("tenant_id", null); claims.put("scope_context", "PLATFORM");
        String token = fixture.token("app-a", "SERVICE", "client-app-a", claims);
        var reached = new java.util.concurrent.atomic.AtomicReference<ServerWebExchange>();
        fixture.forward("app-a", fixture.newSession(), null, token, reached);
        assertThat(reached.get()).isNotNull();
        assertThat(reached.get().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + token);
        assertThat(reached.get().getRequest().getHeaders().getFirst("X-Tenant-Id")).isNull();
        assertThat(fixture.resourceReads).hasValue(1); assertThat(fixture.snapshots).hasValue(1); assertThat(fixture.audits).hasValue(1);
        assertThat(fixture.codeExchanges).hasValue(0); assertThat(fixture.refreshExchanges).hasValue(0);
    }

    @Test
    void fullUserRejectsWrongAudienceIssuerTenantTypeExpiryAndUnrequestedScope() throws Exception {
        for (var overrides : List.<Map<String, Object>>of(Map.of("aud", List.of(RESOURCE_B.toASCIIString())), Map.of("tenant_id", "1002"),
                Map.of("iss", "https://untrusted.example/tenants/1001"), Map.of("exp", NOW.minusSeconds(60).getEpochSecond()),
                Map.of("scope", "openid offline_access secret:write"))) {
            String token = fixture.token("app-a", "USER", "user-1", overrides);
            var client = fixture.clientWithToken("app-a", "user-1", token);
            assertThat(fixture.bindings.verifyClient(client, fixture.authentication("app-a", "user-1"), "fixture").block())
                    .isNotInstanceOf(top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Valid.class);
        }
    }

    @Test
    void missingAuditDeliveryStopsAnOtherwiseValidToken() throws Exception {
        fixture.failAudit = true;
        assertThat(fixture.bindings.verifyExternal("app-a", fixture.token("app-a", "USER", "user-1", Map.of()), "fixture").block())
                .isInstanceOf(top.egon.cola.platform.tianquan.shoubing.starter.security.AccessTokenVerification.Unavailable.class);
    }

    @Test
    void actualNativeCodePkceNonceAndRs256FlowAppendsThenLogsOutBeforeBLogin() throws Exception {
        var session = fixture.newSession(); var first = fixture.completeNative(fixture.begin("app-a", session), "user-1", false);
        assertThat(first.getResponse().getStatusCode().value()).isEqualTo(303);
        assertThat(first.getResponse().getHeaders().getLocation().toASCIIString()).isEqualTo("https://edge.example/a");
        var canonical = fixture.bindings.load(first).block();
        var second = fixture.completeNative(fixture.begin("app-b", session), "user-1", false);
        assertThat(second.getResponse().getStatusCode().value()).isEqualTo(303);
        assertThat(fixture.bindings.load(second).block().getBindingId()).isEqualTo(canonical.getBindingId());
        assertThat(fixture.bindings.load(second).block().getOriginalCreatedAt()).isEqualTo(canonical.getOriginalCreatedAt());
        assertThat(fixture.codeExchanges).hasValue(2); assertThat(fixture.snapshots).hasValue(2); assertThat(fixture.audits).hasValue(2);
        assertThat(fixture.logout(fixture.authentication("app-b", "user-1"), second)).isEqualTo(top.egon.cola.component.yuheng.edge.service.EdgeLogoutService.RemoteState.CONFIRMED);
        assertThat(fixture.intents).hasSize(2); assertThat(fixture.remoteRevocations).hasValue(2);
        assertThat(fixture.leaseOwners).isEmpty(); assertThat(session.isStarted()).isFalse();
        var newSession = fixture.newSession();
        var b = fixture.completeNative(fixture.begin("app-a-b", newSession), "user-1", false);
        assertThat(b.getResponse().getStatusCode().value()).isEqualTo(303);
        assertThat(fixture.bindings.load(b).block().getTenantId()).isEqualTo("1002");
        var anotherB = fixture.completeNative(fixture.begin("app-b", newSession), "user-1", false);
        assertThat(anotherB.getResponse().getStatusCode().value()).isEqualTo(303);
        assertThat(fixture.bindings.load(anotherB).block().getIssuer()).isEqualTo(Fixture.ISSUER_B);
        var reached = new java.util.concurrent.atomic.AtomicReference<ServerWebExchange>();
        fixture.forward("app-a", newSession, fixture.authentication("app-b-b", "user-1"), null, reached);
        var ownA = SignedJWT.parse(reached.get().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION).substring(7));
        assertThat(ownA.getJWTClaimsSet().getIssuer()).isEqualTo(Fixture.ISSUER_B.toASCIIString());
        assertThat(ownA.getJWTClaimsSet().getAudience()).containsExactly(RESOURCE_A.toASCIIString());
        assertThat(ownA.getJWTClaimsSet().getStringClaim("client_id")).isEqualTo("client-app-a-b");
        fixture.forward("app-b", newSession, fixture.authentication("app-b-b", "user-1"), null, reached);
        var ownB = SignedJWT.parse(reached.get().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION).substring(7));
        assertThat(ownB.getJWTClaimsSet().getIssuer()).isEqualTo(Fixture.ISSUER_B.toASCIIString());
        assertThat(ownB.getJWTClaimsSet().getAudience()).containsExactly(RESOURCE_B.toASCIIString());
        assertThat(ownB.getJWTClaimsSet().getStringClaim("client_id")).isEqualTo("client-app-b-b");
        assertThat(fixture.logout(fixture.authentication("app-b-b", "user-1"), anotherB)).isEqualTo(top.egon.cola.component.yuheng.edge.service.EdgeLogoutService.RemoteState.CONFIRMED);
        assertThat(fixture.intents).hasSize(4); assertThat(fixture.intents.subList(2, 4)).allSatisfy(intent -> assertThat(intent.tenantId()).isEqualTo("1002"));
        assertThat(fixture.remoteRevocations).hasValue(4); assertThat(fixture.acknowledged).hasSize(4);
    }

    @Test
    void actualNativeCallbackRejectsWrongNonceBeforeClientSave() throws Exception {
        var callback = fixture.completeNative(fixture.begin("app-a", fixture.newSession()), "user-1", true);
        assertThat(callback.getResponse().getStatusCode().value()).isEqualTo(400);
        assertThat(fixture.cached).isEmpty(); assertThat(fixture.audits).hasValue(0); assertThat(fixture.leaseOwners).isEmpty();
    }

    @Test
    void nativeCallbackStrictIssuerAndInputFailuresMakeNoCodeExchange() throws Exception {
        var started = fixture.begin("app-a", fixture.newSession());
        var pending = fixture.pending(started);
        for (var params : List.of(Map.of("state", pending.getState(), "code", "controlled-code"),
                Map.of("state", pending.getState(), "code", "controlled-code", "iss", Fixture.ISSUER_B.toASCIIString()),
                Map.of("state", "other-state", "code", "controlled-code", "iss", ISSUER.toASCIIString()),
                Map.of("state", pending.getState(), "code", "controlled-code", "error", "access_denied", "iss", ISSUER.toASCIIString()))) {
            var request = MockServerHttpRequest.get("https://edge.example/login/oauth2/code/app-a"); params.forEach(request::queryParam);
            var exchange = MockServerWebExchange.builder(request).session(started.getSession().block()).build();
            fixture.filterNative(exchange, pending, "user-1", false);
            assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(400);
        }
        assertThat(fixture.codeExchanges).hasValue(0); assertThat(fixture.cached).isEmpty();
    }

    @Test
    void allFamiliesAreDurableAndLocalStateTerminalEvenWhenQueueFails() throws Exception {
        var session = fixture.newSession(); var first = fixture.pendingExchange("app-a", session);
        fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), fixture.authentication("app-a", "user-1"), first).block();
        fixture.failQueue = true;
        assertThat(fixture.logout(fixture.authentication("app-a", "user-1"), first)).isEqualTo(top.egon.cola.component.yuheng.edge.service.EdgeLogoutService.RemoteState.UNKNOWN);
        assertThat(fixture.remoteRevocations).hasValue(0); assertThat(fixture.cached.values()).allSatisfy(map -> assertThat(map.size()).isZero());
        assertThat(fixture.hashes.values()).allSatisfy(map -> assertThat(map.get("state")).isEqualTo("CLOSED"));
        assertThat(fixture.leaseOwners).isEmpty();
    }

    @Test
    void readOnlySessionSkipsExpiredCandidateAndNeverMasksUnavailable() throws Exception {
        var session = fixture.newSession(); var first = fixture.pendingExchange("app-a", session);
        var principal = fixture.authentication("app-a", "user-1"); fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), principal, first).block();
        fixture.time.set(NOW.plusSeconds(331));
        var second = fixture.pendingExchange("app-b", session); fixture.clients.saveAuthorizedClient(fixture.client("app-b", "user-1"), fixture.authentication("app-b", "user-1"), second).block();
        var query = fixture.request("/bff/session", session); var controller = fixture.controller();
        assertThat(controller.session(principal, query).block().getStatusCode().value()).isEqualTo(200);
        fixture.snapshot.set(List.of("U", "OAUTH_SECURITY_READY_ABSENT"));
        assertThat(controller.session(principal, query).block().getStatusCode().value()).isEqualTo(503);
        fixture.snapshot.set(List.of("V", "ACTIVE")); fixture.time.set(NOW.plusSeconds(662));
        assertThat(controller.session(principal, query).block().getStatusCode().value()).isEqualTo(401);
        assertThat(fixture.refreshExchanges).hasValue(0); assertThat(fixture.codeExchanges).hasValue(0);
    }

    @Test
    void terminalOrOldSessionDoesNotBecomeAFalseAuthenticatedView() throws Exception {
        var session = fixture.newSession(); var callback = fixture.pendingExchange("app-a", session);
        var principal = fixture.authentication("app-a", "user-1"); fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), principal, callback).block();
        fixture.time.set(NOW.plusSeconds(8 * 3600 + 1)); var query = fixture.request("/bff/session", session);
        assertThat(fixture.controller().session(principal, query).block().getStatusCode().value()).isEqualTo(401);
        assertThat(query.getResponse().getCookies().getFirst("__Host-egon_bff").getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(session.isStarted()).isFalse();
        var old = fixture.request("/bff/session", fixture.newSession()); old.getSession().block().getAttributes().put("legacy-binding", "old-v1");
        assertThat(fixture.controller().session(principal, old).block().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void completedRequestObservationIsRemovedAndClosedAndNoProofReachesHeaders() throws Exception {
        var session = fixture.newSession(); var callback = fixture.pendingExchange("app-a", session);
        var principal = fixture.authentication("app-a", "user-1"); fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), principal, callback).block();
        var reached = new java.util.concurrent.atomic.AtomicReference<ServerWebExchange>();
        var completed = fixture.forward("app-a", session, principal, null, reached);
        assertThat(reached.get()).isNotNull();
        assertThat(completed.getAttributes()).doesNotContainKeys(top.egon.cola.component.yuheng.edge.filter.EdgeResourceAuthorizationGatewayFilterFactory.AUTHENTICATION_OBSERVATION,
                top.egon.cola.component.yuheng.edge.filter.EdgeResourceAuthorizationGatewayFilterFactory.AUTHORIZATION_OBSERVATION,
                top.egon.cola.component.yuheng.edge.filter.EdgeResourceAuthorizationGatewayFilterFactory.VERIFIED_ACCESS);
        assertThat(reached.get().getRequest().getHeaders().keySet()).noneMatch(name -> name.toLowerCase().contains("proof") || name.toLowerCase().contains("marker"));
    }

    @Test
    void externalWrongAudienceNeverReachesBusinessAndMissingResourceTokenNeedsExplicitLogin() throws Exception {
        var reached = new java.util.concurrent.atomic.AtomicReference<ServerWebExchange>();
        var wrong = fixture.forward("app-a", fixture.newSession(), null, fixture.token("app-b", "USER", "user-1", Map.of()), reached);
        assertThat(wrong.getResponse().getStatusCode().value()).isEqualTo(401); assertThat(reached.get()).isNull();
        var session = fixture.newSession(); var callback = fixture.pendingExchange("app-a", session); var principal = fixture.authentication("app-a", "user-1");
        fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), principal, callback).block();
        var missing = fixture.forward("app-b", session, principal, null, reached);
        assertThat(missing.getResponse().getStatusCode().value()).isEqualTo(401); assertThat(reached.get()).isNull();
        assertThat(org.springframework.core.io.buffer.DataBufferUtils.join(((org.springframework.mock.http.server.reactive.MockServerHttpResponse) missing.getResponse()).getBody()).map(buffer -> { byte[] bytes = new byte[buffer.readableByteCount()]; buffer.read(bytes); org.springframework.core.io.buffer.DataBufferUtils.release(buffer); return new String(bytes, java.nio.charset.StandardCharsets.UTF_8); }).block()).contains("resource_authorization_required");
    }

    @Test
    void cancelledRefreshClosesTheBindingAndReleasesEveryLock() throws Exception {
        var session = fixture.newSession(); var callback = fixture.pendingExchange("app-a", session);
        var principal = fixture.authentication("app-a", "user-1"); var valid = fixture.client("app-a", "user-1");
        fixture.clients.saveAuthorizedClient(new OAuth2AuthorizedClient(valid.getClientRegistration(), "user-1", new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                valid.getAccessToken().getTokenValue(), NOW.minusSeconds(600), NOW.minusSeconds(1), valid.getAccessToken().getScopes()), valid.getRefreshToken()), principal, callback).block();
        fixture.neverRefresh = true; var started = new java.util.concurrent.CountDownLatch(1);
        var manager = fixture.manager(); var request = fixture.request("/api/app-a/data", session);
        var pending = manager.authorize(org.springframework.security.oauth2.client.OAuth2AuthorizeRequest.withClientRegistrationId("app-a").principal(principal)
                .attribute(ServerWebExchange.class.getName(), request).build()).doOnSubscribe(ignored -> started.countDown()).subscribe();
        assertThat(started.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        assertThat(fixture.refreshStarted.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        pending.dispose();
        for (int attempt = 0; attempt < 200 && !fixture.leaseOwners.isEmpty(); attempt++) { Thread.sleep(10); }
        assertThat(fixture.leaseOwners).isEmpty(); assertThat(fixture.intents).hasSize(1);
        assertThatThrownBy(() -> fixture.bindings.load(request).block()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void twoNodesCreatingTheSamePreAuthSessionUseOneAtomicBindingPointer() {
        String serverSessionId = "controlled-server-session";
        var first = detachedSession(serverSessionId); var second = detachedSession(serverSessionId);
        var firstExchange = fixture.request("/oauth2/authorization/app-a", first);
        var secondExchange = fixture.request("/oauth2/authorization/app-b", second);
        var bindings = reactor.core.publisher.Flux.merge(
                fixture.bindings.getOrCreateForLogin(firstExchange), fixture.bindings.getOrCreateForLogin(secondExchange))
                .collectList().block(Duration.ofSeconds(5));
        assertThat(bindings).hasSize(2);
        assertThat(bindings).extracting(top.egon.cola.component.yuheng.edge.service.domain.bo.EdgeSessionBindingBO::getBindingId)
                .containsOnly(bindings.getFirst().getBindingId());
        assertThat(fixture.pointers).hasSize(1); assertThat(fixture.hashes).hasSize(1);
        assertThat(first.<String>getAttribute(EdgeSessionBindingComponent.BINDING_ID)).isEqualTo(second.getAttribute(EdgeSessionBindingComponent.BINDING_ID));
        assertThat(bindings).allSatisfy(binding -> { assertThat(binding.getGeneration()).isEqualTo(1); assertThat(binding.getIssuer()).isNull(); });
    }

    @Test
    void csrfRejectsUnexpectedQueryAndBodyBeforeReturningToken() {
        for (var request : List.of(MockServerHttpRequest.get("https://edge.example/api/oauth/csrf?unexpected=value").build(),
                MockServerHttpRequest.method(org.springframework.http.HttpMethod.GET, "https://edge.example/api/oauth/csrf").body("unexpected"))) {
            var exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put(org.springframework.security.web.server.csrf.CsrfToken.class.getName(),
                    Mono.just(new org.springframework.security.web.server.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "private-token")));
            var response = fixture.controller().csrf(exchange).block();
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
            assertThat(response.getBody().toString()).doesNotContain("private-token");
        }
    }

    @Test
    void logoutAcknowledgesOnlyConfirmedFamiliesAndKeepsFailedFamilyPending() throws Exception {
        var session = fixture.newSession(); var principal = fixture.authentication("app-a", "user-1");
        var first = fixture.pendingExchange("app-a", session);
        fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), principal, first).block();
        var second = fixture.pendingExchange("app-b", session);
        fixture.clients.saveAuthorizedClient(fixture.client("app-b", "user-1"), fixture.authentication("app-b", "user-1"), second).block();
        fixture.failRemoteRegistration = "app-b";
        assertThat(fixture.logout(principal, second)).isEqualTo(top.egon.cola.component.yuheng.edge.service.EdgeLogoutService.RemoteState.PENDING);
        assertThat(fixture.intents).hasSize(2); assertThat(fixture.remoteRevocations).hasValue(2);
        assertThat(fixture.acknowledged).containsExactly(fixture.intents.stream().filter(intent -> intent.registrationId().equals("app-a")).findFirst().orElseThrow().intentId());
        assertThat(fixture.leaseOwners).isEmpty(); assertThat(fixture.cached.values()).allSatisfy(map -> assertThat(map.size()).isZero());
        assertThatThrownBy(() -> fixture.bindings.load(second).block()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void logoutSnapshotsLatestRotatedRefreshTokenRatherThanConsumedToken() throws Exception {
        var session = fixture.newSession(); var callback = fixture.pendingExchange("app-a", session);
        var principal = fixture.authentication("app-a", "user-1"); var client = fixture.client("app-a", "user-1");
        fixture.clients.saveAuthorizedClient(new OAuth2AuthorizedClient(client.getClientRegistration(), "user-1",
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, client.getAccessToken().getTokenValue(),
                        NOW.minusSeconds(600), NOW.minusSeconds(1), client.getAccessToken().getScopes()), client.getRefreshToken()), principal, callback).block();
        var request = fixture.request("/api/app-a/data", session);
        var refreshed = fixture.manager().authorize(org.springframework.security.oauth2.client.OAuth2AuthorizeRequest.withClientRegistrationId("app-a")
                .principal(principal).attribute(ServerWebExchange.class.getName(), request).build()).block(Duration.ofSeconds(10));
        assertThat(refreshed.getRefreshToken().getTokenValue()).startsWith("rotated-refresh-");
        assertThat(fixture.logout(principal, request)).isEqualTo(top.egon.cola.component.yuheng.edge.service.EdgeLogoutService.RemoteState.CONFIRMED);
        assertThat(fixture.intents).singleElement().satisfies(intent -> assertThat(intent.tokenValue()).isEqualTo(refreshed.getRefreshToken().getTokenValue()));
        assertThat(fixture.acknowledged).hasSize(1);
    }

    @Test
    void productionConfigurationReusesQualifiedSdkBeansAndActualAuditReadiness() {
        when(fixture.redis.reactive()).thenReturn(fixture.reactive);
        try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            context.setEnvironment(new org.springframework.boot.web.reactive.context.StandardReactiveWebEnvironment());
            Map<String, Object> settings = new LinkedHashMap<>(); String prefix = "egon.cola.platform.tianquan.shoubing.";
            settings.put(prefix + "enabled", false); settings.put(prefix + "resource-server-id", "resource-app-a");
            settings.put(prefix + "resource-uri", RESOURCE_A.toASCIIString());
            settings.put(prefix + "revocation.primary-endpoint", "rediss://redis.example:6379"); settings.put(prefix + "revocation.epoch", "epoch-1");
            settings.put(prefix + "usage-observation.enabled", true); settings.put(prefix + "usage-observation.receiver", "EDGE");
            settings.put(prefix + "usage-observation.fallback-issuer", ISSUER.toASCIIString());
            settings.put(prefix + "audit.stream", "egon:test:edge:audit"); settings.put(prefix + "audit.spool-directory", temporary.resolve("context-spool").toString());
            settings.put(prefix + "audit.hmac-key-file", fixture.properties.getSession().getEncryption().getKeyFile());
            for (var issuer : List.of(ISSUER, Fixture.ISSUER_B)) {
                String tenant = issuer.getPath().substring("/tenants/".length()); String trust = prefix + "trusted-issuers." + tenant + ".";
                settings.put(trust + "issuer", issuer.toASCIIString()); settings.put(trust + "jwks-uri", issuer + "/.well-known/jwks.json");
                settings.put(trust + "realm-key", "tenant:" + tenant); settings.put(trust + "realm-type", "TENANT");
                settings.put(trust + "tenant-id", tenant); settings.put(trust + "status", "ACTIVE"); settings.put(trust + "trust-version", 1);
            }
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("edgeConfigurationFixture", settings));
            org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor.register(context);
            context.registerBean("edgeOAuthProperties", EdgeOAuthProperties.class, () -> fixture.properties);
            context.registerBean("redisson", RedissonClient.class, () -> fixture.redis, definition -> definition.setDestroyMethodName(""));
            context.registerBean("reactiveClientRegistrationRepository", org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository.class, () -> fixture.registrations);
            context.registerBean("jacksonObjectMapper", ObjectMapper.class, () -> fixture.mapper);
            context.registerBean("defaultValidator", jakarta.validation.Validator.class, () -> fixture.validator);
            context.registerBean("meterRegistry", io.micrometer.core.instrument.MeterRegistry.class, () -> fixture.meters);
            context.registerBean("edgeLogoutIntentStore", top.egon.cola.component.yuheng.edge.service.EdgeLogoutIntentStore.class, () -> fixture.intentStore);
            context.register(EdgeSecurityConfiguration.class, top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpSecurityObservationAutoConfiguration.class,
                    EdgeTokenCipher.class, EdgeSessionBindingComponent.class, EdgeAuthorizedClientRepository.class);
            context.refresh();
            assertThat(context.getBean("idpClock")).isSameAs(context.getBean("edgeClock"));
            assertThat(context.getBean("idpStarterProperties")).isSameAs(context.getBean("edgeIdpStarterProperties"));
            assertThat(context.getBean("idpAuditRedissonClient")).isSameAs(fixture.redis);
            assertThat(context.getBean("rbac3RuntimeRedissonClient")).isSameAs(fixture.redis);
            assertThat(context.getBean("reactiveOAuthRevocationReader")).isSameAs(context.getBean("edgeReactiveOAuthRevocationReader"));
            assertThat(context.getBean("trustedIssuerRegistry")).isSameAs(context.getBean("edgeTrustedIssuerRegistry"));
            assertThat(context.getBean("verifiedPrincipalConverter")).isSameAs(context.getBean("verifiedPrincipalConverterImpl"));
            assertThat(context.getBean(org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager.class))
                    .isSameAs(context.getBean("reactiveOAuth2AuthorizedClientManager"));
            assertThat(context.getBean("idpObservedVerificationComponent")).isInstanceOf(top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent.class);
            assertThat(context.getBean("idpSecurityAuditReady", java.util.function.BooleanSupplier.class).getAsBoolean()).isTrue();
            assertThat(context.getBean("edgeSecurityAuditHealthIndicator", org.springframework.boot.actuate.health.HealthIndicator.class).health().getStatus())
                    .isEqualTo(org.springframework.boot.actuate.health.Status.UP);
            assertThat(context.containsBean("idpSecurityObservationFilter")).isFalse();
            try { context.getBean("idpAuditSpoolLease", java.nio.channels.FileChannel.class).close(); }
            catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
            assertThat(context.getBean("idpSecurityAuditReady", java.util.function.BooleanSupplier.class).getAsBoolean()).isFalse();
            assertThat(context.getBean("edgeSecurityAuditHealthIndicator", org.springframework.boot.actuate.health.HealthIndicator.class).health().getStatus())
                    .isEqualTo(org.springframework.boot.actuate.health.Status.DOWN);
        }
        org.mockito.Mockito.verify(fixture.redis, org.mockito.Mockito.never()).shutdown();
    }

    @Test
    void simultaneousFirstTokenWritesCommitOnlyOneCanonicalIdentityAndCompensateLoser() throws Exception {
        var session = fixture.newSession(); var first = fixture.pendingExchange("app-a", session); var second = fixture.pendingExchange("app-b", session);
        var outcomes = reactor.core.publisher.Flux.merge(
                fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), fixture.authentication("app-a", "user-1"), first).materialize(),
                fixture.clients.saveAuthorizedClient(fixture.client("app-b", "user-2"), fixture.authentication("app-b", "user-2"), second).materialize())
                .collectList().block(Duration.ofSeconds(10));
        assertThat(outcomes.stream().filter(reactor.core.publisher.Signal::isOnComplete).count()).isEqualTo(1);
        assertThat(outcomes.stream().filter(reactor.core.publisher.Signal::isOnError).count()).isEqualTo(1);
        var binding = fixture.bindings.load(first).block(); assertThat(binding.getSubject()).isIn("user-1", "user-2");
        assertThat(binding.getGeneration()).isEqualTo(1); assertThat(fixture.cached.values()).singleElement().satisfies(map -> assertThat(map).hasSize(1));
        assertThat(fixture.intents).hasSize(1); assertThat(fixture.leaseOwners).isEmpty();
    }

    @Test
    void concurrentLogoutWaitsForRefreshCommitAndRevokesTheRotatedFamily() throws Exception {
        var session = fixture.newSession(); var callback = fixture.pendingExchange("app-a", session);
        var principal = fixture.authentication("app-a", "user-1"); var client = fixture.client("app-a", "user-1");
        fixture.clients.saveAuthorizedClient(new OAuth2AuthorizedClient(client.getClientRegistration(), "user-1",
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, client.getAccessToken().getTokenValue(),
                        NOW.minusSeconds(600), NOW.minusSeconds(1), client.getAccessToken().getScopes()), client.getRefreshToken()), principal, callback).block();
        var gate = reactor.core.publisher.Sinks.<Void>empty(); fixture.refreshResponseGate = gate.asMono();
        var request = fixture.request("/api/app-a/data", session); var closing = fixture.request("/bff/logout", session);
        var refresh = fixture.manager().authorize(org.springframework.security.oauth2.client.OAuth2AuthorizeRequest.withClientRegistrationId("app-a")
                .principal(principal).attribute(ServerWebExchange.class.getName(), request).build()).toFuture();
        assertThat(fixture.refreshStarted.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        var logout = Mono.fromCallable(() -> fixture.logout(principal, closing)).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).toFuture();
        assertThat(fixture.lifecycleContended.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        assertThat(logout.isDone()).isFalse(); assertThat(fixture.intents).isEmpty();
        gate.tryEmitEmpty(); var updated = refresh.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(logout.get(5, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(top.egon.cola.component.yuheng.edge.service.EdgeLogoutService.RemoteState.CONFIRMED);
        assertThat(fixture.intents).singleElement().satisfies(intent -> assertThat(intent.tokenValue()).isEqualTo(updated.getRefreshToken().getTokenValue()));
        assertThat(fixture.refreshExchanges).hasValue(1); assertThat(fixture.remoteRevocations).hasValue(1); assertThat(fixture.leaseOwners).isEmpty();
    }

    @Test
    void refreshTimeoutClosesUnknownFamilyAndReleasesLocksWithoutRetry() throws Exception {
        var session = fixture.newSession(); var callback = fixture.pendingExchange("app-a", session);
        var principal = fixture.authentication("app-a", "user-1"); var client = fixture.client("app-a", "user-1");
        fixture.clients.saveAuthorizedClient(new OAuth2AuthorizedClient(client.getClientRegistration(), "user-1",
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, client.getAccessToken().getTokenValue(),
                        NOW.minusSeconds(600), NOW.minusSeconds(1), client.getAccessToken().getScopes()), client.getRefreshToken()), principal, callback).block();
        fixture.neverRefresh = true; fixture.properties.getRefresh().setTokenTimeout(Duration.ofMillis(250));
        var request = fixture.request("/api/app-a/data", session);
        assertThatThrownBy(() -> fixture.manager().authorize(org.springframework.security.oauth2.client.OAuth2AuthorizeRequest.withClientRegistrationId("app-a")
                .principal(principal).attribute(ServerWebExchange.class.getName(), request).build()).block(Duration.ofSeconds(5)))
                .isInstanceOf(org.springframework.security.oauth2.core.OAuth2AuthorizationException.class);
        assertThat(fixture.refreshStarted.getCount()).isZero(); assertThat(fixture.intents).hasSize(1); assertThat(fixture.leaseOwners).isEmpty();
        assertThatThrownBy(() -> fixture.bindings.load(request).block()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void missingCanonicalIssuerMappingNeverFallsBackAndHeldMappingDriftIsUnavailable() throws Exception {
        var session = fixture.newSession(); var principal = fixture.authentication("app-a", "user-1");
        var first = fixture.pendingExchange("app-a", session); fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), principal, first).block();
        var secondResource = fixture.properties.getResources().get("app-b"); var mappings = secondResource.getIssuerRegistrationIds();
        secondResource.setIssuerRegistrationIds(Map.of(Fixture.ISSUER_B.toASCIIString(), "app-b-b")).setDefaultRegistrationId("app-b-b");
        var reached = new java.util.concurrent.atomic.AtomicReference<ServerWebExchange>();
        var missing = fixture.forward("app-b", session, principal, null, reached);
        assertThat(missing.getResponse().getStatusCode().value()).isEqualTo(503); assertThat(reached.get()).isNull();
        assertThat(fixture.controller().session(principal, fixture.request("/bff/session", session)).block().getStatusCode().value()).isEqualTo(200);
        secondResource.setIssuerRegistrationIds(mappings).setDefaultRegistrationId("app-b");
        var second = fixture.pendingExchange("app-b", session);
        fixture.clients.saveAuthorizedClient(fixture.client("app-b", "user-1"), fixture.authentication("app-b", "user-1"), second).block();
        secondResource.setIssuerRegistrationIds(Map.of(Fixture.ISSUER_B.toASCIIString(), "app-b-b")).setDefaultRegistrationId("app-b-b");
        assertThat(fixture.controller().session(principal, fixture.request("/bff/session", session)).block().getStatusCode().value()).isEqualTo(503);
        assertThat(fixture.codeExchanges).hasValue(0); assertThat(fixture.refreshExchanges).hasValue(0);
    }

    @Test
    void corruptEncryptedHeldClientIsUnavailableBeforeVerificationOrBusinessForwarding() throws Exception {
        var session = fixture.newSession(); var principal = fixture.authentication("app-a", "user-1"); var callback = fixture.pendingExchange("app-a", session);
        fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), principal, callback).block();
        fixture.cached.values().forEach(map -> map.replaceAll((field, ciphertext) -> ciphertext.replace("ENC:v1:test-key:", "ENC:v1:wrong-key:")));
        int verifiedBefore = fixture.snapshots.get();
        var sessionOutcome = fixture.controller().session(principal, fixture.request("/bff/session", session)).materialize().block();
        assertThat(sessionOutcome.isOnNext()).as("Corrupt server-held ciphertext maps to the defined unavailable response").isTrue();
        assertThat(sessionOutcome.get().getStatusCode().value()).isEqualTo(503);
        var reached = new java.util.concurrent.atomic.AtomicReference<ServerWebExchange>();
        var failed = fixture.forward("app-a", session, principal, null, reached);
        assertThat(failed.getResponse().getStatusCode().value()).isEqualTo(503); assertThat(reached.get()).isNull();
        assertThat(fixture.snapshots.get()).isEqualTo(verifiedBefore);
    }

    @Test
    void failedLateTokenCompensationClosesAndClearsAlreadyHeldClients() throws Exception {
        var session = fixture.newSession(); var principal = fixture.authentication("app-a", "user-1"); var first = fixture.pendingExchange("app-a", session);
        fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), principal, first).block();
        var second = fixture.pendingExchange("app-b", session); fixture.failQueue = true;
        assertThatThrownBy(() -> fixture.clients.saveAuthorizedClient(fixture.clientWithToken("app-b", "user-1", "invalid-access-token"),
                fixture.authentication("app-b", "user-1"), second).block(Duration.ofSeconds(5))).isInstanceOf(RuntimeException.class);
        assertThat(fixture.hashes.values()).allSatisfy(row -> assertThat(row.get("state")).isEqualTo("CLOSED"));
        assertThat(fixture.cached.values()).allSatisfy(map -> assertThat(map.size()).isZero());
        assertThat(fixture.leaseOwners).isEmpty();
    }

    @Test
    void readOnlySessionDoesNotHideLaterDependencyFailureBehindFirstValidResource() throws Exception {
        var session = fixture.newSession(); var principal = fixture.authentication("app-a", "user-1"); var first = fixture.pendingExchange("app-a", session);
        fixture.clients.saveAuthorizedClient(fixture.client("app-a", "user-1"), principal, first).block();
        var second = fixture.pendingExchange("app-b", session);
        fixture.clients.saveAuthorizedClient(fixture.client("app-b", "user-1"), fixture.authentication("app-b", "user-1"), second).block();
        fixture.snapshots.set(0); fixture.unavailableSnapshotNumber = 2;
        assertThat(fixture.controller().session(principal, fixture.request("/bff/session", session)).block().getStatusCode().value()).isEqualTo(503);
        assertThat(fixture.snapshots).hasValue(2); assertThat(fixture.refreshExchanges).hasValue(0); assertThat(fixture.codeExchanges).hasValue(0);
    }

    private static WebSession detachedSession(String serverSessionId) {
        var session = mock(WebSession.class); Map<String, Object> values = new java.util.concurrent.ConcurrentHashMap<>();
        when(session.getId()).thenReturn(serverSessionId); when(session.getAttributes()).thenReturn(values);
        when(session.getAttribute(anyString())).thenAnswer(call -> values.get(call.getArgument(0)));
        return session;
    }

    static final class Fixture implements AutoCloseable {
        static final Instant TIME = NOW;
        static final Clock TIME_CLOCK = CLOCK;
        static final URI ISSUER_B = URI.create("https://idp.example/tenants/1002");
        final java.util.concurrent.atomic.AtomicReference<Instant> time = new java.util.concurrent.atomic.AtomicReference<>(NOW);
        final Clock clock = new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return time.get(); }
        };
        final EdgeOAuthProperties properties = new EdgeOAuthProperties();
        final IdpStarterProperties idp = new IdpStarterProperties();
        final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        final InMemoryReactiveClientRegistrationRepository registrations;
        final RedissonClient redis = mock(RedissonClient.class);
        final org.redisson.api.RedissonReactiveClient reactive = mock(org.redisson.api.RedissonReactiveClient.class);
        final Map<String, Map<String, String>> hashes = new java.util.concurrent.ConcurrentHashMap<>();
        final Map<String, Map<String, String>> cached = new java.util.concurrent.ConcurrentHashMap<>();
        final Map<String, String> pointers = new java.util.concurrent.ConcurrentHashMap<>();
        final Map<String, String> leaseOwners = new java.util.concurrent.ConcurrentHashMap<>();
        final Map<String, Long> leaseDeadline = new java.util.concurrent.ConcurrentHashMap<>();
        final List<top.egon.cola.component.yuheng.edge.service.EdgeLogoutRevocationIntent> intents = new java.util.concurrent.CopyOnWriteArrayList<>();
        final java.util.concurrent.atomic.AtomicInteger audits = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger snapshots = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger codeExchanges = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.CountDownLatch refreshStarted = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicInteger refreshExchanges = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger remoteRevocations = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger resourceReads = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicReference<List<Object>> snapshot = new java.util.concurrent.atomic.AtomicReference<>(List.of("V", "ACTIVE"));
        final com.nimbusds.jose.jwk.RSAKey keyA = new RSAKeyGenerator(2048).keyID("key-a").generate();
        final com.nimbusds.jose.jwk.RSAKey keyB = new RSAKeyGenerator(2048).keyID("key-b").generate();
        final java.util.concurrent.atomic.AtomicInteger tokenIds = new java.util.concurrent.atomic.AtomicInteger(901);
        final top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent managers;
        final top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent observer;
        final EdgeSessionBindingComponent bindings;
        final EdgeAuthorizedClientRepository clients;
        final top.egon.cola.component.yuheng.edge.service.EdgeLogoutIntentStore intentStore;
        final io.micrometer.core.instrument.simple.SimpleMeterRegistry meters = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        final java.util.concurrent.atomic.AtomicReference<Throwable> nativeFailure = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicReference<Throwable> codeBoundaryFailure = new java.util.concurrent.atomic.AtomicReference<>();
        volatile int unavailableSnapshotNumber = -1;
        volatile boolean failAudit;
        volatile boolean failQueue;
        volatile boolean neverRefresh;
        volatile String failRemoteRegistration;
        volatile Mono<Void> refreshResponseGate = Mono.empty();
        final java.util.concurrent.CountDownLatch lifecycleContended = new java.util.concurrent.CountDownLatch(1);
        final List<String> acknowledged = new java.util.concurrent.CopyOnWriteArrayList<>();
        volatile top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus resourceStatus = top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus.ACTIVE;

        @SuppressWarnings("unchecked")
        Fixture(Path directory) throws Exception {
            validator.afterPropertiesSet();
            properties.getUpstreams().setAuthorizationServer(URI.create("https://idp.example"));
            properties.getUpstreams().setBizGateway(URI.create("https://biz.example")); properties.getUpstreams().setMcpGateway(URI.create("https://mcp.example"));
            properties.setPublicOrigin(URI.create("https://edge.example")).setDefaultRegistrationId("app-a").setAllowedReturnPaths(List.of("/", "/a", "/b"));
            properties.setResources(Map.of("app-a", resource(RESOURCE_A, "app-a", "app:a:read", "/a"), "app-b", resource(RESOURCE_B, "app-b", "app:b:read", "/b")));
            Path secret = directory.resolve("edge.key"); Files.writeString(secret, "0123456789abcdef0123456789abcdef");
            Files.setPosixFilePermissions(secret, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            properties.getSession().getEncryption().setKeyId("test-key").setKeyFile(secret.toString());
            EdgeTokenCipher cipher = new EdgeTokenCipher(properties); cipher.initializeKey();
            registrations = new InMemoryReactiveClientRegistrationRepository(registration("app-a", "app:a:read"), registration("app-b", "app:b:read"),
                    registration("app-a-b", "app:a:read"), registration("app-b-b", "app:b:read"));
            idp.setResourceServerId("resource-app-a"); idp.setResourceUri(RESOURCE_A);
            idp.getRevocation().setPrimaryEndpoint("rediss://redis.example:6379"); idp.getRevocation().setEpoch("epoch-1");
            Map<String, IdpStarterProperties.TrustedIssuerSettings> trusts = new LinkedHashMap<>();
            for (var issuer : List.of(ISSUER, ISSUER_B)) {
                String tenant = issuer.getPath().substring("/tenants/".length());
                var trust = new IdpStarterProperties.TrustedIssuerSettings(); trust.setIssuer(issuer.toASCIIString()); trust.setJwksUri(issuer + "/.well-known/jwks.json");
                trust.setRealmKey("tenant:" + tenant); trust.setRealmType("TENANT"); trust.setTenantId(tenant); trust.setStatus("ACTIVE"); trust.setTrustVersion(1);
                trusts.put(tenant, trust);
            }
            var platform = new IdpStarterProperties.TrustedIssuerSettings(); platform.setIssuer("https://idp.example/platform");
            platform.setJwksUri("https://idp.example/platform/.well-known/jwks.json"); platform.setRealmKey("platform");
            platform.setRealmType("PLATFORM"); platform.setStatus("ACTIVE"); platform.setTrustVersion(1); trusts.put("platform", platform);
            idp.setTrustedIssuers(trusts); var registry = new top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry(idp);
            managers = new top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent(registry, idp, clock); managers.initialize();
            var rest = managers.getClass().getDeclaredField("jwksRestOperations"); rest.setAccessible(true);
            var jwks = org.springframework.test.web.client.MockRestServiceServer.bindTo((org.springframework.web.client.RestTemplate) rest.get(managers)).ignoreExpectOrder(true).build();
            for (var issuer : List.of(ISSUER, ISSUER_B)) {
                jwks.expect(org.springframework.test.web.client.ExpectedCount.manyTimes(), org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(issuer + "/.well-known/jwks.json"))
                        .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                                new com.nimbusds.jose.jwk.JWKSet((issuer.equals(ISSUER) ? keyA : keyB).toPublicJWK()).toString(), org.springframework.http.MediaType.APPLICATION_JSON));
            }
            jwks.expect(org.springframework.test.web.client.ExpectedCount.manyTimes(), org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://idp.example/platform/.well-known/jwks.json"))
                    .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(new com.nimbusds.jose.jwk.JWKSet(keyA.toPublicJWK()).toString(), org.springframework.http.MediaType.APPLICATION_JSON));
            var scripts = mock(org.redisson.api.RScript.class); when(redis.getScript(org.redisson.client.codec.StringCodec.INSTANCE)).thenReturn(scripts);
            doAnswer(call -> eval(call.getArgument(1), call.getArgument(3), (Object[]) call.getRawArguments()[4])).when(scripts)
                    .eval(any(org.redisson.api.RScript.Mode.class), anyString(), any(org.redisson.api.RScript.ReturnType.class), any(List.class), any(Object[].class));
            doAnswer(call -> {
                String name = call.getArgument(0); var map = mock(org.redisson.api.RMap.class);
                when(map.readAllMap()).thenAnswer(ignored -> hashSnapshot(name));
                return map;
            }).when(redis).getMap(anyString(), any(org.redisson.client.codec.Codec.class));
            doAnswer(call -> {
                String name = call.getArgument(0); var map = mock(RMapCache.class);
                when(map.get(anyString())).thenAnswer(get -> cachedValue(name, get.getArgument(0)));
                when(map.readAllMap()).thenAnswer(ignored -> cachedSnapshot(name)); return map;
            }).when(redis).getMapCache(anyString(), any(org.redisson.client.codec.Codec.class));
            var revScripts = mock(org.redisson.api.RScriptReactive.class); when(reactive.getScript(org.redisson.client.codec.StringCodec.INSTANCE)).thenReturn(revScripts);
            doAnswer(call -> Mono.defer(() -> { int number = snapshots.incrementAndGet(); assertThat((List<?>) call.getArgument(3)).hasSize(7);
                return Mono.just(number == unavailableSnapshotNumber ? List.of("U", "OAUTH_SECURITY_READY_ABSENT") : snapshot.get()); }))
                    .when(revScripts).eval(eq(org.redisson.api.RScript.Mode.READ_ONLY), anyString(), eq(org.redisson.api.RScript.ReturnType.MULTI), any(List.class), any(Object[].class));
            var revocations = new top.egon.cola.platform.tianquan.shoubing.starter.security.reactive.ReactiveOAuthRevocationReader(reactive, registry, idp);
            org.redisson.api.RStream<String, String> stream = mock(org.redisson.api.RStream.class);
            doReturn(stream).when(redis).getStream(eq("egon:test:edge:audit"), eq(org.redisson.client.codec.StringCodec.INSTANCE));
            when(stream.add(any(org.redisson.api.stream.StreamAddArgs.class))).thenAnswer(call -> {
                assertThat(reactor.core.scheduler.Schedulers.isInNonBlockingThread()).isFalse();
                if (failAudit) { throw new org.redisson.client.RedisException("controlled unavailable"); }
                return new org.redisson.api.StreamMessageId(1, audits.incrementAndGet());
            });
            var auditProps = new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties("egon:test:edge:audit", directory.resolve("spool"), 1_048_576, secret);
            var producer = new top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent(redis, mapper, auditProps,
                    new top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties(), clock, validator);
            var converter = (top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter) Class.forName(top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter.class.getName() + "Impl").getConstructor().newInstance();
            var events = (top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter) Class.forName(top.egon.cola.platform.tianquan.shoubing.starter.security.domain.converter.TokenUsageAuditEventConverter.class.getName() + "Impl").getConstructor().newInstance();
            var usage = new top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties(true, ISSUER, false,
                    top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReceiverEnum.EDGE);
            var metrics = new top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityMetricsComponent(() -> meters, validator, usage);
            observer = new top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent(producer, events, usage, metrics, clock, validator, RESOURCE_A);
            top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader states = resourceId -> {
                resourceReads.incrementAndGet(); URI target = resourceId.equals("resource-app-a") ? RESOURCE_A : RESOURCE_B;
                return java.util.Optional.of(new top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerState(resourceId, target, "biz", "app", "test", resourceStatus, 7));
            };
            bindings = new EdgeSessionBindingComponent(redis, registrations, properties, cipher, mapper, clock, validator, registry, idp, managers::authenticate,
                    revocations::check, observer, converter, managers, states, mock(top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader.class), revocations);
            intentStore = new top.egon.cola.component.yuheng.edge.service.EdgeLogoutIntentStore() {
                @Override public Mono<List<String>> enqueueAll(List<top.egon.cola.component.yuheng.edge.service.EdgeLogoutRevocationIntent> values) {
                    return Mono.defer(() -> { if (failQueue) { return Mono.error(new IllegalStateException("controlled queue failure")); }
                        intents.addAll(values); return Mono.just(values.stream().map(value -> value.intentId()).toList()); });
                }
                @Override public Mono<List<PendingIntent>> pollNewAndClaimStale() { return Mono.just(List.of()); }
                @Override public Mono<Void> acknowledge(String id) { return Mono.fromRunnable(() -> acknowledged.add(id)); }
            };
            clients = new EdgeAuthorizedClientRepository(redis, registrations, properties, cipher, clock, bindings, intentStore);
            bindings.validateConfiguration();
        }

        WebSession newSession() { return MockServerWebExchange.from(MockServerHttpRequest.get("https://edge.example/")).getSession().block(); }

        MockServerWebExchange begin(String id, WebSession session) {
            var config = new EdgeSecurityConfiguration(properties);
            var chain = config.edgeBrowserSecurityWebFilterChain(ServerHttpSecurity.http(), registrations, clients,
                    new WebSessionServerCsrfTokenRepository(), clock, idp, mapper);
            var exchange = MockServerWebExchange.builder(MockServerHttpRequest.get("https://edge.example/oauth2/authorization/" + id)).session(session).build();
            new WebFilterChainProxy(chain).filter(exchange, current -> current.getResponse().setComplete()).block(Duration.ofSeconds(5)); return exchange;
        }

        MockServerWebExchange pendingExchange(String id, WebSession session) {
            var started = begin(id, session); assertThat(started.getResponse().getStatusCode().value()).isEqualTo(302);
            var state = org.springframework.web.util.UriComponentsBuilder.fromUri(started.getResponse().getHeaders().getLocation()).build().getQueryParams().getFirst("state");
            var exchange = MockServerWebExchange.builder(MockServerHttpRequest.get("https://edge.example/login/oauth2/code/" + id).queryParam("state", org.springframework.web.util.UriUtils.decode(state, java.nio.charset.StandardCharsets.UTF_8))).session(session).build();
            var pending = new org.springframework.security.oauth2.client.web.server.WebSessionOAuth2ServerAuthorizationRequestRepository().loadAuthorizationRequest(exchange).block();
            exchange.getAttributes().put(EdgeAuthorizedClientRepository.PENDING_REQUEST, pending); return exchange;
        }

        EdgeSessionController controller() {
            return new EdgeSessionController(clients, mock(top.egon.cola.component.yuheng.edge.service.EdgeLogoutService.class), registrations,
                    properties, idp, managers::authenticate, ignored -> Mono.empty(), clock);
        }

        org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager manager() {
            var tokenClient = new org.springframework.security.oauth2.client.endpoint.WebClientReactiveRefreshTokenTokenResponseClient();
            tokenClient.setWebClient(org.springframework.web.reactive.function.client.WebClient.builder().exchangeFunction(request -> neverRefresh ? Mono.defer(() -> { refreshStarted.countDown(); return Mono.never(); }) : Mono.fromCallable(() -> {
                refreshStarted.countDown(); refreshExchanges.incrementAndGet();
                String basic = request.headers().getFirst(HttpHeaders.AUTHORIZATION).substring("Basic ".length());
                String clientId = new String(java.util.Base64.getDecoder().decode(basic), java.nio.charset.StandardCharsets.UTF_8).split(":", 2)[0];
                String realId = clientId.substring("client-".length()); var registration = registrations.findByRegistrationId(realId).block();
                String body = mapper.writeValueAsString(Map.of("access_token", token(realId, "USER", "user-1", Map.of()), "token_type", "Bearer", "expires_in", 300,
                        "scope", String.join(" ", registration.getScopes()), "refresh_token", "rotated-refresh-" + tokenIds.incrementAndGet()));
                return org.springframework.web.reactive.function.client.ClientResponse.create(org.springframework.http.HttpStatus.OK).header("Content-Type", "application/json").body(body).build();
            }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()).delayUntil(ignored -> refreshResponseGate)).build());
            var provider = new org.springframework.security.oauth2.client.RefreshTokenReactiveOAuth2AuthorizedClientProvider(); provider.setClock(clock); provider.setClockSkew(Duration.ZERO); provider.setAccessTokenResponseClient(tokenClient);
            var delegate = new org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager(registrations, clients); delegate.setAuthorizedClientProvider(provider);
            when(reactive.getLock(anyString())).thenAnswer(call -> {
                var lock = mock(org.redisson.api.RLockReactive.class);
                when(lock.tryLock(anyLong(), anyLong(), any(java.util.concurrent.TimeUnit.class), anyLong())).thenReturn(Mono.just(true));
                when(lock.unlock(anyLong())).thenReturn(Mono.empty()); return lock;
            });
            return top.egon.cola.component.yuheng.edge.service.impl.EdgeSingleFlightAuthorizedClientManager.createConfigured(delegate, registrations, reactive, properties, bindings, clients);
        }

        MockServerWebExchange request(String path, WebSession session) { return MockServerWebExchange.builder(MockServerHttpRequest.get("https://edge.example" + path)).session(session).build(); }

        ServerWebExchange forward(String alias, WebSession session, org.springframework.security.core.Authentication principal, String external,
                java.util.concurrent.atomic.AtomicReference<ServerWebExchange> reached) {
            var resource = properties.getResources().get(alias); var request = MockServerHttpRequest.get("https://edge.example/api/" + alias + "/data")
                    .header("x-egon-user-id", "spoofed").header("tenant_id", "spoofed").header(HttpHeaders.COOKIE, "legacy=old");
            if (external != null) { request.header(HttpHeaders.AUTHORIZATION, "Bearer " + external); }
            else { request.cookie(new org.springframework.http.HttpCookie("__Host-egon_bff", "opaque")); }
            var base = MockServerWebExchange.builder(request).session(session).build();
            ServerWebExchange exchange = principal == null ? base : base.mutate().principal(Mono.just(principal)).build();
            exchange.getAttributes().put(org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR,
                    org.springframework.cloud.gateway.route.Route.async().id(alias).uri(resource.getResourceUri()).metadata("edge-registration-id", alias).predicate(current -> true).build());
            var beans = new org.springframework.beans.factory.support.StaticListableBeanFactory(); beans.addBean("manager", manager());
            var relay = new org.springframework.cloud.gateway.filter.factory.TokenRelayGatewayFilterFactory(beans.getBeanProvider(org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager.class));
            var guard = new top.egon.cola.component.yuheng.edge.filter.EdgeResourceAuthorizationGatewayFilterFactory(bindings, clients, relay, mapper);
            var config = new org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory.NameConfig(); config.setName(alias);
            var headers = new top.egon.cola.component.yuheng.edge.filter.TrustedIdentityHeaderFilter(mapper, bindings);
            new org.springframework.web.filter.reactive.ServerWebExchangeContextFilter().filter(exchange, current -> guard.apply(config).filter(current,
                    relayed -> headers.filter(relayed, trusted -> { reached.set(trusted); return Mono.empty(); }))).block(Duration.ofSeconds(10));
            return exchange;
        }

        org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest pending(MockServerWebExchange started) {
            String rawState = org.springframework.web.util.UriComponentsBuilder.fromUri(started.getResponse().getHeaders().getLocation()).build().getQueryParams().getFirst("state");
            var exchange = MockServerWebExchange.builder(MockServerHttpRequest.get("https://edge.example/login/oauth2/code/app-a")
                    .queryParam("state", org.springframework.web.util.UriUtils.decode(rawState, java.nio.charset.StandardCharsets.UTF_8))).session(started.getSession().block()).build();
            return new org.springframework.security.oauth2.client.web.server.WebSessionOAuth2ServerAuthorizationRequestRepository().loadAuthorizationRequest(exchange).block();
        }

        MockServerWebExchange completeNative(MockServerWebExchange started, String subject, boolean wrongNonce) throws Exception {
            var pending = pending(started);
            String id = pending.getAttribute(EdgeSessionBindingComponent.REGISTRATION_ID);
            var exchange = MockServerWebExchange.builder(MockServerHttpRequest.get("https://edge.example/login/oauth2/code/" + id)
                    .queryParam("state", pending.getState()).queryParam("code", "controlled-code").queryParam("iss", pending.<String>getAttribute(EdgeSessionBindingComponent.ISSUER)))
                    .session(started.getSession().block()).build();
            filterNative(exchange, pending, subject, wrongNonce); return exchange;
        }

        void filterNative(MockServerWebExchange exchange, org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest pending,
                String subject, boolean wrongNonce) throws Exception {
            String id = pending.getAttribute(EdgeSessionBindingComponent.REGISTRATION_ID);
            var registration = registrations.findByRegistrationId(id).block();
            String issuer = registration.getProviderDetails().getIssuerUri(); var signingKey = issuer.equals(ISSUER.toASCIIString()) ? keyA : keyB;
            var idClaims = new LinkedHashMap<String, Object>(); idClaims.put("iss", issuer); idClaims.put("sub", subject); idClaims.put("aud", List.of(registration.getClientId()));
            idClaims.put("iat", time.get().minusSeconds(1).getEpochSecond()); idClaims.put("exp", time.get().plusSeconds(300).getEpochSecond());
            idClaims.put("nonce", wrongNonce ? "wrong-nonce" : pending.getAdditionalParameters().get("nonce")); idClaims.put("name", "Controlled user");
            var signedId = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID(signingKey.getKeyID()).build(), JWTClaimsSet.parse(idClaims));
            signedId.sign(new RSASSASigner(signingKey));
            String access = token(id, "USER", subject, Map.of());
            String response = mapper.writeValueAsString(Map.of("access_token", access, "token_type", "Bearer", "expires_in", 300,
                    "scope", String.join(" ", registration.getScopes()), "refresh_token", "controlled-refresh-native-" + tokenIds.incrementAndGet(), "id_token", signedId.serialize()));
            var tokenClient = new org.springframework.security.oauth2.client.endpoint.WebClientReactiveAuthorizationCodeTokenResponseClient();
            tokenClient.setWebClient(org.springframework.web.reactive.function.client.WebClient.builder().exchangeFunction(request -> {
                codeExchanges.incrementAndGet();
                assertThat(request.url().toASCIIString()).isEqualTo(issuer + "/oauth2/token");
                var captured = new org.springframework.mock.http.client.reactive.MockClientHttpRequest(org.springframework.http.HttpMethod.POST, request.url());
                org.springframework.web.reactive.function.BodyInserter.Context context = new org.springframework.web.reactive.function.BodyInserter.Context() {
                    @Override public java.util.List<org.springframework.http.codec.HttpMessageWriter<?>> messageWriters() { return org.springframework.web.reactive.function.client.ExchangeStrategies.withDefaults().messageWriters(); }
                    @Override public java.util.Optional<org.springframework.http.server.reactive.ServerHttpRequest> serverRequest() { return java.util.Optional.empty(); }
                    @Override public Map<String, Object> hints() { return Map.of(); }
                };
                return request.body().insert(captured, context).then(Mono.defer(captured::getBodyAsString)).map(form -> {
                    var fields = org.springframework.web.util.UriComponentsBuilder.fromUriString("https://edge.example/?" + form).build().getQueryParams();
                    String verifier = fields.getFirst("code_verifier");
                    assertThat(verifier).isEqualTo(pending.<String>getAttribute("code_verifier"));
                    assertThat(fields.getFirst("code")).isEqualTo("controlled-code");
                    return org.springframework.web.reactive.function.client.ClientResponse.create(org.springframework.http.HttpStatus.OK)
                            .header("Content-Type", "application/json").body(response).build();
                }).doOnError(codeBoundaryFailure::set);
            }).build());
            var nativeManager = new org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeReactiveAuthenticationManager(tokenClient,
                    new org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService());
            var publicKey = signingKey.toRSAPublicKey();
            nativeManager.setJwtDecoderFactory(realRegistration -> {
                var decoder = org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder.withPublicKey(publicKey).build();
                var validator = new org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator(realRegistration); validator.setClock(clock); validator.setClockSkew(Duration.ZERO);
                decoder.setJwtValidator(validator); return decoder;
            });
            var http = ServerHttpSecurity.http(); http.oauth2Login(oauth -> oauth.authenticationManager(authentication -> nativeManager.authenticate(authentication).doOnError(nativeFailure::set)));
            var chain = new EdgeSecurityConfiguration(properties).edgeBrowserSecurityWebFilterChain(http, registrations, clients,
                    new WebSessionServerCsrfTokenRepository(), clock, idp, mapper);
            new WebFilterChainProxy(chain).filter(exchange, current -> current.getResponse().setComplete()).block(Duration.ofSeconds(10));
            if (exchange.getResponse().getStatusCode().value() == 503 && nativeFailure.get() != null) { throw new IllegalStateException("Unexpected native 503", nativeFailure.get()); }
            if (codeBoundaryFailure.get() != null) { throw new IllegalStateException("Controlled code boundary failed", codeBoundaryFailure.get()); }
        }

        top.egon.cola.component.yuheng.edge.service.EdgeLogoutService.RemoteState logout(org.springframework.security.core.Authentication principal, ServerWebExchange exchange) {
            var builder = org.springframework.web.reactive.function.client.WebClient.builder().exchangeFunction(request -> {
                assertThat(leaseOwners).isEmpty();
                assertThat(hashes.values()).allSatisfy(row -> assertThat(row.get("state")).isEqualTo("CLOSED"));
                remoteRevocations.incrementAndGet();
                String basic = request.headers().getFirst(HttpHeaders.AUTHORIZATION).substring("Basic ".length());
                String clientId = new String(java.util.Base64.getDecoder().decode(basic), java.nio.charset.StandardCharsets.UTF_8).split(":", 2)[0];
                if (clientId.equals("client-" + failRemoteRegistration)) { return Mono.error(new java.io.IOException("controlled remote revocation failure")); }
                return Mono.just(org.springframework.web.reactive.function.client.ClientResponse.create(org.springframework.http.HttpStatus.NO_CONTENT).build());
            });
            var remote = new top.egon.cola.component.yuheng.edge.service.impl.EdgeOAuthTokenRevocationClient(registrations, properties, builder); remote.initializeWebClient();
            return new top.egon.cola.component.yuheng.edge.service.EdgeLogoutService(clients, intentStore, remote, clock).logout(principal, exchange).block(Duration.ofSeconds(10));
        }

        OAuth2AuthenticationToken authentication(String registration, String sub) {
            var user = new DefaultOAuth2User(Set.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("sub", sub, "name", "Controlled user"), "sub");
            return new OAuth2AuthenticationToken(user, user.getAuthorities(), registration);
        }

        OAuth2AuthorizedClient client(String id, String sub) throws Exception { return clientWithToken(id, sub, token(id, "USER", sub, Map.of())); }
        OAuth2AuthorizedClient clientWithToken(String id, String sub, String token) {
            var registration = registrations.findByRegistrationId(id).block();
            return new OAuth2AuthorizedClient(registration, sub, new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, token, time.get().minusSeconds(1), time.get().plusSeconds(300), registration.getScopes()),
                    new OAuth2RefreshToken("controlled-refresh-" + tokenIds.incrementAndGet(), time.get()));
        }

        String token(String id, String type, String sub, Map<String, Object> overrides) throws Exception {
            var registration = registrations.findByRegistrationId(id).block(); String issuer = registration.getProviderDetails().getIssuerUri();
            String alias = properties.getResources().containsKey(id) ? id : bindings.aliasForRegistration(id);
            var claims = new LinkedHashMap<String, Object>(); claims.put("iss", issuer); claims.put("aud", List.of(properties.getResources().get(alias).getResourceUri().toASCIIString()));
            claims.put("sub", sub); claims.put("client_id", registration.getClientId()); claims.put("jti", Integer.toString(tokenIds.incrementAndGet()));
            claims.put("tenant_id", URI.create(issuer).getPath().substring("/tenants/".length())); claims.put("iat", time.get().minusSeconds(1).getEpochSecond());
            claims.put("nbf", time.get().minusSeconds(1).getEpochSecond()); claims.put("exp", time.get().plusSeconds(300).getEpochSecond());
            claims.put("principal_type", type); claims.put("scope_context", "TENANT"); claims.put("scope", String.join(" ", registration.getScopes()));
            claims.put("authorization_id", "801"); claims.put("client_registration_id", "501");
            claims.put("security_version", type.equals("USER") ? Map.of("realm", 1L, "client", 2L, "subject", 3L) : Map.of("realm", 1L, "client", 2L));
            if (type.equals("USER")) { claims.put("acr", "PASSWORD"); claims.put("auth_time", time.get().minusSeconds(60).getEpochSecond()); }
            else { claims.put("resource_version", 7L); claims.put("source_biz", "biz"); claims.put("source_app", "app"); claims.put("source_env", "test"); claims.put("app_id", "app"); claims.put("credential_id", "credential-1"); }
            claims.putAll(overrides); var key = issuer.equals(ISSUER.toASCIIString()) ? keyA : keyB;
            var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(new JOSEObjectType("at+jwt")).keyID(key.getKeyID()).build(), JWTClaimsSet.parse(claims));
            jwt.sign(new RSASSASigner(key)); return jwt.serialize();
        }

        private synchronized Map<String, String> hashSnapshot(String name) { return Map.copyOf(hashes.computeIfAbsent(name, key -> new LinkedHashMap<>())); }
        private synchronized Map<String, String> cachedSnapshot(String name) { return Map.copyOf(cached.computeIfAbsent(name, key -> new LinkedHashMap<>())); }
        private synchronized String cachedValue(String name, String field) { return cached.computeIfAbsent(name, key -> new LinkedHashMap<>()).get(field); }

        private synchronized Object eval(String script, List<Object> keys, Object[] args) {
            String key = keys.getFirst().toString();
            if (script.contains("local value = redis.call('get'")) { return pointers.computeIfAbsent(key, ignored -> args[0].toString()); }
            if (keys.size() == 1 && script.contains("redis.call('del', KEYS[1])")) {
                String id = bindingId(key); if (args[0].equals(leaseOwners.get(id))) { leaseOwners.remove(id); leaseDeadline.remove(id); return 1L; } return 0L;
            }
            Map<String, String> row = hashes.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
            if (script.contains("if redis.call('exists'")) {
                if (!row.isEmpty()) { return 0L; }
                row.putAll(Map.of("epoch", args[0].toString(), "generation", "1", "fence", "1", "state", "NEW", "deadline", args[1].toString(), "payload", args[2].toString())); return 1L;
            }
            if (script.contains("hincrby")) {
                if (!args[0].toString().equals(row.get("epoch")) || !args[1].toString().equals(row.get("generation")) || !Set.of("NEW", "ACTIVE").contains(row.get("state"))
                        || Long.parseLong(row.get("deadline")) <= ((Number) args[2]).longValue()) { return -1L; }
                String id = bindingId(key); if (leaseDeadline.getOrDefault(id, 0L) > clock.millis()) { lifecycleContended.countDown(); return 0L; }
                leaseOwners.put(id, args[3].toString()); leaseDeadline.put(id, clock.millis() + ((Number) args[4]).longValue());
                long fence = Long.parseLong(row.get("fence")) + 1; row.put("fence", Long.toString(fence)); return fence;
            }
            if (keys.size() == 1) { String id = bindingId(key); if (args[0].equals(leaseOwners.get(id))) { leaseOwners.remove(id); leaseDeadline.remove(id); return 1L; } return 0L; }
            String id = bindingId(key);
            if (leaseDeadline.getOrDefault(id, 0L) <= clock.millis() || !args[0].equals(leaseOwners.get(id)) || !args[1].equals(row.get("epoch"))
                    || !args[2].toString().equals(row.get("generation")) || !args[3].toString().equals(row.get("fence"))) { return 0L; }
            if (script.contains("struct.pack")) {
                if (!Set.of("NEW", "ACTIVE").contains(row.get("state")) || Long.parseLong(row.get("deadline")) <= ((Number) args[4]).longValue()) { return 0L; }
                var clients = cached.computeIfAbsent(keys.get(2).toString(), ignored -> new LinkedHashMap<>());
                String field = args[7].toString(); if (!clients.containsKey(field) && clients.size() >= ((Number) args[6]).intValue()) { return 0L; }
                clients.put(field, args[8].toString()); row.put("state", "ACTIVE"); row.put("payload", args[10].toString()); return 1L;
            }
            if (script.contains("redis.call('del', KEYS[3]")) {
                if (!Set.of("CLOSING", "CLOSED").contains(row.get("state"))) { return 0L; } cached.remove(keys.get(2).toString()); return 1L;
            }
            if (script.contains("redis.call('hdel'")) { if (!row.get("state").equals("ACTIVE")) { return 0L; } cached.computeIfAbsent(keys.get(2).toString(), ignored -> new LinkedHashMap<>()).remove(args[4]); return 1L; }
            if (!args[4].equals(row.get("state"))) { return 0L; } row.put("generation", args[5].toString()); row.put("state", args[6].toString()); row.put("payload", args[7].toString()); return 1L;
        }
        private static String bindingId(String key) { return key.substring(key.indexOf('{') + 1, key.indexOf('}')); }
        @Override public void close() { validator.close(); meters.close(); }
    }

    private static EdgeRegistrationResourceBindingBO resource(URI uri, String id, String scope, String path) {
        return EdgeRegistrationResourceBindingBO.builder().resourceServerId("resource-" + id).resourceUri(uri).defaultRegistrationId(id)
                .issuerRegistrationIds(Map.of(ISSUER.toASCIIString(), id, Fixture.ISSUER_B.toASCIIString(), id + "-b"))
                .routeIds(Set.of(id)).requestPaths(List.of("/api/" + id + "/**")).scopes(Set.of("openid", "offline_access", scope)).returnPath(path).build();
    }

    private static ClientRegistration registration(String id, String scope) {
        URI issuer = id.endsWith("-b") && !id.equals("app-b") ? Fixture.ISSUER_B : ISSUER;
        return ClientRegistration.withRegistrationId(id).clientId("client-" + id).clientSecret("controlled-secret")
                .clientAuthenticationMethod(new ClientAuthenticationMethod("client_secret_basic"))
                .authorizationGrantType(new AuthorizationGrantType("authorization_code"))
                .redirectUri("https://edge.example/login/oauth2/code/{registrationId}").scope("openid", "offline_access", scope)
                .authorizationUri(issuer + "/oauth2/authorize").tokenUri(issuer + "/oauth2/token").jwkSetUri(issuer + "/.well-known/jwks.json")
                .providerConfigurationMetadata(Map.of("revocation_endpoint", issuer + "/oauth2/revoke"))
                .issuerUri(issuer.toASCIIString()).userNameAttributeName("sub").clientName(id).build();
    }
}
