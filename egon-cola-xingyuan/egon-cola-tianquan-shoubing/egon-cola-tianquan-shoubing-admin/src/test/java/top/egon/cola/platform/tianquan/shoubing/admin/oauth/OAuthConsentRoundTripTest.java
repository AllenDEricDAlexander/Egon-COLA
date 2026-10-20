package top.egon.cola.platform.tianquan.shoubing.admin.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.validation.Validation;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.config.AuthorizationServerConfiguration;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.filter.TenantLoginAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthConsentBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthSecuritySnapshotBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthAuthorizationConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasAuthorizationConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasConsentConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasRegisteredClientConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthAuthorizationPO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthAuthorizationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthConsentRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.TenantRegisteredClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthTokenIssueServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.TenantAuthorizationServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.TenantConsentServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.ClientResourceGrantViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.ResourceServerViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.UpsertClientResourceGrantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.DeleteClientResourceGrantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthResourceRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.impl.ResourceServerServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.TransactionalOutboxResourceServerEventAdapter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.config.OAuthKeyEncryptionProperties;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthTokenConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthTokenPO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSigningKeyRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthKeyCipherComponent;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.TenantJwkSourceComponent;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthSecurityProjectionComponent;
import top.egon.cola.platform.tianquan.shoubing.core.identity.AuthenticatedIdentity;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityFacade;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.port.ResourceServerStore;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientCredentialsAccessPolicy;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServer;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.core.port.TenantMembershipPort;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = OAuthConsentRoundTripTest.NativeConfiguration.class)
@WebAppConfiguration
class OAuthConsentRoundTripTest {

    private static final URI ORIGIN = URI.create("https://idp.example.test");
    private static final URI RESOURCE = URI.create("https://orders.example.test");
    private static final String CLIENT = "consent-client";
    private static final String CALLBACK = "https://client.example.test/callback";
    private static final String CLIENT_STATE = "fixture state /with+reserved?values";
    private static final String NONCE = "fixture-oidc-nonce-" + "N".repeat(32);
    private static final String VERIFIER = "V".repeat(64);
    private static Fixture lastFixture;

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private Fixture fixture;
    private MockMvc mvc;

