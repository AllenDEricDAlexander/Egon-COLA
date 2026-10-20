package top.egon.cola.platform.tianquan.shoubing.admin.oauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import java.time.Instant;
import java.net.URI;
import java.time.Duration;
import java.time.Clock;
import java.time.ZoneOffset;
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
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenRevocationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenRevocationAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
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
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthTokenIssueService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.TransactionalOAuthRevocationAuthenticationProvider;
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
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServer;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSecurityStateBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.SignedTokenClaimsBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthRevocationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.config.OAuthRevocationProperties;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthRevocationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSecurityStateRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.RevokeOAuthTokenCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.token.support.outbox.OAuthSecurityDeliveryHandler;
import top.egon.cola.platform.tianquan.shoubing.core.audit.OAuthSecurityChangedEvent;

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

    @Test
    void loginRequestCachePreservesTheExactProtocolQueryWithoutAnInternalContinueParameter() {
        var cache = new AuthorizationServerConfiguration().oauthAuthorizationRequestCache();
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/tenants/1001/oauth2/authorize");
        request.setScheme("https");
        request.setServerName("issuer.example");
        request.setServerPort(443);
        request.setQueryString("response_type=code&client_id=controlled-client&state=controlled-state");
        var response = new org.springframework.mock.web.MockHttpServletResponse();

        cache.saveRequest(request, response);

        assertEquals("https://issuer.example/tenants/1001/oauth2/authorize?response_type=code&client_id=controlled-client&state=controlled-state",
                cache.getRequest(request, response).getRedirectUrl());
    }

    private static final Instant NOW = Instant.parse("2026-10-08T08:00:00Z");

    @Test
    void idMetadataRetainsOidcFieldsWithoutWeakeningAccessTokenRequiredGroups() throws Exception {
        SignedTokenClaimsBO id = SignedTokenClaimsBO.builder().issuer(URI.create("https://idp.example.test/tenants/1001"))
                .audience(Set.of(URI.create("oidc-client"))).subject("fixture-subject").jti("1234567")
                .issuedAt(NOW).expiresAt(NOW.plusSeconds(300)).authTime(NOW).acr("PASSWORD")
                .tokenUse(OAuthTokenTypeEnum.ID).authorizedParty("oidc-client").nonce("N".repeat(64))
                .sessionId("fixture-session").build();
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        SignedTokenClaimsBO restored = mapper.readValue(mapper.writeValueAsString(id), SignedTokenClaimsBO.class);
        assertEquals(id.getNonce(), restored.getNonce());
        assertEquals(id.getSessionId(), restored.getSessionId());
        assertEquals(id.getAuthorizedParty(), restored.getAuthorizedParty());
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(restored).isEmpty());
            restored.setTokenUse(OAuthTokenTypeEnum.ACCESS);
            Set<String> required = validator.validate(restored).stream()
                    .map(value -> value.getPropertyPath().toString()).collect(java.util.stream.Collectors.toSet());
            assertTrue(required.containsAll(Set.of("scope", "clientId", "clientRegistrationId", "authorizationId",
                    "principalType", "scopeContext", "securityVersion", "notBefore")));
            restored.setTokenUse(OAuthTokenTypeEnum.ID).setAuthorizedParty(null);
            assertTrue(validator.validate(restored).stream().anyMatch(value -> value.getPropertyPath().toString().equals("authorizedParty")));
            restored.setAuthorizedParty("oidc-client").setTokenUse(OAuthTokenTypeEnum.CONSENT_STATE);
            assertFalse(validator.validate(restored).isEmpty());
        }
    }

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
    void decoratesThePinnedSasRevocationProvider() {
        OAuth2AuthorizationService authorizationService = mock(OAuth2AuthorizationService.class);
        TokenRevocationService revocationService = mock(TokenRevocationService.class);
        TenantIssuerService issuerService = mock(TenantIssuerService.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        List<AuthenticationProvider> providers = new ArrayList<>(List.of(
                new OAuth2TokenRevocationAuthenticationProvider(authorizationService)));

        AuthorizationServerConfiguration.configureRevocationProviders(providers,
                authorizationService, revocationService, issuerService, transactionManager);

        assertTrue(providers.get(0) instanceof TransactionalOAuthRevocationAuthenticationProvider);
        assertTrue(providers.get(0).supports(OAuth2TokenRevocationAuthenticationToken.class));
    }

    @Test
    void standardSasClientChecksAndDurableRevocationShareOneTransaction() {
        OAuth2AuthorizationService authorizations = mock(OAuth2AuthorizationService.class);
        TokenRevocationService revocations = mock(TokenRevocationService.class);
        TenantIssuerService issuers = mock(TenantIssuerService.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionStatus status = mock(TransactionStatus.class);
        when(status.isNewTransaction()).thenReturn(true);
        when(transactionManager.getTransaction(any())).thenReturn(status);
        TrustedRealmBO realm = TrustedRealmBO.builder()
                .issuer(URI.create("https://idp.example/tenants/1001"))
                .realmKey("tenant:1001")
                .realmType(RealmTypeEnum.TENANT)
                .storagePartitionId(1001L)
                .businessTenantId(1001L)
                .trustVersion(0L)
                .build();
        when(issuers.resolveCurrentIssuer()).thenReturn(realm);
        RegisteredClient client = RegisteredClient.withId("501")
                .clientId("orders-app")
                .clientSecret("secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        OAuth2ClientAuthenticationToken clientAuthentication = new OAuth2ClientAuthenticationToken(
                client, ClientAuthenticationMethod.CLIENT_SECRET_BASIC, "secret");
        String rawToken = "opaque-token-value";
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(client)
                .principalName("orders-app")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .token(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, rawToken,
                        NOW.minusSeconds(1), NOW.plusSeconds(600)))
                .build();
        when(authorizations.findByToken(rawToken, null)).thenReturn(authorization);
        List<AuthenticationProvider> providers = new ArrayList<>(List.of(
                new OAuth2TokenRevocationAuthenticationProvider(authorizations)));
        AuthorizationServerConfiguration.configureRevocationProviders(
                providers, authorizations, revocations, issuers, transactionManager);
        OAuth2TokenRevocationAuthenticationToken request = new OAuth2TokenRevocationAuthenticationToken(
                rawToken, clientAuthentication, "access_token");

        var result = providers.get(0).authenticate(request);

        assertTrue(result.isAuthenticated());
        verify(authorizations).save(any(OAuth2Authorization.class));
        var command = ArgumentCaptor.forClass(RevokeOAuthTokenCommand.class);
        verify(revocations).revoke(eq(realm), command.capture());
        assertEquals(Digests.sha256Hex(rawToken), command.getValue().getTokenDigest());
        assertEquals(501L, command.getValue().getClientPk());
        assertEquals("RFC7009_REVOKE", command.getValue().getReasonCode());
        verify(transactionManager).commit(status);
    }

    @Test
    void securityOutboxReloadsTrustedRealmAndMarksRevocationApplied() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        TenantIssuerService issuers = mock(TenantIssuerService.class);
        OAuthSecurityProjectionComponent projection = mock(OAuthSecurityProjectionComponent.class);
        OAuthRevocationRepository revocations = mock(OAuthRevocationRepository.class);
        TrustedRealmBO realm = trustedTenant();
        OAuthRevocationBO pending = OAuthRevocationBO.builder()
                .id(701L).version(4L).targetType(OAuthSecurityTargetEnum.JTI).targetKey("123456")
                .reasonCode("ADMIN_REVOKE").revokedAt(NOW).expiresAt(NOW.plusSeconds(300))
                .revision(8L).projectionStatus("PENDING").build();
        when(issuers.resolveExactIssuer(realm.getIssuer())).thenReturn(realm);
        when(revocations.findByTarget(realm, OAuthSecurityTargetEnum.JTI, "123456"))
                .thenReturn(Optional.of(pending));
        when(revocations.save(eq(realm), any(OAuthRevocationBO.class), eq(4L)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        OAuthSecurityDeliveryHandler handler = new OAuthSecurityDeliveryHandler(
                mapper, issuers, projection, revocations);
        String eventId = "security-event-1";
        String payload = mapper.writeValueAsString(Map.of(
                "eventId", eventId,
                "schemaVersion", 1,
                "issuer", realm.getIssuer().toASCIIString(),
                "realmKey", realm.getRealmKey(),
                "revision", 8L,
                "targetType", "JTI",
                "targetKey", "123456",
                "expiresAt", NOW.plusSeconds(300).toString(),
                "reasonCode", "ADMIN_REVOKE",
                "occurredAt", NOW.toString()));
        DeliveryContext context = new DeliveryContext(
                eventId, OAuthSecurityDeliveryHandler.CHANNEL, OAuthSecurityDeliveryHandler.DESTINATION,
                payload, "application/json", "1", Map.of(), null, 1, 5, NOW);

        DeliveryResult result = handler.deliver(context);

        assertEquals(DeliveryResult.Kind.SUCCESS, result.kind());
        verify(projection).applyProjection(eq(realm), any(OAuthSecurityChangedEvent.class));
        ArgumentCaptor<OAuthRevocationBO> saved = ArgumentCaptor.forClass(OAuthRevocationBO.class);
        verify(revocations).save(eq(realm), saved.capture(), eq(4L));
        assertEquals("APPLIED", saved.getValue().getProjectionStatus());
        assertEquals(5L, saved.getValue().getVersion());
    }

    @Test
    void securityOutboxRejectsUnlistedSecretFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        TenantIssuerService issuers = mock(TenantIssuerService.class);
        OAuthSecurityProjectionComponent projection = mock(OAuthSecurityProjectionComponent.class);
        OAuthRevocationRepository revocations = mock(OAuthRevocationRepository.class);
        OAuthSecurityDeliveryHandler handler = new OAuthSecurityDeliveryHandler(
                mapper, issuers, projection, revocations);
        Map<String, Object> fields = new java.util.LinkedHashMap<>(Map.of(
                "eventId", "security-event-2",
                "schemaVersion", 1,
                "issuer", "https://idp.example.test/tenants/1001",
                "realmKey", "tenant:1001",
                "revision", 9L,
                "targetType", "JTI",
                "targetKey", "123457",
                "reasonCode", "ADMIN_REVOKE",
                "occurredAt", NOW.toString()));
        fields.put("token", "must-not-be-persisted");
        DeliveryContext context = new DeliveryContext(
                "security-event-2", OAuthSecurityDeliveryHandler.CHANNEL,
                OAuthSecurityDeliveryHandler.DESTINATION, mapper.writeValueAsString(fields),
                "application/json", "1", Map.of(), null, 1, 5, NOW);

        assertEquals(DeliveryResult.Kind.PERMANENT_FAILURE, handler.deliver(context).kind());
        org.mockito.Mockito.verifyNoInteractions(projection, revocations, issuers);
    }

    @Test
    @SuppressWarnings("unchecked")
    void readsOneSameSlotSecuritySnapshotAndFailsWhenTheReadyFenceIsMissing() {
        RedissonClient redisson = mock(RedissonClient.class);
        RScript script = mock(RScript.class);
        doReturn(script).when(redisson).getScript(StringCodec.INSTANCE);
        OAuthSecurityStateRepository states = mock(OAuthSecurityStateRepository.class);
        OAuthRevocationRepository revocations = mock(OAuthRevocationRepository.class);
        OAuthTokenRepository tokenRecords = mock(OAuthTokenRepository.class);
        OAuthAuthorizationRepository authorizations = mock(OAuthAuthorizationRepository.class);
        OAuthClientRepository clients = mock(OAuthClientRepository.class);
        OAuthRealmRepository realms = mock(OAuthRealmRepository.class);
        OAuthManagementRealmResolver realmResolver = mock(OAuthManagementRealmResolver.class);
        OAuthRevocationProperties revocationProperties = OAuthRevocationProperties.builder()
                .primaryEndpoint(URI.create("rediss://redis.example:6379"))
                .epoch("epoch-1")
                .build();
        TrustedRealmBO trustedRealm = trustedTenant();
        when(states.findByTarget(eq(trustedRealm), eq(OAuthSecurityTargetEnum.REALM), eq("self")))
                .thenReturn(Optional.of(securityState(OAuthSecurityTargetEnum.REALM, "self", 1L, 4L)));
        when(states.findByTarget(eq(trustedRealm), eq(OAuthSecurityTargetEnum.CLIENT), eq("501")))
                .thenReturn(Optional.of(securityState(OAuthSecurityTargetEnum.CLIENT, "501", 2L, 4L)));
        when(states.findByTarget(eq(trustedRealm), eq(OAuthSecurityTargetEnum.SUBJECT), eq("subject-1")))
                .thenReturn(Optional.of(securityState(OAuthSecurityTargetEnum.SUBJECT, "subject-1", 3L, 4L)));
        List<Object> observedKeys = new ArrayList<>();
        org.mockito.Mockito.doAnswer(invocation -> {
            observedKeys.addAll(invocation.getArgument(3));
            return List.of("epoch-1", "4", "READY", "1", "true", "4",
                    "2", "true", "4", "3", "true", "4");
        }).when(script).<List<Object>>eval(
                eq(RScript.Mode.READ_ONLY), org.mockito.ArgumentMatchers.anyString(),
                eq(RScript.ReturnType.MULTI), org.mockito.ArgumentMatchers.anyList());
        OAuthSecurityProjectionComponent projection = new OAuthSecurityProjectionComponent(
                redisson, states, revocations, tokenRecords, authorizations, clients, realms, realmResolver,
                revocationProperties,
                Clock.fixed(Instant.parse("2026-10-08T12:00:00Z"), ZoneOffset.UTC));

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
    void carriesAppAndVersionBoundCredentialIdentityIntoServiceJwtClaims() throws Exception {
        Set<String> issueContextFields = Arrays.stream(
                        OAuthTokenIssueService.ClientCredentialsIssueContext.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(issueContextFields.containsAll(Set.of("appId", "credentialId")));

        var claimMapper = Arrays.stream(AuthorizationServerConfiguration.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("serviceIdentityClaims"))
                .findFirst()
                .orElse(null);
        assertNotNull(claimMapper);
        @SuppressWarnings("unchecked")
        Map<String, Object> claims = (Map<String, Object>) claimMapper.invoke(
                null, "stable-app-42", "501:8");
        assertEquals("stable-app-42", claims.get("app_id"));
        assertEquals("501:8", claims.get("credential_id"));
    }

    @Test
    void authorizesServiceFromGrantsWithEmptyUserResourceListAndLockedCredentialVersion() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
        TrustedRealmBO realm = trustedTenant();
        URI targetUri = URI.create("https://api.example.test/orders");
        Set<String> requestedScopes = Set.of("orders:read");
        OAuthClientRepository clients = mock(OAuthClientRepository.class);
        OAuthRealmRepository realms = mock(OAuthRealmRepository.class);
        OAuthSecurityProjectionComponent projection = mock(OAuthSecurityProjectionComponent.class);
        ResourceServerStore resources = mock(ResourceServerStore.class);
        ClientCredentialsAccessPolicy accessPolicy = new ClientCredentialsAccessPolicy(resources);
        OAuthGrantRepository grants = mock(OAuthGrantRepository.class);
        OAuthClientBO client = OAuthClientBO.builder()
                .id(501L).version(2L).clientId("orders-service").appId("stable-app-42")
                .clientType(OAuthClient.ClientType.CONFIDENTIAL)
                .purpose(OAuthClientPurposeEnum.SERVICE).status(OAuthClient.Status.ACTIVE)
                .credentialVersion(8L).resourceUris(List.of())
                .accessTtlSeconds(300).refreshTtlSeconds(0).build();
        ResourceServer target = new ResourceServer(
                "orders-resource", targetUri, "orders", "orders", "prod",
                "orders-service", "orders", "orders:entry", Duration.ofMinutes(5),
                ResourceServerStatus.ACTIVE, 2L);
        ResourceServer source = new ResourceServer(
                "source-resource", URI.create("https://source.example.test/orders"), "commerce", "orders", "prod",
                "orders-service", "orders", "orders:entry", Duration.ofMinutes(5),
                ResourceServerStatus.ACTIVE, 1L);
        ClientResourceGrant grant = new ClientResourceGrant(
                "orders-service", "orders-resource", ResourceGrantType.CLIENT_CREDENTIALS, "1001",
                requestedScopes, ClientResourceGrant.Status.ACTIVE, 4L,
                top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.TENANT);
        OAuthSecuritySnapshotBO snapshot = OAuthSecuritySnapshotBO.builder()
                .realmGeneration(1L).clientGeneration(2L).readyEpoch("epoch-1").revision(4L).build();
        when(clients.findById(realm, 501L)).thenReturn(Optional.of(client));
        when(realms.findByRealmKeyForUpdate(realm)).thenReturn(Optional.of(
                OAuthRealmBO.builder().id(1001L).build()));
        when(clients.findByClientIdForUpdate(realm, "orders-service"))
                .thenReturn(Optional.of(client));
        when(projection.snapshotForAuthorization(realm, 501L, "SERVICE", null))
                .thenReturn(snapshot);
        when(resources.findByUri(targetUri)).thenReturn(Optional.of(target));
        when(resources.findByManagementClientId("orders-service")).thenReturn(Optional.of(source));
        when(resources.findGrant("orders-service", "orders-resource", ResourceGrantType.CLIENT_CREDENTIALS, "1001"))
                .thenReturn(Optional.of(grant));
        when(grants.findGrantForUpdate(realm, 501L, "orders-resource",
                ResourceGrantType.CLIENT_CREDENTIALS)).thenReturn(Optional.of(OAuthGrantBO.builder()
                .id(700L).version(4L).clientPk(501L).resourceServerId("orders-resource")
                .grantType(ResourceGrantType.CLIENT_CREDENTIALS)
                .allowedScopes(List.of("orders:read"))
                .status(ClientResourceGrant.Status.ACTIVE).resourceVersion(2L).build()));
        OAuthTokenIssueServiceImpl service = new OAuthTokenIssueServiceImpl(
                mock(OAuthTokenRepository.class), mock(OAuthAuthorizationRepository.class), clients,
                grants, mock(OAuthConsentRepository.class), realms, mock(OAuthAuditRepository.class),
                mock(TransactionalOutbox.class), projection, accessPolicy, resources,
                Clock.fixed(NOW, ZoneOffset.UTC), mock(TenantMembershipPort.class), 1001L);

        OAuthTokenIssueService.ClientCredentialsIssueContext result =
                service.authorizeClientCredentials(realm, 501L, targetUri, requestedScopes);

        assertEquals("stable-app-42", result.appId());
        assertEquals("501:8", result.credentialId());
        assertEquals("1001", result.resourceAccess().tenantId());
        assertEquals(requestedScopes, result.resourceAccess().scopes());

        assertThrows(top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceAuthorizationException.class,
                () -> service.authorizeClientCredentials(realm, 501L, targetUri, Set.of("orders:write")));
        when(resources.findGrant("orders-service", "orders-resource", ResourceGrantType.CLIENT_CREDENTIALS, "1001"))
                .thenReturn(Optional.empty());
        assertThrows(top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceAuthorizationException.class,
                () -> service.authorizeClientCredentials(realm, 501L, targetUri, requestedScopes));
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
                redisson, new ObjectMapper().findAndRegisterModules(), OAuthAuditOutboxDeliveryHandler.STREAM);
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
