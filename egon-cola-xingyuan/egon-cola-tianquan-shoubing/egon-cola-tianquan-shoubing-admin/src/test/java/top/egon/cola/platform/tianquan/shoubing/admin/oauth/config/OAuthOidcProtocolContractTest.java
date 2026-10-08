package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RStream;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.support.outbox.OAuthAuditOutboxDeliveryHandler;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationAttributesBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthConsentBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthSecuritySnapshotBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.TenantLoginController;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.controller.filter.TenantLoginAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.SasAuthorizationConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthAuthorizationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthConsentRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthTokenIssueServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthSecurityProjectionComponent;
import top.egon.cola.platform.tianquan.shoubing.core.port.ResourceServerStore;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientCredentialsAccessPolicy;
import top.egon.cola.platform.tianquan.shoubing.core.port.TenantMembershipPort;
import top.egon.cola.platform.tianquan.shoubing.core.identity.AuthenticatedIdentity;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityFacade;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServer;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSecurityStateBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSecurityStateRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mapstruct.factory.Mappers.getMapper;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthOidcProtocolContractTest {

    private static final Instant NOW = Instant.parse("2026-10-08T08:00:00Z");

    @Test
    void requiresS256AndRejectsMissingPlainOrMalformedChallenges() {
        AuthorizationServerConfiguration.requireS256Challenge("S256", "A".repeat(43));

        assertThrows(OAuth2AuthenticationException.class,
                () -> AuthorizationServerConfiguration.requireS256Challenge("plain", "A".repeat(43)));
        assertThrows(OAuth2AuthenticationException.class,
                () -> AuthorizationServerConfiguration.requireS256Challenge(null, null));
        assertThrows(OAuth2AuthenticationException.class,
                () -> AuthorizationServerConfiguration.requireS256Challenge("S256", "not-a-challenge"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void readsOneSameSlotSecuritySnapshotAndFailsWhenTheReadyFenceIsMissing() {
        RedissonClient redisson = mock(RedissonClient.class);
        RScript script = mock(RScript.class);
        doReturn(script).when(redisson).getScript(StringCodec.INSTANCE);
        OAuthSecurityStateRepository states = mock(OAuthSecurityStateRepository.class);
        TrustedRealmBO trustedRealm = trustedTenant();
        when(states.findByTargetForUpdate(eq(trustedRealm), eq(OAuthSecurityTargetEnum.REALM), eq("self")))
                .thenReturn(Optional.of(securityState(OAuthSecurityTargetEnum.REALM, "self", 1L, 4L)));
        when(states.findByTargetForUpdate(eq(trustedRealm), eq(OAuthSecurityTargetEnum.CLIENT), eq("501")))
                .thenReturn(Optional.of(securityState(OAuthSecurityTargetEnum.CLIENT, "501", 2L, 4L)));
        when(states.findByTargetForUpdate(eq(trustedRealm), eq(OAuthSecurityTargetEnum.SUBJECT), eq("subject-1")))
                .thenReturn(Optional.of(securityState(OAuthSecurityTargetEnum.SUBJECT, "subject-1", 3L, 4L)));
        List<Object> observedKeys = new ArrayList<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            observedKeys.addAll(invocation.getArgument(3));
            return List.of("epoch-1", "4", "READY", "1", "true", "4",
                    "2", "true", "4", "3", "true", "4");
        }).when(script).<List<Object>>eval(
                eq(RScript.Mode.READ_ONLY), org.mockito.ArgumentMatchers.anyString(),
                eq(RScript.ReturnType.MULTI), org.mockito.ArgumentMatchers.anyList());
        OAuthSecurityProjectionComponent projection = new OAuthSecurityProjectionComponent(redisson, states, "epoch-1");

        var snapshot = projection.snapshotForAuthorization(trustedRealm, 501L, "USER", "subject-1");

        assertEquals(1L, snapshot.getRealmGeneration());
        assertEquals(2L, snapshot.getClientGeneration());
        assertEquals(3L, snapshot.getSubjectGeneration());
        assertEquals("epoch-1", snapshot.getReadyEpoch());
        assertEquals(4L, snapshot.getRevision());
        assertEquals(4, observedKeys.size());
        String issuerHash = Digests.sha256Hex(trustedRealm.getIssuer().toASCIIString());
        assertTrue(observedKeys.stream().allMatch(key -> key.toString().contains("{" + issuerHash + "}")));
        assertFalse(observedKeys.toString().contains("subject-1"));

        org.mockito.Mockito.doAnswer(invocation -> List.of("", "4", "NOT_READY", "1", "true", "4",
                "2", "true", "4", "3", "true", "4"))
                .when(script).<List<Object>>eval(
                        eq(RScript.Mode.READ_ONLY), org.mockito.ArgumentMatchers.anyString(),
                        eq(RScript.ReturnType.MULTI), org.mockito.ArgumentMatchers.anyList());
        OAuth2AuthenticationException unavailable = assertThrows(OAuth2AuthenticationException.class,
                () -> projection.snapshotForAuthorization(trustedRealm, 501L, "USER", "subject-1"));
        assertEquals("temporarily_unavailable", unavailable.getError().getErrorCode());
    }

    @Test
    void mapsSavedAuthorizationRequestToTypedPkceAndOidcAttributes() {
        String state = "state-that-must-not-be-persisted";
        String nonce = "nonce-that-must-not-be-logged-or-serialized";
        RegisteredClient registeredClient = RegisteredClient.withId("501")
                .clientId("public-client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://client.example.test/callback")
                .scope("openid")
                .build();
        OAuth2AuthorizationRequest request = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://idp.example.test/tenants/1001/oauth2/authorize")
                .clientId("public-client")
                .redirectUri("https://client.example.test/callback")
                .scopes(Set.of("openid"))
                .state(state)
                .additionalParameters(Map.of(
                        "code_challenge", "A".repeat(43),
                        "code_challenge_method", "S256",
                        "nonce", nonce))
                .build();
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(null)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .principalName("subject-1")
                .authorizedScopes(Set.of("openid"))
                .attribute(java.security.Principal.class.getName(),
                        UsernamePasswordAuthenticationToken.authenticated("subject-1", "N/A", java.util.List.of()))
                .attribute(OAuth2AuthorizationRequest.class.getName(), request)
                .token(new OAuth2AuthorizationCode("one-time-code", NOW, NOW.plusSeconds(120)))
                .build();

        var result = getMapper(SasAuthorizationConverter.class)
                .toSource(authorization, null, new ObjectMapper().findAndRegisterModules());

        assertEquals("A".repeat(43), result.getAttributes().getCodeChallenge());
        assertNull(result.getId());
        assertEquals("S256", result.getAttributes().getCodeChallengeMethod());
        assertEquals("https://client.example.test/callback", result.getAttributes().getRedirectUri().toString());
        assertEquals(nonce, result.getAttributes().getNonce());
        assertEquals(Digests.sha256Hex(state), result.getAttributes().getStateDigest());
        assertFalse(result.toString().contains(state));
        assertFalse(result.toString().contains(nonce));
    }

    @Test
    void loginPageRequiresASavedAuthorizeRequestAndRendersSessionCsrf() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        MockHttpServletRequest savedRequest = authorizeRequest();
        requestCache.saveRequest(savedRequest, new MockHttpServletResponse());
        MockHttpServletRequest loginRequest = new MockHttpServletRequest(
                "GET", "/tenants/1001/login");
        loginRequest.setSession(savedRequest.getSession(false));
        CsrfToken csrf = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "csrf-value");
        loginRequest.setAttribute(CsrfToken.class.getName(), csrf);
        TenantIssuerService issuers = mock(TenantIssuerService.class);
        when(issuers.resolveCurrentIssuer()).thenReturn(trustedTenant());

        var response = new TenantLoginController(issuers, requestCache).loginPage("1001", loginRequest);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("name=\"_csrf\" value=\"csrf-value\""));
        assertTrue(response.getBody().contains("action=\"/tenants/1001/login\""));
        assertFalse(response.getBody().contains("client_id"));

        HttpSessionRequestCache invalidCache = new HttpSessionRequestCache();
        MockHttpServletRequest wrongRequest = new MockHttpServletRequest("GET", "/tenants/1001/oauth2/token");
        wrongRequest.setServerName("idp.example.test");
        wrongRequest.setScheme("https");
        wrongRequest.setQueryString("client_id=public-client");
        invalidCache.saveRequest(wrongRequest, new MockHttpServletResponse());
        MockHttpServletRequest invalidLogin = new MockHttpServletRequest("GET", "/tenants/1001/login");
        invalidLogin.setSession(wrongRequest.getSession(false));
        invalidLogin.setAttribute(CsrfToken.class.getName(), csrf);
        assertEquals(404, new TenantLoginController(issuers, invalidCache)
                .loginPage("1001", invalidLogin).getStatusCode().value());
    }

    @Test
    void loginSubmitRotatesSessionAndResumesOnlyTheSavedAuthorizeRequest() throws Exception {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        MockHttpServletRequest savedRequest = authorizeRequest();
        requestCache.saveRequest(savedRequest, new MockHttpServletResponse());
        MockHttpSession session = (MockHttpSession) savedRequest.getSession(false);
        MockHttpServletRequest loginRequest = loginRequest(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        IdentityFacade identities = mock(IdentityFacade.class);
        TenantMembershipPort memberships = mock(TenantMembershipPort.class);
        TenantIssuerService issuers = mock(TenantIssuerService.class);
        when(issuers.resolveCurrentIssuer()).thenReturn(trustedTenant());
        when(identities.authenticate(eq("alice"), any(char[].class), eq("SAS_LOGIN"), any(Instant.class)))
                .thenReturn(new AuthenticatedIdentity("subject-1", "alice", "Alice", false));
        when(memberships.resolve("subject-1", "1001")).thenReturn(
                new TenantMembershipPort.TenantMembership("subject-1", "1001", "Tenant 1001",
                        TenantMembershipPort.MembershipStatus.ACTIVE));
        SessionAuthenticationStrategy sessionStrategy = mock(SessionAuthenticationStrategy.class);
        TenantLoginAuthenticationFilter filter = new TenantLoginAuthenticationFilter(
                identities, memberships, issuers, requestCache,
                java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC), sessionStrategy);

        try {
            filter.doFilter(loginRequest, response, (request, result) -> { });
            assertEquals(303, response.getStatus());
            assertTrue(response.getHeader("Location").startsWith(
                    "/tenants/1001/oauth2/authorize?client_id=public-client&state=opaque-state"));
            assertNotNull(loginRequest.getSession(false).getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
            ArgumentCaptor<char[]> password = ArgumentCaptor.forClass(char[].class);
            verify(identities).authenticate(eq("alice"), password.capture(), eq("SAS_LOGIN"), any(Instant.class));
            assertTrue(Arrays.equals(password.getValue(), new char[password.getValue().length]));
            verify(sessionStrategy).onAuthentication(any(), eq(loginRequest), eq(response));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    void loginSubmitDoesNotCreateSessionWhenPasswordMustBeChanged() throws Exception {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        MockHttpServletRequest savedRequest = authorizeRequest();
        requestCache.saveRequest(savedRequest, new MockHttpServletResponse());
        MockHttpSession session = (MockHttpSession) savedRequest.getSession(false);
        MockHttpServletRequest loginRequest = loginRequest(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        IdentityFacade identities = mock(IdentityFacade.class);
        TenantMembershipPort memberships = mock(TenantMembershipPort.class);
        TenantIssuerService issuers = mock(TenantIssuerService.class);
        when(issuers.resolveCurrentIssuer()).thenReturn(trustedTenant());
        when(identities.authenticate(eq("alice"), any(char[].class), eq("SAS_LOGIN"), any(Instant.class)))
                .thenReturn(new AuthenticatedIdentity("subject-1", "alice", "Alice", true));
        SessionAuthenticationStrategy sessionStrategy = mock(SessionAuthenticationStrategy.class);
        TenantLoginAuthenticationFilter filter = new TenantLoginAuthenticationFilter(
                identities, memberships, issuers, requestCache,
                java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC), sessionStrategy);

        try {
            filter.doFilter(loginRequest, response, (request, result) -> { });
            assertEquals(403, response.getStatus());
            assertNull(loginRequest.getSession(false).getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
            org.mockito.Mockito.verifyNoInteractions(memberships);
            org.mockito.Mockito.verifyNoInteractions(sessionStrategy);
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    void mapsClientCredentialsSignedAuthorizationIdInsteadOfSasUuid() {
        RegisteredClient registeredClient = RegisteredClient.withId("501")
                .clientId("orders-service")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("orders.read")
                .build();
        Map<String, Object> claims = new java.util.LinkedHashMap<>();
        claims.put("iss", URI.create("https://idp.example.test/platform"));
        claims.put("aud", List.of(URI.create("https://api.example.test/orders")));
        claims.put("sub", "orders-service");
        claims.put("scope", Set.of("orders.read"));
        claims.put("principal_type", "SERVICE");
        claims.put("jti", "91002");
        claims.put("iat", NOW);
        claims.put("nbf", NOW);
        claims.put("exp", NOW.plusSeconds(300));
        claims.put("client_id", "orders-service");
        claims.put("authorization_id", "91001");
        claims.put("client_registration_id", "501");
        claims.put("scope_context", "PLATFORM");
        claims.put("security_version", Map.of("realm", 1L, "client", 2L));
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "synthetic-access-token", NOW, NOW.plusSeconds(300),
                Set.of("orders.read"));
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .principalName("orders-service")
                .authorizedScopes(Set.of("orders.read"))
                .token(accessToken, metadata -> metadata.put(
                        OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claims))
                .build();

        var result = getMapper(SasAuthorizationConverter.class)
                .toSource(authorization, null, new ObjectMapper().findAndRegisterModules());

        assertEquals(91001L, result.getId());
        assertEquals("SERVICE", result.getPrincipalType());
        assertEquals(List.of("https://api.example.test/orders"), result.getAudiences());
        assertEquals(OAuthTokenTypeEnum.ACCESS, result.getTokens().getFirst().getTokenType());
    }

    @Test
    void recordsSuccessfulTokenIssueInsideTransactionalOutboxEnvelope() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
        TrustedRealmBO trustedRealm = trustedTenant();
        OAuthClientRepository clients = mock(OAuthClientRepository.class);
        OAuthAuditRepository audits = mock(OAuthAuditRepository.class);
        TransactionalOutbox outbox = mock(TransactionalOutbox.class);
        OAuthTokenIssueServiceImpl service = new OAuthTokenIssueServiceImpl(
                mock(OAuthTokenRepository.class),
                mock(OAuthAuthorizationRepository.class),
                clients,
                mock(OAuthGrantRepository.class),
                mock(OAuthConsentRepository.class),
                mock(OAuthRealmRepository.class),
                audits,
                outbox,
                mock(OAuthSecurityProjectionComponent.class),
                mock(ClientCredentialsAccessPolicy.class),
                mock(ResourceServerStore.class),
                java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC),
                mock(top.egon.cola.platform.tianquan.shoubing.core.port.TenantMembershipPort.class),
                1_001L);
        when(clients.findById(eq(trustedRealm), eq(501L))).thenReturn(
                Optional.of(OAuthClientBO.builder().id(501L).build()));
        when(outbox.enqueue(any(OutboxMessage.class))).thenAnswer(invocation -> {
            OutboxMessage message = invocation.getArgument(0);
            return new OutboxReceipt(message.messageId(), message.idempotencyKey(), true);
        });

        service.recordClientCredentialsIssued(
                trustedRealm, 501L, URI.create("https://api.example.test/orders"));

        org.mockito.ArgumentCaptor<OutboxMessage> messageCaptor =
                org.mockito.ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outbox).enqueue(messageCaptor.capture());
        OutboxMessage message = messageCaptor.getValue();
        assertEquals(OAuthAuditOutboxDeliveryHandler.CHANNEL, message.channel());
        assertEquals(OAuthAuditOutboxDeliveryHandler.DESTINATION, message.destination());
        assertEquals(message.messageId(), message.idempotencyKey().substring("oauth-audit:".length()));
        assertFalse(message.payload().toString().contains("access_token"));
        verify(audits).save(eq(trustedRealm), any(OAuthAuditBO.class), eq(0L));
    }

    @Test
    void platformUserAuthorizationUsesTheConfiguredManagementTenantMembership() {
        TrustedRealmBO platformRealm = TrustedRealmBO.builder()
                .issuer(URI.create("https://idp.example.test/platform"))
                .realmKey("platform")
                .realmType(RealmTypeEnum.PLATFORM)
                .storagePartitionId(9_000L)
                .businessTenantId(null)
                .trustVersion(1L)
                .build();
        URI resourceUri = URI.create("https://api.example.test/orders");
        OAuthClientBO client = OAuthClientBO.builder()
                .id(501L).version(0L).clientId("admin-client").clientName("Admin Client")
                .clientType(top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient.ClientType.PUBLIC)
                .purpose(OAuthClientPurposeEnum.USER)
                .status(top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient.Status.ACTIVE)
                .credentialVersion(0L).redirectUris(List.of("https://client.example.test/callback"))
                .resourceUris(List.of(resourceUri.toASCIIString())).allowedScopes(List.of("openid"))
                .accessTtlSeconds(300).refreshTtlSeconds(0).build();
        OAuthSecuritySnapshotBO snapshot = OAuthSecuritySnapshotBO.builder()
                .realmGeneration(1L).clientGeneration(2L).subjectGeneration(3L)
                .readyEpoch("epoch-1").revision(4L).build();
        OAuthAuthorizationBO authorization = OAuthAuthorizationBO.builder()
                .id(700L).version(0L).clientPk(501L).principalName("platform-admin-sub")
                .principalType("USER").grantType("authorization_code")
                .authorizedScopes(List.of("openid")).audiences(List.of(resourceUri.toASCIIString()))
                .status("ACTIVE")
                .attributes(OAuthAuthorizationAttributesBO.builder().schemaVersion(1)
                        .securitySnapshot(snapshot).authTime(NOW).acr("urn:egon:loa:password").build())
                .absoluteExpiresAt(NOW.plusSeconds(300)).build();

        OAuthTokenRepository tokens = mock(OAuthTokenRepository.class);
        OAuthAuthorizationRepository authorizations = mock(OAuthAuthorizationRepository.class);
        OAuthClientRepository clients = mock(OAuthClientRepository.class);
        OAuthGrantRepository grants = mock(OAuthGrantRepository.class);
        OAuthConsentRepository consents = mock(OAuthConsentRepository.class);
        OAuthRealmRepository realms = mock(OAuthRealmRepository.class);
        OAuthAuditRepository audits = mock(OAuthAuditRepository.class);
        TransactionalOutbox outbox = mock(TransactionalOutbox.class);
        OAuthSecurityProjectionComponent projection = mock(OAuthSecurityProjectionComponent.class);
        ClientCredentialsAccessPolicy accessPolicy = mock(ClientCredentialsAccessPolicy.class);
        ResourceServerStore resources = mock(ResourceServerStore.class);
        TenantMembershipPort memberships = mock(TenantMembershipPort.class);
        when(clients.findById(eq(platformRealm), eq(501L))).thenReturn(Optional.of(client));
        when(realms.findByRealmKeyForUpdate(eq(platformRealm))).thenReturn(Optional.of(
                OAuthRealmBO.builder().id(9_000L).version(0L).issuer(platformRealm.getIssuer())
                        .realmKey("platform").realmType(RealmTypeEnum.PLATFORM)
                        .storagePartitionId(9_000L).status("ACTIVE").activeKid("platform-kid")
                        .trustVersion(1L).build()));
        when(clients.findByClientIdForUpdate(eq(platformRealm), eq("admin-client")))
                .thenReturn(Optional.of(client));
        when(projection.snapshotForAuthorization(eq(platformRealm), eq(501L), eq("USER"),
                eq("platform-admin-sub"))).thenReturn(snapshot);
        when(memberships.resolve("platform-admin-sub", "9001")).thenReturn(
                new TenantMembershipPort.TenantMembership("platform-admin-sub", "9001", "Admin",
                        TenantMembershipPort.MembershipStatus.ACTIVE));
        when(consents.findConsentForUpdate(eq(platformRealm), eq(501L), eq("platform-admin-sub")))
                .thenReturn(Optional.of(OAuthConsentBO.builder().id(800L).version(0L).clientPk(501L)
                        .subject("platform-admin-sub").authorities(List.of("SCOPE_openid"))
                        .grantedAt(NOW).status("ACTIVE").build()));
        ResourceServer resource = new ResourceServer("orders-resource", resourceUri, "orders", "orders", "prod",
                "orders-service", "orders", "orders:entry", Duration.ofMinutes(5),
                ResourceServerStatus.ACTIVE, 1L);
        when(resources.findByUri(resourceUri)).thenReturn(Optional.of(resource));
        when(grants.findGrantForUpdate(eq(platformRealm), eq(501L), eq("orders-resource"),
                eq(ResourceGrantType.USER_DELEGATION))).thenReturn(Optional.of(OAuthGrantBO.builder()
                        .id(801L).version(1L).clientPk(501L).resourceServerId("orders-resource")
                        .grantType(ResourceGrantType.USER_DELEGATION).allowedScopes(List.of())
                        .status(ClientResourceGrant.Status.ACTIVE).resourceVersion(1L).build()));
        OAuthTokenIssueServiceImpl service = new OAuthTokenIssueServiceImpl(
                tokens, authorizations, clients, grants, consents, realms, audits, outbox,
                projection, accessPolicy, resources, java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC),
                memberships, 9_001L);

        var issue = service.authorizeUserToken(platformRealm, authorization);

        assertEquals(700L, issue.authorizationId());
        assertEquals(Set.of(resourceUri), issue.audiences());
        verify(memberships).resolve("platform-admin-sub", "9001");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deliversOnlyWhitelistedAuditFieldsToTheStreamsQueue() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        RStream<String, String> stream = mock(RStream.class);
        doReturn(stream).when(redisson)
                .getStream(eq(OAuthAuditOutboxDeliveryHandler.STREAM), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class))).thenReturn(mock(StreamMessageId.class));
        OAuthAuditOutboxDeliveryHandler handler = new OAuthAuditOutboxDeliveryHandler(
                redisson, new ObjectMapper().findAndRegisterModules());
        String eventId = "event-1";
        Map<String, Object> event = Map.of(
                "eventId", eventId,
                "schemaVersion", 1,
                "issuer", "https://idp.example.test/tenants/1001",
                "resource", "https://api.example.test/orders",
                "clientRef", "a".repeat(64),
                "result", "SUCCESS",
                "reasonCode", "CLIENT_CREDENTIALS_TOKEN_ISSUED",
                "occurredAt", NOW.toString());
        DeliveryContext context = new DeliveryContext(
                eventId, OAuthAuditOutboxDeliveryHandler.CHANNEL, OAuthAuditOutboxDeliveryHandler.DESTINATION,
                new ObjectMapper().writeValueAsString(event), "application/json", "1", Map.of(), null,
                1, 5, NOW.plusSeconds(30));

        DeliveryResult result = handler.deliver(context);

        assertEquals(DeliveryResult.Kind.SUCCESS, result.kind());
        verify(stream).add(any(StreamAddArgs.class));
        Map<String, Object> invalidEvent = new java.util.LinkedHashMap<>(event);
        invalidEvent.put("password", "must-not-be-delivered");
        DeliveryContext invalid = new DeliveryContext(
                eventId, OAuthAuditOutboxDeliveryHandler.CHANNEL, OAuthAuditOutboxDeliveryHandler.DESTINATION,
                new ObjectMapper().writeValueAsString(invalidEvent), "application/json", "1", Map.of(), null,
                1, 5, NOW.plusSeconds(30));
        assertEquals(DeliveryResult.Kind.PERMANENT_FAILURE, handler.deliver(invalid).kind());
    }

    private static TrustedRealmBO trustedTenant() {
        return TrustedRealmBO.builder()
                .issuer(URI.create("https://idp.example.test/tenants/1001"))
                .realmKey("tenant:1001")
                .realmType(RealmTypeEnum.TENANT)
                .storagePartitionId(1_001L)
                .businessTenantId(1_001L)
                .trustVersion(1L)
                .build();
    }

    private static MockHttpServletRequest authorizeRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/tenants/1001/oauth2/authorize");
        request.setScheme("https");
        request.setServerName("idp.example.test");
        request.setServerPort(443);
        request.setQueryString("client_id=public-client&state=opaque-state");
        request.addParameter("client_id", "public-client");
        request.addParameter("state", "opaque-state");
        return request;
    }

    private static MockHttpServletRequest loginRequest(MockHttpSession session) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/tenants/1001/login");
        request.setScheme("https");
        request.setServerName("idp.example.test");
        request.setServerPort(443);
        request.setContentType("application/x-www-form-urlencoded");
        request.setSession(session);
        request.addParameter("username", "alice");
        request.addParameter("password", "must-not-leak");
        request.addParameter("_csrf", "csrf-value");
        return request;
    }

    private static OAuthSecurityStateBO securityState(
            OAuthSecurityTargetEnum targetType,
            String targetKey,
            long generation,
            long revision) {
        return OAuthSecurityStateBO.builder()
                .id(1L)
                .version(0L)
                .targetType(targetType)
                .targetKey(targetKey)
                .generation(generation)
                .enabled(true)
                .revision(revision)
                .reasonCode("INITIALIZED")
                .build();
    }
}