    @BeforeEach
    void prepareNativeFixture() {
        top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
        lastFixture = fixture;
        fixture.reset();
        AuthorizationServerContextHolder.resetContext();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterAll
    static void removeOnlyOwnedKeyFixture() throws Exception {
        if (lastFixture != null) {
            lastFixture.close();
        }
    }

    @Test
    void nativeLoginConsentCodeOidcUserinfoAndRefreshRoundTrip() throws Exception {
        Pending pending = assertDoesNotThrow(() -> startThroughRealLogin(1001L));
        MvcResult page = mvc.perform(request(get(URI.create(pending.consentLocation()))).session(pending.session()))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn();
        assertEquals(200, page.getResponse().getStatus(), "Consent handler=" + page.getHandler()
                + "; exception=" + page.getResolvedException() + "; parameters="
                + page.getRequest().getParameterMap().keySet() + "; response=" + page.getResponse().getContentAsString());
        String html = page.getResponse().getContentAsString();
        assertTrue(html.contains("Consent &amp; &lt;fixture&gt;"));
        assertFalse(html.contains("checked"));
        assertFalse(html.contains("client_secret"));
        String csrf = hidden(html, "_csrf");
        MvcResult approved = mvc.perform(consent(pending).param("_csrf", csrf).param("scope", "orders.read"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String callback = approved.getResponse().getHeader("Location");
        assertEquals(CLIENT_STATE, query(callback, "state"));
        String code = query(callback, "code");
        assertNotNull(code);
        Instant absoluteDeadline = fixture.authorizations.values().iterator().next().getAbsoluteExpiresAt();

        JsonNode tokens = token(code, VERIFIER);
        assertEquals("openid orders.read", tokens.path("scope").asText());
        assertTrue(tokens.hasNonNull("access_token"));
        assertTrue(tokens.hasNonNull("id_token"));
        assertTrue(tokens.hasNonNull("refresh_token"));
        Jwt access = decoded(tokens.path("access_token").asText());
        Jwt id = decoded(tokens.path("id_token").asText());
        assertEquals(Set.of(RESOURCE.toASCIIString()), Set.copyOf(access.getAudience()));
        assertEquals("USER", access.getClaimAsString("principal_type"));
        assertEquals("at+jwt", access.getHeaders().get("typ"));
        assertEquals(List.of(CLIENT), id.getAudience());
        assertEquals(NONCE, id.getClaimAsString("nonce"));
        assertEquals(CLIENT, id.getClaimAsString("azp"));
        assertFalse(id.hasClaim("security_version"));
        assertFalse(id.hasClaim("token_use"));
        assertFalse(id.hasClaim("_token_use"));
        assertEquals("user-1001", id.getSubject());
        TrustedRealmBO realm = fixture.realm(ORIGIN.resolve("/tenants/1001"));
        OAuthAuthorizationBO loaded = fixture.readAuthorization(realm,
                fixture.authorizations.values().iterator().next().getId()).orElseThrow();
        loaded.setTokens(fixture.tokens.values().stream().filter(row -> row.getAuthorizationPk().equals(loaded.getId()))
                .map(row -> fixture.tokenConverter.toTarget(realm, fixture.cipher, fixture.mapper, row.getTokenDigest(), row)).toList());
        var nativeLoaded = context.getBean("sasAuthorizationConverterImpl", SasAuthorizationConverter.class).toTarget(
                loaded, context.getBean("sasRegisteredClientConverterImpl", SasRegisteredClientConverter.class).toTarget(fixture.client(realm)),
                fixture.mapper, realm.getIssuer());
        assertTrue(nativeLoaded.getAccessToken().isActive(), "Reloaded native AT metadata must retain Instant nbf");
        assertFalse(nativeLoaded.getAccessToken().getClaims().containsKey("_token_use"));
        assertInstanceOf(Instant.class, nativeLoaded.getToken(org.springframework.security.oauth2.core.oidc.OidcIdToken.class)
                .getToken().getClaims().get("auth_time"));
        MvcResult userinfo = mvc.perform(request(get("/tenants/1001/userinfo"))
                        .header("Authorization", "Bearer " + tokens.path("access_token").asText())).andReturn();
        assertEquals(200, userinfo.getResponse().getStatus(), "UserInfo response=" + userinfo.getResponse().getContentAsString()
                + "; bearer=" + userinfo.getResponse().getHeader("WWW-Authenticate"));
        assertEquals("user-1001", fixture.mapper.readTree(userinfo.getResponse().getContentAsString()).path("sub").asText());
        mvc.perform(request(get("/tenants/1001/userinfo"))
                        .header("Authorization", "Bearer " + tokens.path("id_token").asText()))
                .andExpect(status().is4xxClientError());

        MvcResult refreshed = mvc.perform(request(post("/tenants/1001/oauth2/token"))
                        .header("Authorization", basic())
                        .contentType("application/x-www-form-urlencoded")
                        .param("grant_type", "refresh_token")
                        .param("refresh_token", tokens.path("refresh_token").asText()))
                .andExpect(status().isOk()).andReturn();
        JsonNode rotated = fixture.mapper.readTree(refreshed.getResponse().getContentAsString());
        assertNotEquals(tokens.path("refresh_token").asText(), rotated.path("refresh_token").asText());
        assertEquals(absoluteDeadline, fixture.authorizations.values().iterator().next().getAbsoluteExpiresAt());
        assertTrue(fixture.tokens.values().stream().allMatch(row -> !row.getExpiresAt().isAfter(absoluteDeadline)));
        assertTrue(fixture.authorizations.values().stream().noneMatch(row ->
                row.getAttributesCiphertext().contains(CLIENT_STATE) || row.getAttributesCiphertext().contains(pending.state())));
        mvc.perform(request(post("/tenants/1001/oauth2/token"))
                        .header("Authorization", basic()).contentType("application/x-www-form-urlencoded")
                        .param("grant_type", "refresh_token").param("refresh_token", tokens.path("refresh_token").asText()))
                .andExpect(status().is4xxClientError());
        tokenRejected(code, VERIFIER);
        mvc.perform(consent(pending).param("_csrf", csrf).param("scope", "orders.read"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void nativeConsentPostRequiresCsrfBeforeAnyRepositoryWrite() throws Exception {
        mvc.perform(request(post("/tenants/1001/oauth2/authorize"))
                        .session(new MockHttpSession()).with(user("user-1001"))
                        .contentType("application/x-www-form-urlencoded")
                        .param("client_id", CLIENT).param("state", "S".repeat(64)).param("scope", "orders.read"))
                .andExpect(status().isForbidden());
        assertTrue(fixture.authorizations.isEmpty());
        assertTrue(fixture.tokens.isEmpty());
        assertTrue(fixture.consents.isEmpty());
    }

    @Test
    void nativeConsentPageRejectsMissingSessionAndWrongClientWithoutWrites() throws Exception {
        Pending pending = startThroughRealLogin(1001L);
        Map<String, OAuthAuthorizationPO> initial = new LinkedHashMap<>(fixture.authorizations);
        Map<String, OAuthTokenPO> initialTokens = new LinkedHashMap<>(fixture.tokens);
        mvc.perform(request(get(URI.create(pending.consentLocation())))).andExpect(status().isUnauthorized());
        mvc.perform(request(get("/tenants/1001/consent")).session(pending.session())
                        .queryParam("state", pending.state()).queryParam("client_id", "another-client"))
                .andExpect(status().isNotFound());
        assertEquals(initial, fixture.authorizations);
        assertEquals(initialTokens, fixture.tokens);
        assertTrue(fixture.consents.isEmpty());
        assertTrue(fixture.events.isEmpty());
    }

    @Test
    void nativePendingStateCannotCrossRealmSubjectOrSession() throws Exception {
        Pending pending = startThroughRealLogin(1001L);
        mvc.perform(request(get(URI.create(pending.consentLocation().replace("/tenants/1001/", "/tenants/1002/"))))
                        .session(pending.session())).andExpect(status().isNotFound());
        mvc.perform(request(get(URI.create(pending.consentLocation()))).session(new MockHttpSession())
                        .with(user("user-1001"))).andExpect(status().isNotFound());
        mvc.perform(request(get(URI.create(pending.consentLocation()))).session(pending.session())
                        .with(user("different-subject"))).andExpect(status().isNotFound());
        assertEquals(1, fixture.authorizations.size());
        assertEquals(1, fixture.tokens.size());
        assertTrue(fixture.consents.isEmpty());
    }

    @Test
    void nativeConsentQueryAndFormRejectUnknownDuplicateAndOvergrant() throws Exception {
        Pending pending = startThroughRealLogin(1001L);
        String csrf = consentCsrf(pending);
        mvc.perform(request(get(URI.create(pending.consentLocation()))).session(pending.session())
                        .queryParam("unknown", "<script>"))
                .andExpect(status().isBadRequest());
        mvc.perform(request(get(URI.create(pending.consentLocation()))).session(pending.session())
                        .queryParam("state", pending.state()))
                .andExpect(status().isBadRequest());
        mvc.perform(consent(pending).param("_csrf", csrf).param("scope", "orders.read").param("unknown", "x"))
                .andExpect(status().isBadRequest());
        mvc.perform(consent(pending).param("_csrf", csrf).param("state", pending.state()).param("scope", "orders.read"))
                .andExpect(status().isBadRequest());
        MvcResult overgrant = mvc.perform(consent(pending).param("_csrf", csrf).param("scope", "orders.write"))
                .andReturn();
        assertNull(query(overgrant.getResponse().getHeader("Location"), "code"));
        assertTrue(overgrant.getResponse().getStatus() >= 400
                || query(overgrant.getResponse().getHeader("Location"), "error") != null);
        assertTrue(fixture.consents.isEmpty());
        assertTrue(fixture.tokens.values().stream().noneMatch(row -> row.getTokenType() == OAuthTokenTypeEnum.CODE));
        assertTrue(fixture.events.isEmpty());
    }

    @Test
    void wrongCsrfAndExpiredPendingStateCannotIssueCode() throws Exception {
        Pending pending = startThroughRealLogin(1001L);
        String csrf = consentCsrf(pending);
        mvc.perform(consent(pending).param("_csrf", "wrong-csrf").param("scope", "orders.read"))
                .andExpect(status().isForbidden());
        fixture.clock.now = fixture.clock.now.plusSeconds(300);
        mvc.perform(request(get(URI.create(pending.consentLocation()))).session(pending.session()))
                .andExpect(status().isNotFound());
        mvc.perform(consent(pending).param("_csrf", csrf).param("scope", "orders.read"))
                .andExpect(status().isBadRequest());
        assertTrue(fixture.consents.isEmpty());
        assertTrue(fixture.tokens.values().stream().noneMatch(row -> row.getTokenType() == OAuthTokenTypeEnum.CODE));
        assertTrue(fixture.events.isEmpty());
    }

    @Test
    void hostSpoofCannotChooseConsentIssuerOrRegisteredCallback() throws Exception {
        Pending pending = startThroughRealLogin(1001L);
        MvcResult page = mvc.perform(request(get(URI.create(pending.consentLocation()))).session(pending.session())
                        .header("Host", "attacker.example.test").with(request -> {
                            request.setServerName("attacker.example.test"); return request;
                        })).andExpect(status().isOk()).andReturn();
        String html = page.getResponse().getContentAsString();
        assertTrue(html.contains("action=\"/tenants/1001/oauth2/authorize\""));
        assertFalse(html.contains("attacker.example.test"));
        MvcResult accepted = mvc.perform(consent(pending).param("_csrf", hidden(html, "_csrf"))
                        .param("scope", "orders.read").with(request -> {
                            request.setServerName("attacker.example.test"); return request;
                        })).andExpect(status().is3xxRedirection()).andReturn();
        URI callback = URI.create(accepted.getResponse().getHeader("Location"));
        assertEquals(URI.create(CALLBACK).getScheme(), callback.getScheme());
        assertEquals(URI.create(CALLBACK).getRawAuthority(), callback.getRawAuthority());
        assertEquals(URI.create(CALLBACK).getRawPath(), callback.getRawPath());
        assertEquals(CLIENT_STATE, query(callback.toASCIIString(), "state"));
        assertEquals(ORIGIN.resolve("/tenants/1001").toASCIIString(), query(callback.toASCIIString(), "iss"));
    }

    @Test
    void explicitCancellationCommitsPendingInvalidationAndPreservesPreviousConsent() throws Exception {
        TrustedRealmBO realm = fixture.realm(ORIGIN.resolve("/tenants/1001"));
        OAuthConsentBO existing = OAuthConsentBO.builder().id(600L).version(0L).clientPk(501L)
                .subject("user-1001").authorities(List.of("SCOPE_orders.write"))
                .grantedAt(fixture.clock.instant()).status("ACTIVE").build();
        fixture.consents.put(Fixture.consentKey(realm, 501L, "user-1001"), existing);
        Pending pending = startThroughRealLogin(1001L);
        String csrf = consentCsrf(pending);
        MvcResult cancelled = mvc.perform(consent(pending).param("_csrf", csrf).param("consent_action", "deny"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertEquals("access_denied", query(cancelled.getResponse().getHeader("Location"), "error"));
        assertEquals(CLIENT_STATE, query(cancelled.getResponse().getHeader("Location"), "state"));
        assertTrue(fixture.authorizations.isEmpty());
        assertEquals(existing, fixture.consents.get(Fixture.consentKey(realm, 501L, "user-1001")));
        assertTrue(fixture.tokens.values().stream().allMatch(row -> row.getConsumedAt() != null));
        assertTrue(fixture.events.isEmpty());
        mvc.perform(consent(pending).param("_csrf", csrf).param("consent_action", "deny"))
                .andExpect(status().isBadRequest());
        mvc.perform(request(get(URI.create(pending.consentLocation()))).session(pending.session()))
                .andExpect(status().isNotFound());
    }

    @Test
    void consentSaveCodeSaveAndOutboxFailuresRollbackEveryFact() throws Exception {
        for (String failure : List.of("consent", "code", "outbox")) {
            fixture.reset();
            Pending pending = startThroughRealLogin(1001L);
            String csrf = consentCsrf(pending);
            Map<String, OAuthAuthorizationPO> original = new LinkedHashMap<>(fixture.authorizations);
            Map<String, OAuthTokenPO> originalTokens = new LinkedHashMap<>(fixture.tokens);
            fixture.failConsentSave = failure.equals("consent");
            fixture.failCodeSave = failure.equals("code");
            fixture.failEnqueue = failure.equals("outbox");
            mvc.perform(consent(pending).param("_csrf", csrf).param("scope", "orders.read"))
                    .andExpect(status().isServiceUnavailable());
            assertEquals(original, fixture.authorizations, failure);
            assertEquals(originalTokens, fixture.tokens, failure);
            assertTrue(fixture.consents.isEmpty(), failure);
            assertTrue(fixture.events.isEmpty(), failure);
            assertTrue(fixture.audits.isEmpty(), failure);
            fixture.failConsentSave = false; fixture.failCodeSave = false; fixture.failEnqueue = false;
            approve(pending, csrf);
        }
    }

    @Test
    void signingFailureRollsBackCodeConsumptionAndCanRetry() throws Exception {
        Pending pending = startThroughRealLogin(1001L);
        String code = approve(pending, consentCsrf(pending));
        Map<String, OAuthAuthorizationPO> original = new LinkedHashMap<>(fixture.authorizations);
        Map<String, OAuthTokenPO> originalTokens = new LinkedHashMap<>(fixture.tokens);
        int priorAudits = fixture.audits.size();
        int priorEvents = fixture.events.size();
        fixture.failSigning = true;
        mvc.perform(tokenRequest(code, VERIFIER)).andExpect(status().isServiceUnavailable());
        assertEquals(original, fixture.authorizations);
        assertEquals(originalTokens, fixture.tokens);
        assertEquals(priorAudits, fixture.audits.size());
        assertEquals(priorEvents, fixture.events.size());
        fixture.failSigning = false;
        assertTrue(token(code, VERIFIER).hasNonNull("access_token"));
    }

    @Test
    void wrongPkceVerifierAndCrossIssuerCodeNeverConsumeSuccessfulGrant() throws Exception {
        Pending pending = startThroughRealLogin(1001L);
        String code = approve(pending, consentCsrf(pending));
        tokenRejected(code, "W".repeat(64));
        mvc.perform(request(post("/tenants/1002/oauth2/token")).header("Authorization", basic())
                        .contentType("application/x-www-form-urlencoded").param("grant_type", "authorization_code")
                        .param("code", code).param("redirect_uri", CALLBACK).param("code_verifier", VERIFIER))
                .andExpect(status().is4xxClientError());
        assertTrue(token(code, VERIFIER).hasNonNull("access_token"));
    }

    @Test
    void familyExpiryRejectsRefreshWithoutChangingFacts() throws Exception {
        Pending pending = startThroughRealLogin(1001L);
        JsonNode tokens = token(approve(pending, consentCsrf(pending)), VERIFIER);
        fixture.clock.now = fixture.authorizations.values().iterator().next().getAbsoluteExpiresAt();
        Map<String, OAuthAuthorizationPO> original = new LinkedHashMap<>(fixture.authorizations);
        Map<String, OAuthTokenPO> originalTokens = new LinkedHashMap<>(fixture.tokens);
        mvc.perform(request(post("/tenants/1001/oauth2/token")).header("Authorization", basic())
                        .contentType("application/x-www-form-urlencoded").param("grant_type", "refresh_token")
                        .param("refresh_token", tokens.path("refresh_token").asText()))
                .andExpect(status().is4xxClientError());
        assertEquals(original, fixture.authorizations);
        assertEquals(originalTokens, fixture.tokens);
    }

    @Test
    void publicUserCompletesPkceOidcWithoutAnyRefreshToken() throws Exception {
        fixture.publicClient = true;
        Pending pending = startThroughRealLogin(1001L);
        String code = approve(pending, consentCsrf(pending));
        MvcResult issued = mvc.perform(request(post("/tenants/1001/oauth2/token"))
                        .contentType("application/x-www-form-urlencoded").param("client_id", CLIENT)
                        .param("grant_type", "authorization_code").param("code", code)
                        .param("redirect_uri", CALLBACK).param("code_verifier", VERIFIER))
                .andExpect(status().isOk()).andReturn();
        JsonNode tokens = fixture.mapper.readTree(issued.getResponse().getContentAsString());
        assertTrue(tokens.hasNonNull("access_token"));
        assertTrue(tokens.hasNonNull("id_token"));
        assertFalse(tokens.has("refresh_token"));
        assertTrue(fixture.tokens.values().stream().noneMatch(row -> row.getTokenType() == OAuthTokenTypeEnum.REFRESH));
    }

    @Test
    void nativeAuthorizeRejectsMissingAndPlainPkceBeforeLoginOrPersistence() throws Exception {
        for (String method : List.of("plain", "missing")) {
            var request = authorize(1001L);
            request.with(value -> {
                if (method.equals("missing")) {
                    value.removeParameter("code_challenge"); value.removeParameter("code_challenge_method");
                } else { value.setParameter("code_challenge_method", "plain"); }
                return value;
            });
            MvcResult rejected = mvc.perform(request).andReturn();
            assertTrue(rejected.getResponse().getStatus() >= 400
                    || query(rejected.getResponse().getHeader("Location"), "error") != null);
            assertTrue(fixture.authorizations.isEmpty());
            assertTrue(fixture.tokens.isEmpty());
        }
    }

    @Test
    void oidcPromptNoneRejectsMissingAuthenticationOrConsent() throws Exception {
        MvcResult anonymous = mvc.perform(authorize(1001L).queryParam("prompt", "none"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertEquals("login_required", query(anonymous.getResponse().getHeader("Location"), "error"));
        assertTrue(fixture.authorizations.isEmpty());
        Pending pending = startThroughRealLogin(1001L);
        MvcResult noConsent = mvc.perform(authorize(1001L).session(pending.session()).queryParam("prompt", "none"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertEquals("consent_required", query(noConsent.getResponse().getHeader("Location"), "error"));
        assertEquals(1, fixture.authorizations.size());
        assertTrue(fixture.consents.isEmpty());
    }

    @Test
    void nativeStepUpRequiresPasswordProofBeforeFreshAuthorization() throws Exception {
        Pending initial = startThroughRealLogin(1001L);
        Instant originalAuthTime = fixture.clock.instant();
        fixture.clock.now = originalAuthTime.plusSeconds(60);
        int before = fixture.authorizations.size();
        MvcResult freshLogin = mvc.perform(authorize(1001L).session(initial.session())
                        .queryParam("prompt", "login").queryParam("max_age", "0"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertEquals("/tenants/1001/login", URI.create(freshLogin.getResponse().getHeader("Location")).getRawPath());
        assertEquals(before, fixture.authorizations.size(), "Freshness intent cannot create a pending/code fact before password proof");
        MvcResult loginPage = mvc.perform(request(get("/tenants/1001/login")).session(initial.session()))
                .andExpect(status().isOk()).andReturn();
        mvc.perform(request(post("/tenants/1001/login")).session(initial.session())
                        .contentType("application/x-www-form-urlencoded").param("username", "ignored-stepup-username")
                        .param("password", "fixture-password").param("_csrf", hidden(loginPage.getResponse().getContentAsString(), "_csrf")))
                .andExpect(status().isSeeOther());
        verify(fixture.identities).authenticateCurrent(eq("user-1001"), any(char[].class), eq(fixture.clock.instant()));
        MvcResult consentRedirect = mvc.perform(authorize(1001L).session(initial.session())
                        .queryParam("prompt", "login").queryParam("max_age", "0"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String location = consentRedirect.getResponse().getHeader("Location");
        assertEquals("/tenants/1001/consent", URI.create(location).getRawPath());
        Pending stepUp = new Pending(1001L, initial.session(), location, query(location, "state"));
        JsonNode tokens = token(approve(stepUp, consentCsrf(stepUp)), VERIFIER);
        Jwt access = decoded(tokens.path("access_token").asText());
        assertEquals("STRONG", access.getClaimAsString("acr"));
        long authenticatedAt = ((Number) access.getClaims().get("auth_time")).longValue();
        assertEquals(fixture.clock.instant().getEpochSecond(), authenticatedAt);
        assertTrue(authenticatedAt > originalAuthTime.getEpochSecond());
        int after = fixture.authorizations.size();
        MvcResult repeated = mvc.perform(authorize(1001L).session(initial.session())
                        .queryParam("prompt", "login").queryParam("max_age", "0"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertEquals("/tenants/1001/login", URI.create(repeated.getResponse().getHeader("Location")).getRawPath());
        assertEquals(after, fixture.authorizations.size(), "Consumed password proof cannot be replayed for another issuance");
    }

    @Test
    void realKeySourceUsesLegalSpiMethodValidationAndEncryptedPrivateKey() {
        AuthorizationServerContextHolder.setContext(new org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext() {
            @Override public String getIssuer() { return ORIGIN.resolve("/tenants/1001").toASCIIString(); }
            @Override public org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings getAuthorizationServerSettings() {
                return context.getBean("authorizationServerSettings", org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings.class);
            }
        });
        try {
            JWKSource<?> source = context.getBean("tenantJwkSourceComponent", JWKSource.class);
            var selector = new com.nimbusds.jose.jwk.JWKSelector(new com.nimbusds.jose.jwk.JWKMatcher.Builder()
                    .privateOnly(true).keyID("fixture-signing-kid").build());
            var keys = assertDoesNotThrow(() -> source.get(selector, null));
            assertEquals(1, keys.size());
            assertTrue(keys.getFirst().isPrivate());
            assertThrows(com.nimbusds.jose.KeySourceException.class, () -> source.get(null, null));
        } finally { AuthorizationServerContextHolder.resetContext(); }
    }

    @Test
    void emptyConsentSelectionDeniesAndPreservesUnrelatedPriorConsent() throws Exception {
        TrustedRealmBO realm = fixture.realm(ORIGIN.resolve("/tenants/1001"));
        OAuthConsentBO existing = OAuthConsentBO.builder().id(600L).version(0L).clientPk(501L)
                .subject("user-1001").authorities(List.of("SCOPE_orders.write"))
                .grantedAt(fixture.clock.instant()).status("ACTIVE").build();
        fixture.consents.put(Fixture.consentKey(realm, 501L, "user-1001"), existing);
        Pending pending = startThroughRealLogin(1001L);
        MvcResult denied = mvc.perform(consent(pending).param("_csrf", consentCsrf(pending)))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertEquals("access_denied", query(denied.getResponse().getHeader("Location"), "error"));
        assertNull(query(denied.getResponse().getHeader("Location"), "code"));
        assertEquals(existing, fixture.consents.get(Fixture.consentKey(realm, 501L, "user-1001")));
        assertTrue(fixture.authorizations.isEmpty());
        assertTrue(fixture.tokens.values().stream().noneMatch(row -> row.getTokenType() == OAuthTokenTypeEnum.CODE));
    }

    @Test
    void userInfoDependencyFailuresAreUnavailableAndNeverReturnIdentity() throws Exception {
        Pending pending = startThroughRealLogin(1001L);
        JsonNode tokens = token(approve(pending, consentCsrf(pending)), VERIFIER);
        String access = tokens.path("access_token").asText();
        fixture.failTokenLookup = true;
        mvc.perform(request(get("/tenants/1001/userinfo")).header("Authorization", "Bearer " + access))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error").value("temporarily_unavailable"))
                .andExpect(jsonPath("$.sub").doesNotExist());
        fixture.failTokenLookup = false;
        fixture.failSigning = true;
        mvc.perform(request(get("/tenants/1001/userinfo")).header("Authorization", "Bearer " + access))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error").value("temporarily_unavailable"))
                .andExpect(jsonPath("$.sub").doesNotExist());
        fixture.failSigning = false;
        mvc.perform(request(get("/tenants/1002/userinfo")).header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.sub").doesNotExist());
    }

    @Test
    void acceptedNativeQueryBoundsRejectBeforeAnyNewPendingFact() throws Exception {
        Pending pending = startThroughRealLogin(1001L);
        Map<String, OAuthAuthorizationPO> original = new LinkedHashMap<>(fixture.authorizations);
        for (String missing : List.of("state", "nonce", "resource")) {
            MvcResult rejected = mvc.perform(authorize(1001L).session(pending.session()).with(value -> {
                value.removeParameter(missing); return value;
            })).andReturn();
            assertTrue(rejected.getResponse().getStatus() >= 400 || query(rejected.getResponse().getHeader("Location"), "error") != null,
                    "Missing native transaction field must reject: " + missing);
            assertEquals(original, fixture.authorizations);
        }
        mvc.perform(authorize(1001L).session(pending.session()).queryParam("client_id", CLIENT))
                .andExpect(status().isBadRequest());
        mvc.perform(authorize(1001L).session(pending.session()).queryParam("unrecognized", "x"))
                .andExpect(status().isBadRequest());
        mvc.perform(authorize(1001L).session(pending.session()).queryParam("max_age", "86401"))
                .andExpect(status().isBadRequest());
        MvcResult wrongTarget = mvc.perform(authorize(1001L).session(pending.session()).with(value -> {
            value.setParameter("resource", "https://unregistered.example.test/resource"); return value;
        })).andReturn();
        assertTrue(wrongTarget.getResponse().getStatus() >= 400 || query(wrongTarget.getResponse().getHeader("Location"), "error") != null);
        assertEquals(original, fixture.authorizations);
    }

    @Test
    void maxAgeBoundaryAndPromptNoneRequireActualAuthenticationFreshness() throws Exception {
        Pending pending = startThroughRealLogin(1001L);
        fixture.clock.now = fixture.clock.now.plusSeconds(600);
        int initial = fixture.authorizations.size();
        MvcResult silent = mvc.perform(authorize(1001L).session(pending.session()).queryParam("max_age", "600")
                        .queryParam("prompt", "none")).andExpect(status().is3xxRedirection()).andReturn();
        assertEquals("login_required", query(silent.getResponse().getHeader("Location"), "error"));
        assertEquals(initial, fixture.authorizations.size());
        MvcResult login = mvc.perform(authorize(1001L).session(pending.session()).queryParam("max_age", "600"))
                .andExpect(status().is3xxRedirection()).andReturn();
        assertEquals("/tenants/1001/login", URI.create(login.getResponse().getHeader("Location")).getRawPath());
        assertEquals(initial, fixture.authorizations.size());
    }

    @Test
    void userBusinessScopeRequiresExplicitCurrentResourceGrant() throws Exception {
        Pending pending = startThroughRealLogin(1001L);
        String code = approve(pending, consentCsrf(pending));
        fixture.userGrantScopes = List.of();
        tokenRejected(code, VERIFIER);
        fixture.userGrantScopes = List.of("orders.read");
        fixture.grantResourceVersion = 1L;
        tokenRejected(code, VERIFIER);
        fixture.grantResourceVersion = 0L;
        assertTrue(token(code, VERIFIER).hasNonNull("access_token"));
    }

    @Test
    void managedUserGrantSurvivesNativeExchangeAndClearDisableDoNotConsumeCode() throws Exception {
        TrustedRealmBO realm = fixture.realm(URI.create("https://idp.example.test/tenants/1001"));
        TrustedRealmBO platform = TrustedRealmBO.builder().issuer(ORIGIN.resolve("/platform"))
                .realmKey("platform").realmType(RealmTypeEnum.PLATFORM).storagePartitionId(9000L)
                .trustVersion(0L).build();
        OAuthResourceRepository resources = mock(OAuthResourceRepository.class);
        when(resources.findByResourceServerIdForUpdate(platform, "orders")).thenReturn(Optional.of(
                OAuthResourceBO.builder().id(601L).version(0L).resourceServerId("orders")
                        .resourceUri(RESOURCE).status(ResourceServerStatus.ACTIVE).build()));
        when(fixture.grantRepository.save(eq(realm), any(OAuthGrantBO.class), anyLong())).thenAnswer(call -> {
            OAuthGrantBO saved = call.getArgument(1);
            fixture.userGrantScopes = saved.getAllowedScopes();
            fixture.userGrantVersion = saved.getVersion();
            fixture.userGrantStatus = saved.getStatus();
            return saved;
        });
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            ResourceServerServiceImpl management = new ResourceServerServiceImpl(resources, fixture.grantRepository,
                    fixture.clientRepository, Mappers.getMapper(ResourceServerViewConverter.class),
                    Mappers.getMapper(ClientResourceGrantViewConverter.class), fixture.clock,
                    mock(TransactionalOutboxResourceServerEventAdapter.class), validatorFactory.getValidator());
            fixture.userGrantScopes = List.of();
            management.putGrant(realm, platform, CLIENT, "orders", UpsertClientResourceGrantDTO.builder()
                    .grantType(ResourceGrantType.USER_DELEGATION).allowedScopes(Set.of("orders.read"))
                    .expectedResourceVersion(0L).expectedGrantVersion(0L).build());
            Pending first = startThroughRealLogin(1001L);
            assertTrue(token(approve(first, consentCsrf(first)), VERIFIER).hasNonNull("access_token"));
            fixture.authorizations.clear();
            fixture.consents.clear();
            Pending pending = startThroughRealLogin(1001L);
            String code = approve(pending, consentCsrf(pending));
            management.putGrant(realm, platform, CLIENT, "orders", UpsertClientResourceGrantDTO.builder()
                    .grantType(ResourceGrantType.USER_DELEGATION).allowedScopes(Set.of())
                    .expectedResourceVersion(0L).expectedGrantVersion(1L).build());
            tokenRejected(code, VERIFIER);
            management.putGrant(realm, platform, CLIENT, "orders", UpsertClientResourceGrantDTO.builder()
                    .grantType(ResourceGrantType.USER_DELEGATION).allowedScopes(Set.of("orders.read"))
                    .expectedResourceVersion(0L).expectedGrantVersion(2L).build());
            assertThrows(org.springframework.dao.OptimisticLockingFailureException.class,
                    () -> management.putGrant(realm, platform, CLIENT, "orders", UpsertClientResourceGrantDTO.builder()
                            .grantType(ResourceGrantType.USER_DELEGATION).allowedScopes(Set.of("orders.write"))
                            .expectedResourceVersion(99L).expectedGrantVersion(3L).build()));
            management.deleteGrant(realm, platform, CLIENT, "orders", DeleteClientResourceGrantDTO.builder()
                    .grantType(ResourceGrantType.USER_DELEGATION).expectedResourceVersion(0L)
                    .expectedGrantVersion(3L).build());
            tokenRejected(code, VERIFIER);
            management.putGrant(realm, platform, CLIENT, "orders", UpsertClientResourceGrantDTO.builder()
                    .grantType(ResourceGrantType.USER_DELEGATION).allowedScopes(Set.of("orders.read"))
                    .expectedResourceVersion(0L).expectedGrantVersion(4L).build());
            assertTrue(token(code, VERIFIER).hasNonNull("access_token"));
        }
    }

    @Test
    void reducedRefreshScopeCannotRegainOpenidThroughMetadataReload() throws Exception {
        Pending pending = startThroughRealLogin(1001L);
        JsonNode original = token(approve(pending, consentCsrf(pending)), VERIFIER);
        MvcResult refreshed = mvc.perform(request(post("/tenants/1001/oauth2/token")).header("Authorization", basic())
                        .contentType("application/x-www-form-urlencoded").param("grant_type", "refresh_token")
                        .param("refresh_token", original.path("refresh_token").asText()).param("scope", "orders.read"))
                .andExpect(status().isOk()).andReturn();
        JsonNode issued = fixture.mapper.readTree(refreshed.getResponse().getContentAsString());
        assertEquals("orders.read", decoded(issued.path("access_token").asText()).getClaimAsString("scope"));
        mvc.perform(request(get("/tenants/1001/userinfo"))
                        .header("Authorization", "Bearer " + issued.path("access_token").asText()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.sub").doesNotExist());
    }

    @Test
    void explicitScopeSelectionDoesNotRegrantUncheckedPreviouslyConsentedScope() throws Exception {
        Pending initial = startThroughRealLogin(1001L);
        approve(initial, consentCsrf(initial));
        MvcResult redirect = mvc.perform(authorize(1001L).session(initial.session()).with(value -> {
            value.setParameter("scope", "openid orders.read orders.write"); return value;
        })).andExpect(status().is3xxRedirection()).andReturn();
        String location = redirect.getResponse().getHeader("Location");
        assertEquals("/tenants/1001/consent", URI.create(location).getRawPath());
        Pending pending = new Pending(1001L, initial.session(), location, query(location, "state"));
        MvcResult allowed = mvc.perform(consent(pending).param("_csrf", consentCsrf(pending)).param("scope", "orders.write"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String code = query(allowed.getResponse().getHeader("Location"), "code");
        assertNotNull(code);
        JsonNode issued = token(code, VERIFIER);
        assertEquals(Set.of("openid", "orders.write"), Set.copyOf(List.of(issued.path("scope").asText().split(" "))));
        assertEquals(Set.of("openid", "orders.write"), Set.copyOf(List.of(
                decoded(issued.path("access_token").asText()).getClaimAsString("scope").split(" "))));
    }

    private String consentCsrf(Pending pending) throws Exception {
        MvcResult result = mvc.perform(request(get(URI.create(pending.consentLocation()))).session(pending.session()))
                .andExpect(status().isOk()).andReturn();
        return hidden(result.getResponse().getContentAsString(), "_csrf");
    }

    private String approve(Pending pending, String csrf) throws Exception {
        MvcResult approved = mvc.perform(consent(pending).param("_csrf", csrf).param("scope", "orders.read"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String code = query(approved.getResponse().getHeader("Location"), "code");
        assertNotNull(code);
        return code;
    }

    private Pending startThroughRealLogin(long tenant) throws Exception {
        MvcResult loginRedirect = mvc.perform(authorize(tenant)).andExpect(status().is3xxRedirection()).andReturn();
        MockHttpSession session = (MockHttpSession) loginRedirect.getRequest().getSession(false);
        assertNotNull(session);
        assertEquals("/tenants/" + tenant + "/login", loginRedirect.getResponse().getHeader("Location"));
        RequestCache cache = context.getBean("oauthAuthorizationRequestCache", RequestCache.class);
        var saved = cache.getRequest(loginRedirect.getRequest(), loginRedirect.getResponse());
        assertNotNull(saved, "Native login entry must retain the authorize request");
        assertTrue(context.containsBean("tenantLoginController"));
        MvcResult loginPage = mvc.perform(request(get("/tenants/" + tenant + "/login")).session(session)).andReturn();
        assertEquals(200, loginPage.getResponse().getStatus(), () -> "Login handler=" + loginPage.getHandler()
                + "; saved method=" + (saved instanceof org.springframework.security.web.savedrequest.DefaultSavedRequest value
                        ? value.getMethod() : "unknown"));
        MvcResult resumed = mvc.perform(request(post("/tenants/" + tenant + "/login")).session(session)
                        .contentType("application/x-www-form-urlencoded")
                        .param("username", "fixture-user").param("password", "fixture-password")
                        .param("_csrf", hidden(loginPage.getResponse().getContentAsString(), "_csrf")))
                .andExpect(status().isSeeOther()).andReturn();
        assertNotNull(resumed.getResponse().getHeader("Location"));
        MvcResult consentRedirect = mvc.perform(authorize(tenant).session(session))
                .andExpect(status().is3xxRedirection()).andReturn();
        String location = consentRedirect.getResponse().getHeader("Location");
        TrustedRealmBO realm = fixture.realm(ORIGIN.resolve("/tenants/" + tenant));
        OAuthAuthorizationBO stored = fixture.readAuthorization(realm,
                fixture.authorizations.values().iterator().next().getId()).orElseThrow();
        assertNotNull(stored.getAttributes().getPendingConsent(), "Encrypted pending payload must survive native save/load");
        assertEquals(query(location, "state"), stored.getAttributes().getPendingConsent().getConsentState());
        assertEquals(Digests.sha256Hex(session.getId()), stored.getAttributes().getPendingConsent().getSessionBindingDigest(),
                "Pending state must bind the session used for the consent redirect");
        assertEquals("/tenants/" + tenant + "/consent", URI.create(location).getRawPath());
        assertEquals(ORIGIN.getHost(), URI.create(location).isAbsolute() ? URI.create(location).getHost() : ORIGIN.getHost());
        return new Pending(tenant, session, location, query(location, "state"));
    }

    private MockHttpServletRequestBuilder authorize(long tenant) throws Exception {
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(VERIFIER.getBytes(StandardCharsets.US_ASCII)));
        return request(get("/tenants/" + tenant + "/oauth2/authorize"))
                .queryParam("response_type", "code").queryParam("client_id", CLIENT)
                .queryParam("redirect_uri", CALLBACK).queryParam("scope", "openid orders.read")
                .queryParam("state", CLIENT_STATE).queryParam("nonce", NONCE)
                .queryParam("resource", RESOURCE.toASCIIString())
                .queryParam("code_challenge", challenge).queryParam("code_challenge_method", "S256");
    }

    private MockHttpServletRequestBuilder consent(Pending pending) {
        return request(post("/tenants/" + pending.tenant() + "/oauth2/authorize"))
                .session(pending.session()).contentType("application/x-www-form-urlencoded")
                .param("client_id", CLIENT).param("state", pending.state());
    }

    private JsonNode token(String code, String verifier) throws Exception {
        MvcResult result = mvc.perform(tokenRequest(code, verifier))
                .andExpect(status().isOk()).andReturn();
        return fixture.mapper.readTree(result.getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder tokenRequest(String code, String verifier) {
        return request(post("/tenants/1001/oauth2/token")).header("Authorization", basic())
                .contentType("application/x-www-form-urlencoded").param("grant_type", "authorization_code")
                .param("code", code).param("redirect_uri", CALLBACK).param("code_verifier", verifier);
    }

    private void tokenRejected(String code, String verifier) throws Exception {
        mvc.perform(request(post("/tenants/1001/oauth2/token"))
                        .header("Authorization", basic()).contentType("application/x-www-form-urlencoded")
                        .param("grant_type", "authorization_code").param("code", code)
                        .param("redirect_uri", CALLBACK).param("code_verifier", verifier))
                .andExpect(status().is4xxClientError());
    }

    private Jwt decoded(String value) throws Exception {
        com.nimbusds.jwt.SignedJWT jwt = com.nimbusds.jwt.SignedJWT.parse(value);
        assertTrue(jwt.verify(new com.nimbusds.jose.crypto.RSASSAVerifier(fixture.key.toRSAPublicKey())));
        Map<String, Object> claims = new HashMap<>(jwt.getJWTClaimsSet().getClaims());
        claims.put("iat", jwt.getJWTClaimsSet().getIssueTime().toInstant());
        claims.put("exp", jwt.getJWTClaimsSet().getExpirationTime().toInstant());
        return new Jwt(value, jwt.getJWTClaimsSet().getIssueTime().toInstant(),
                jwt.getJWTClaimsSet().getExpirationTime().toInstant(), jwt.getHeader().toJSONObject(), claims);
    }

    private static MockHttpServletRequestBuilder request(MockHttpServletRequestBuilder builder) {
        return builder.secure(true).with(request -> {
            request.setScheme("https"); request.setServerName(ORIGIN.getHost()); request.setServerPort(443);
            return request;
        });
    }

    private static String basic() {
        return "Basic " + Base64.getEncoder().encodeToString((CLIENT + ":fixture-secret").getBytes(StandardCharsets.UTF_8));
    }

    private static String hidden(String html, String name) {
        var match = Pattern.compile("name=\"" + name + "\" value=\"([^\"]+)\"").matcher(html);
        assertTrue(match.find(), "Missing hidden form field " + name);
        return match.group(1).replace("&amp;", "&");
    }

    private static String query(String uri, String key) {
        if (uri == null) { return null; }
        for (String part : Optional.ofNullable(URI.create(uri).getRawQuery()).orElse("").split("&")) {
            String[] entry = part.split("=", 2);
            if (URLDecoder.decode(entry[0], StandardCharsets.UTF_8).equals(key)) {
                return entry.length == 2 ? URLDecoder.decode(entry[1], StandardCharsets.UTF_8) : "";
            }
        }
        return null;
    }

    private record Pending(long tenant, MockHttpSession session, String consentLocation, String state) { }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @EnableTransactionManagement(proxyTargetClass = true)
    @Import({AuthorizationServerConfiguration.class, ValidationAutoConfiguration.class,
            TenantRegisteredClientRepository.class, TenantAuthorizationServiceImpl.class,
            TenantConsentServiceImpl.class, OAuthTokenIssueServiceImpl.class, TenantJwkSourceComponent.class,
            OAuthKeyCipherComponent.class})
    @ComponentScan(basePackages = "top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller",
            useDefaultFilters = false, includeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
                    pattern = "top[.]egon[.]cola[.]platform[.]tianquan[.]shoubing[.]admin[.]oauth[.]controller[.](TenantLoginController|OAuthConsentController)"))
    @ComponentScan(basePackageClasses = SasConsentConverter.class, useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {SasConsentConverter.class, SasAuthorizationConverter.class, SasRegisteredClientConverter.class}))
    static class NativeConfiguration {
        @Bean("fixture") Fixture fixture() throws Exception { return new Fixture(); }
        @Bean("jacksonObjectMapper") ObjectMapper mapper(Fixture f) { return f.mapper; }
        @Bean("tenantIssuerService") TenantIssuerService issuers(Fixture f) { return f.issuers; }
        @Bean("oAuthRealmRepository") OAuthRealmRepository realms(Fixture f) { return f.realmRepository; }
        @Bean("oAuthClientRepository") OAuthClientRepository clients(Fixture f) { return f.clientRepository; }
        @Bean("oAuthAuthorizationRepository") OAuthAuthorizationRepository authorizations(Fixture f) { return f.authorizationRepository; }
        @Bean("oAuthTokenRepository") OAuthTokenRepository tokens(Fixture f) { return f.tokenRepository; }
        @Bean("oAuthSigningKeyRepository") OAuthSigningKeyRepository signingKeys(Fixture f) { return f.signingKeyRepository; }
        @Bean("oauthPayloadCipherComponent") OAuthPayloadCipherComponent payloadCipher(Fixture f) { return f.cipher; }
        @Bean("oAuthConsentRepository") OAuthConsentRepository consents(Fixture f) { return f.consentRepository; }
        @Bean("oAuthGrantRepository") OAuthGrantRepository grants(Fixture f) { return f.grantRepository; }
        @Bean("oAuthAuditRepository") OAuthAuditRepository audits(Fixture f) { return f.auditRepository; }
        @Bean("transactionalOutbox") TransactionalOutbox outbox(Fixture f) { return f.outbox; }
        @Bean("oauthSecurityProjectionComponent") OAuthSecurityProjectionComponent projection(Fixture f) { return f.projection; }
        @Bean("clientCredentialsAccessPolicy") ClientCredentialsAccessPolicy policy() { return mock(ClientCredentialsAccessPolicy.class); }
        @Bean("resourceServerStore") ResourceServerStore resources(Fixture f) { return f.resources; }
        @Bean("tenantMembershipPort") TenantMembershipPort memberships(Fixture f) { return f.memberships; }
        @Bean("identityFacade") IdentityFacade identities(Fixture f) { return f.identities; }
        @Bean("tokenRevocationService") TokenRevocationService revocations() { return mock(TokenRevocationService.class); }
        @Bean({"idpClock", "egonColaMybatisPlusClock"}) Clock clock(Fixture f) { return f.clock; }
        @Bean("transactionManager") PlatformTransactionManager transactions(Fixture f) { return new FixtureTransactionManager(f); }
        @Bean("tenantLoginAuthenticationFilter") TenantLoginAuthenticationFilter login(
                Fixture f, @Qualifier("oauthAuthorizationRequestCache") RequestCache cache,
                @Qualifier("oauthLoginSessionAuthenticationStrategy") SessionAuthenticationStrategy strategy) {
            return new TenantLoginAuthenticationFilter(f.identities, f.memberships, f.issuers, cache, f.clock, strategy);
        }
        @Bean("fixtureFallbackSecurity") @Order(10)
        SecurityFilterChain fallback(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(a -> a.anyRequest().permitAll()).csrf(c -> c.disable()).build();
        }
        @Bean static org.springframework.context.support.PropertySourcesPlaceholderConfigurer properties() {
            var config = new org.springframework.context.support.PropertySourcesPlaceholderConfigurer();
            var values = new java.util.Properties();
            values.setProperty("egon.tianquan-shoubing.oauth.public-origin", ORIGIN.toASCIIString());
            values.setProperty("egon.tianquan-shoubing.oauth.platform-management-tenant-id", "9000");
            config.setProperties(values); return config;
        }
    }

    static final class Fixture implements AutoCloseable {
        final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        final MutableClock clock = new MutableClock();
        final TenantIssuerService issuers = mock(TenantIssuerService.class);
        final OAuthRealmRepository realmRepository = mock(OAuthRealmRepository.class);
        final OAuthClientRepository clientRepository = mock(OAuthClientRepository.class);
        final OAuthAuthorizationRepository authorizationRepository = mock(OAuthAuthorizationRepository.class);
        final OAuthTokenRepository tokenRepository = mock(OAuthTokenRepository.class);
        final OAuthSigningKeyRepository signingKeyRepository = mock(OAuthSigningKeyRepository.class);
        final OAuthConsentRepository consentRepository = mock(OAuthConsentRepository.class);
        final OAuthGrantRepository grantRepository = mock(OAuthGrantRepository.class);
        final OAuthAuditRepository auditRepository = mock(OAuthAuditRepository.class);
        final TransactionalOutbox outbox = mock(TransactionalOutbox.class);
        final OAuthSecurityProjectionComponent projection = mock(OAuthSecurityProjectionComponent.class);
        final ResourceServerStore resources = mock(ResourceServerStore.class);
        final TenantMembershipPort memberships = mock(TenantMembershipPort.class);
        final IdentityFacade identities = mock(IdentityFacade.class);
        final Map<String, OAuthAuthorizationPO> authorizations = new LinkedHashMap<>();
        final Map<String, OAuthTokenPO> tokens = new LinkedHashMap<>();
        final Map<String, OAuthConsentBO> consents = new LinkedHashMap<>();
        final List<OutboxMessage> events = new ArrayList<>();
        final List<OAuthAuditBO> audits = new ArrayList<>();
        final OAuthAuthorizationConverter authorizationConverter = Mappers.getMapper(OAuthAuthorizationConverter.class);
        final OAuthTokenConverter tokenConverter = Mappers.getMapper(OAuthTokenConverter.class);
        final AtomicLong ids = new AtomicLong(2000L);
        final Path directory;
        final Path keyFile;
        final OAuthPayloadCipherComponent cipher;
        final RSAKey key;
        boolean failCodeSave;
        boolean failEnqueue;
        boolean failConsentSave;
        boolean failSigning;
        boolean publicClient;
        boolean failTokenLookup;
        List<String> userGrantScopes = List.of("orders.read", "orders.write");
        long grantResourceVersion;
        long userGrantVersion;
        ClientResourceGrant.Status userGrantStatus = ClientResourceGrant.Status.ACTIVE;

        Fixture() throws Exception {
            directory = Files.createTempDirectory("egon-native-consent-");
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
            keyFile = directory.resolve("fixture.key");
            Files.write(keyFile, new byte[32]);
            Files.setPosixFilePermissions(keyFile, PosixFilePermissions.fromString("rw-------"));
            var properties = new OAuthKeyEncryptionProperties().setKeyId("fixture-key").setKeyFile(keyFile);
            cipher = new OAuthPayloadCipherComponent(properties);
            cipher.initializeKey();
            var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(3072);
            var pair = generator.generateKeyPair();
            key = new RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyID("fixture-signing-kid").algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                    .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE).build();
            configurePorts();
        }

        void reset() {
            authorizations.clear(); tokens.clear(); consents.clear(); events.clear(); audits.clear();
            failCodeSave = false; failEnqueue = false; failConsentSave = false; failSigning = false; publicClient = false;
            failTokenLookup = false;
            userGrantScopes = List.of("orders.read", "orders.write"); grantResourceVersion = 0L;
            userGrantVersion = 0L; userGrantStatus = ClientResourceGrant.Status.ACTIVE;
            clock.now = Instant.now();
        }

        TrustedRealmBO realm(URI issuer) {
            long tenant = switch (issuer.getPath()) {
                case "/tenants/1001" -> 1001L;
                case "/tenants/1002" -> 1002L;
                default -> throw new OAuth2AuthenticationException(new OAuth2Error("invalid_request"));
            };
            return TrustedRealmBO.builder().issuer(ORIGIN.resolve(issuer.getPath()))
                    .realmKey("tenant:" + tenant).realmType(RealmTypeEnum.TENANT)
                    .businessTenantId(tenant).storagePartitionId(tenant).trustVersion(0L).build();
        }

        OAuthClientBO client(TrustedRealmBO realm) {
            return OAuthClientBO.builder().id(501L).version(0L).clientId(CLIENT).clientName("Consent & <fixture>")
                    .clientType(publicClient ? OAuthClient.ClientType.PUBLIC : OAuthClient.ClientType.CONFIDENTIAL).purpose(OAuthClientPurposeEnum.USER)
                    .status(OAuthClient.Status.ACTIVE).secretHash(SECRET_HASH).credentialVersion(0L)
                    .redirectUris(List.of(CALLBACK)).resourceUris(List.of(RESOURCE.toASCIIString()))
                    .allowedScopes(List.of("openid", "orders.read", "orders.write"))
                    .accessTtlSeconds(300).refreshTtlSeconds(3600).build();
        }
        private static final String SECRET_HASH = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12)
                .encode("fixture-secret");

        void configurePorts() {
            when(issuers.resolveExactIssuer(any(URI.class))).thenAnswer(i -> realm(i.getArgument(0)));
            when(issuers.resolveCurrentIssuer()).thenAnswer(i -> realm(URI.create(
                    AuthorizationServerContextHolder.getContext().getIssuer())));
            when(realmRepository.findByRealmKeyForUpdate(any())).thenAnswer(i -> Optional.of(realmFact(i.getArgument(0))));
            when(realmRepository.findByIssuer(any())).thenAnswer(i -> Optional.of(realmFact(i.getArgument(0))));
            when(signingKeyRepository.findByStatuses(any(), anySet(), anyLong(), anyInt())).thenAnswer(i -> {
                if (failSigning) { throw new TransientDataAccessResourceException("fixture signing key lookup failure"); }
                TrustedRealmBO realm = i.getArgument(0);
                String privatePem = "-----BEGIN PRIVATE KEY-----\n"
                        + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(key.toRSAPrivateKey().getEncoded())
                        + "\n-----END PRIVATE KEY-----";
                OAuthSigningKeyBO value = OAuthSigningKeyBO.builder().id(801L).version(0L).kid(key.getKeyID())
                        .algorithm("RS256").publicJwk(mapper.valueToTree(key.toPublicJWK().toJSONObject()))
                        .privateKeyCiphertext(new OAuthKeyCipherComponent(cipher).encryptPrivateKey(realm, key.getKeyID(), privatePem))
                        .status(top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthKeyStatusEnum.ACTIVE)
                        .publishedAt(clock.instant()).activatedAt(clock.instant()).build();
                validate(value); return List.of(value);
            });
            when(clientRepository.findByClientId(any(), anyString())).thenAnswer(i -> CLIENT.equals(i.getArgument(1))
                    ? Optional.of(client(i.getArgument(0))) : Optional.empty());
            when(clientRepository.findByClientIdForUpdate(any(), anyString())).thenAnswer(i -> CLIENT.equals(i.getArgument(1))
                    ? Optional.of(client(i.getArgument(0))) : Optional.empty());
            when(clientRepository.findById(any(), anyLong())).thenAnswer(i -> ((Long) i.getArgument(1)) == 501L
                    ? Optional.of(client(i.getArgument(0))) : Optional.empty());
            when(projection.snapshotForAuthorization(any(), anyLong(), anyString(), anyString())).thenReturn(snapshot());
            when(memberships.resolve(anyString(), anyString())).thenAnswer(i -> new TenantMembershipPort.TenantMembership(
                    i.getArgument(0), i.getArgument(1), "Fixture tenant", TenantMembershipPort.MembershipStatus.ACTIVE));
            when(identities.authenticate(anyString(), any(char[].class), anyString(), any(Instant.class))).thenAnswer(i ->
                    new AuthenticatedIdentity("user-" + issuers.resolveCurrentIssuer().getBusinessTenantId(), "fixture-user", "Fixture User", false));
            when(identities.authenticateCurrent(anyString(), any(char[].class), any(Instant.class))).thenAnswer(i ->
                    new AuthenticatedIdentity(i.getArgument(0), "fixture-user", "Fixture User", false));
            ResourceServer resource = new ResourceServer("orders", RESOURCE, "orders", "orders", "test", "manager-client",
                    "orders", "orders:enter", Duration.ofSeconds(300), ResourceServerStatus.ACTIVE, 0L);
            when(resources.findByUri(RESOURCE)).thenReturn(Optional.of(resource));
            when(grantRepository.findGrantForUpdate(any(), anyLong(), eq("orders"), eq(ResourceGrantType.USER_DELEGATION)))
                    .thenAnswer(i -> Optional.of(OAuthGrantBO.builder().id(701L).version(userGrantVersion).clientPk(501L)
                            .resourceServerId("orders").grantType(ResourceGrantType.USER_DELEGATION)
                            .allowedScopes(userGrantScopes).status(userGrantStatus)
                            .resourceVersion(grantResourceVersion).build()));
            when(authorizationRepository.findById(any(), anyLong())).thenAnswer(i -> readAuthorization(i.getArgument(0), i.getArgument(1)));
            when(authorizationRepository.findByIdForUpdate(any(), anyLong())).thenAnswer(i -> readAuthorization(i.getArgument(0), i.getArgument(1)));
            when(authorizationRepository.save(any(), any(OAuthAuthorizationBO.class), anyLong())).thenAnswer(i -> {
                TrustedRealmBO realm = i.getArgument(0); OAuthAuthorizationBO value = i.getArgument(1);
                validate(value);
                OAuthAuthorizationPO row = authorizationConverter.toSource(realm, cipher, mapper, value.getId().toString(), value);
                authorizations.put(key(realm, value.getId()), row); return readAuthorization(realm, value.getId()).orElseThrow();
            });
            doAnswer(i -> { authorizations.remove(key(i.getArgument(0), i.getArgument(1))); return true; })
                    .when(authorizationRepository).deleteVersionedById(any(), anyLong(), anyLong());
            when(tokenRepository.findByTokenDigest(any(), anyString())).thenAnswer(i -> {
                if (failTokenLookup) { throw new TransientDataAccessResourceException("fixture token lookup failure"); }
                return readToken(i.getArgument(0), i.getArgument(1));
            });
            when(tokenRepository.findByTokenDigestForUpdate(any(), anyString())).thenAnswer(i -> readToken(i.getArgument(0), i.getArgument(1)));
            when(tokenRepository.findByAuthorization(any(), anyLong(), any(), anyLong(), anyInt())).thenAnswer(i -> {
                TrustedRealmBO realm = i.getArgument(0); long authorizationId = i.getArgument(1); OAuthTokenTypeEnum type = i.getArgument(2);
                long after = i.getArgument(3); int limit = i.getArgument(4);
                return tokens.entrySet().stream().filter(e -> e.getKey().startsWith(realm.getRealmKey() + ":"))
                        .map(Map.Entry::getValue).filter(v -> v.getAuthorizationPk() == authorizationId && v.getTokenType() == type && v.getId() > after)
                        .sorted(Comparator.comparing(OAuthTokenPO::getId)).limit(limit)
                        .map(row -> tokenConverter.toTarget(realm, cipher, mapper, row.getTokenDigest(), row)).toList();
            });
            when(tokenRepository.save(any(), any(OAuthTokenBO.class), anyLong())).thenAnswer(i -> {
                TrustedRealmBO realm = i.getArgument(0); OAuthTokenBO value = i.getArgument(1);
                if (failCodeSave && value.getTokenType() == OAuthTokenTypeEnum.CODE) {
                    throw new TransientDataAccessResourceException("fixture code save failure");
                }
                if (value.getId() == null) { value.setId(ids.incrementAndGet()); }
                validate(value);
                OAuthTokenPO row = tokenConverter.toSource(realm, cipher, mapper, value.getTokenDigest(), value);
                tokens.put(key(realm, value.getTokenDigest()), row); return readToken(realm, value.getTokenDigest()).orElseThrow();
            });
            when(consentRepository.findConsent(any(), anyLong(), anyString())).thenAnswer(i ->
                    Optional.ofNullable(consents.get(consentKey(i.getArgument(0), i.getArgument(1), i.getArgument(2)))));
            when(consentRepository.findConsentForUpdate(any(), anyLong(), anyString())).thenAnswer(i ->
                    Optional.ofNullable(consents.get(consentKey(i.getArgument(0), i.getArgument(1), i.getArgument(2)))));
            when(consentRepository.save(any(), any(OAuthConsentBO.class), anyLong())).thenAnswer(i -> {
                if (failConsentSave) { throw new TransientDataAccessResourceException("fixture consent save failure"); }
                TrustedRealmBO realm = i.getArgument(0); OAuthConsentBO value = i.getArgument(1);
                if (value.getId() == null) { value.setId(ids.incrementAndGet()); }
                validate(value); consents.put(consentKey(realm, value.getClientPk(), value.getSubject()), value); return value;
            });
            when(auditRepository.save(any(), any(OAuthAuditBO.class), anyLong())).thenAnswer(i -> {
                OAuthAuditBO value = i.getArgument(1); value.setId(ids.incrementAndGet()); audits.add(value); return value;
            });
            doAnswer(i -> { if (failEnqueue) { throw new TransientDataAccessResourceException("fixture enqueue failure"); }
                events.add(i.getArgument(0)); return null; }).when(outbox).enqueue(any());
        }

        private Optional<OAuthAuthorizationBO> readAuthorization(TrustedRealmBO realm, long id) {
            OAuthAuthorizationPO row = authorizations.get(key(realm, id));
            return row == null ? Optional.empty() : Optional.of(authorizationConverter.toTarget(realm, cipher, mapper, Long.toString(id), row));
        }
        private Optional<OAuthTokenBO> readToken(TrustedRealmBO realm, String digest) {
            OAuthTokenPO row = tokens.get(key(realm, digest));
            return row == null ? Optional.empty() : Optional.of(tokenConverter.toTarget(realm, cipher, mapper, digest, row));
        }
        private static OAuthRealmBO realmFact(TrustedRealmBO realm) {
            return OAuthRealmBO.builder().id(realm.getStoragePartitionId()).version(0L).issuer(realm.getIssuer())
                    .realmKey(realm.getRealmKey()).realmType(realm.getRealmType()).businessTenantId(realm.getBusinessTenantId())
                    .storagePartitionId(realm.getStoragePartitionId()).status("ACTIVE").activeKid("fixture-signing-kid").trustVersion(0L).build();
        }
        private static OAuthSecuritySnapshotBO snapshot() {
            return OAuthSecuritySnapshotBO.builder().realmGeneration(0L).clientGeneration(0L).subjectGeneration(0L)
                    .readyEpoch("fixture-epoch").revision(0L).build();
        }
        private static String key(TrustedRealmBO realm, Object value) { return realm.getRealmKey() + ":" + value; }
        private static String consentKey(TrustedRealmBO realm, Object client, String subject) { return key(realm, client + ":" + subject); }
        private static void validate(Object value) {
            try (var factory = Validation.buildDefaultValidatorFactory()) {
                var errors = factory.getValidator().validate(value);
                assertTrue(errors.isEmpty(), () -> "Stored model constraint paths: " + errors.stream().map(v -> v.getPropertyPath().toString()).toList());
            }
        }
        @Override public void close() throws Exception { Files.deleteIfExists(keyFile); Files.deleteIfExists(directory); }
    }

    static final class MutableClock extends Clock {
        Instant now = Instant.now();
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    static final class FixtureTransactionManager extends AbstractPlatformTransactionManager {
        private final Fixture fixture;
        private final ThreadLocal<FixtureTransaction> current = new ThreadLocal<>();
        FixtureTransactionManager(Fixture fixture) { this.fixture = fixture; }
        @Override protected Object doGetTransaction() {
            FixtureTransaction existing = current.get(); return existing == null ? new FixtureTransaction() : existing;
        }
        @Override protected boolean isExistingTransaction(Object transaction) { return ((FixtureTransaction) transaction).active; }
        @Override protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) {
            FixtureTransaction state = (FixtureTransaction) transaction; state.active = true;
            state.authorizations = new LinkedHashMap<>(fixture.authorizations); state.tokens = new LinkedHashMap<>(fixture.tokens);
            state.consents = new LinkedHashMap<>(fixture.consents); state.auditCount = fixture.audits.size(); state.eventCount = fixture.events.size();
            current.set(state);
        }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) {
            FixtureTransaction state = (FixtureTransaction) status.getTransaction();
            fixture.authorizations.clear(); fixture.authorizations.putAll(state.authorizations);
            fixture.tokens.clear(); fixture.tokens.putAll(state.tokens); fixture.consents.clear(); fixture.consents.putAll(state.consents);
            fixture.audits.subList(state.auditCount, fixture.audits.size()).clear(); fixture.events.subList(state.eventCount, fixture.events.size()).clear();
        }
        @Override protected void doSetRollbackOnly(DefaultTransactionStatus status) { ((FixtureTransaction) status.getTransaction()).rollbackOnly = true; }
        @Override protected void doCleanupAfterCompletion(Object transaction) { current.remove(); }
    }

    static final class FixtureTransaction implements SmartTransactionObject {
        boolean active; boolean rollbackOnly; int auditCount; int eventCount;
        Map<String, OAuthAuthorizationPO> authorizations; Map<String, OAuthTokenPO> tokens; Map<String, OAuthConsentBO> consents;
        @Override public boolean isRollbackOnly() { return rollbackOnly; }
        @Override public void flush() { }
    }
}
