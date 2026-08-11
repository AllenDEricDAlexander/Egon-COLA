package top.egon.cola.platform.idp.core.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.core.port.AuthorizationCodeStore;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;
import top.egon.cola.platform.idp.core.port.UserResourceAccessAuthorizationPort;
import top.egon.cola.platform.idp.core.resource.ClientResourceGrant;
import top.egon.cola.platform.idp.core.resource.ResourceGrantType;
import top.egon.cola.platform.idp.core.resource.ResourceServer;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;
import top.egon.cola.platform.idp.core.resource.UserResourceAccessPolicy;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationFacadeTest {

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");
    private static final String CLIENT_ID = "gateway-admin-web";
    private static final String REDIRECT_URI =
            "https://gateway.example.test/oauth/callback";
    private static final String RESOURCE =
            "https://api.egon.internal/prod/platform/gateway";
    private static final String OTHER_RESOURCE =
            "https://api.egon.internal/prod/platform/ddc";
    private static final String VERIFIER =
            "0123456789abcdefghijklmnopqrstuvwxyz-._~ABCDE";
    private static final String RAW_CODE =
            "authorization-code-secret-value-0123456789";

    private FakeOAuthClientStore clients;
    private FakeAuthorizationCodeStore codes;
    private FakeTenantMembershipPort memberships;
    private FakeResourceServerStore resources;
    private FakeResourceDecisionPort decisions;
    private AuthorizationFacade facade;

    @BeforeEach
    void setUp() {
        clients = new FakeOAuthClientStore();
        clients.put(new OAuthClient(
                CLIENT_ID,
                OAuthClient.ClientType.PUBLIC,
                OAuthClient.Status.ACTIVE,
                true,
                List.of(REDIRECT_URI)
        ));
        codes = new FakeAuthorizationCodeStore();
        memberships = new FakeTenantMembershipPort();
        memberships.put(new TenantMembershipPort.TenantMembership(
                "alice-sub",
                "tenant-a",
                "tenant-user-a",
                "Tenant A",
                TenantMembershipPort.MembershipStatus.ACTIVE
        ));
        resources = new FakeResourceServerStore();
        resources.put(resource("gateway", RESOURCE, ResourceServerStatus.ACTIVE));
        resources.put(resource("ddc", OTHER_RESOURCE, ResourceServerStatus.ACTIVE));
        resources.grant(CLIENT_ID, "platform-gateway-prod");
        resources.grant(CLIENT_ID, "platform-ddc-prod");
        decisions = new FakeResourceDecisionPort();
        facade = facade();
    }

    @Test
    void storesOnlyCodeDigestAndBindsOneResourceBeforeOneTimeExchange() {
        AuthorizationFacade.AuthorizationResult result = facade.authorize(
                validRequest(), "alice-sub", "sso-session-1");

        assertEquals(RAW_CODE, result.code());
        assertEquals("state-123", result.state());
        assertEquals(NOW.plusSeconds(60), result.expiresAt());
        assertFalse(codes.values.containsKey(RAW_CODE));
        assertNotEquals(RAW_CODE, codes.singleDigest());
        assertEquals(Duration.ofSeconds(60), codes.singleTtl());

        AuthorizationCode consumed = facade.consume(
                result.code(), VERIFIER, REDIRECT_URI, CLIENT_ID, RESOURCE);

        assertEquals("alice-sub", consumed.identitySub());
        assertEquals("tenant-a", consumed.tenantId());
        assertEquals("tenant-user-a", consumed.rbac3UserId());
        assertEquals("sso-session-1", consumed.sessionId());
        assertEquals(URI.create(RESOURCE), consumed.resourceUri());
        assertEquals("platform-gateway-prod", consumed.resourceServerId());
        assertEquals(7L, consumed.resourceVersion());
        assertOAuthGrantFailure(() -> facade.consume(
                result.code(), VERIFIER, REDIRECT_URI, CLIENT_ID, RESOURCE));
    }

    @Test
    void rejectsMissingRelativeFragmentedDisabledAndUngrantedResource() {
        assertOAuthError("invalid_request", validRequest().withResource(""));
        assertOAuthError("invalid_target", validRequest().withResource("/gateway"));
        assertOAuthError("invalid_target",
                validRequest().withResource(RESOURCE + "#fragment"));

        resources.put(resource("gateway", RESOURCE, ResourceServerStatus.DISABLED));
        assertOAuthError("invalid_target", validRequest());

        resources.put(resource("gateway", RESOURCE, ResourceServerStatus.ACTIVE));
        resources.revoke(CLIENT_ID, "platform-gateway-prod");
        assertOAuthError("invalid_target", validRequest());
    }

    @Test
    void userAllowedForApplicationACannotReceiveApplicationBResource() {
        decisions.denyApplication("ddc");

        OAuthException exception = assertThrows(OAuthException.class,
                () -> facade.authorize(validRequest().withResource(OTHER_RESOURCE),
                        "alice-sub", "sso-session-1"));

        assertEquals("access_denied", exception.oauthError());
        assertEquals(0, codes.values.size());
    }

    @Test
    void exchangeRejectsDifferentResourceAndRechecksCurrentPolicy() {
        String code = facade.authorize(
                validRequest(), "alice-sub", "sso-session-1").code();

        assertOAuthGrantFailure(() -> facade.consume(
                code, VERIFIER, REDIRECT_URI, CLIENT_ID, OTHER_RESOURCE));

        String second = facade.authorize(
                validRequest(), "alice-sub", "sso-session-1").code();
        resources.put(resource("gateway", RESOURCE, ResourceServerStatus.DISABLED));
        OAuthException disabled = assertThrows(OAuthException.class,
                () -> facade.consume(
                        second, VERIFIER, REDIRECT_URI, CLIENT_ID, RESOURCE));
        assertEquals("access_denied", disabled.oauthError());
    }

    @Test
    void mapsExplicitRbacDenyAndUnavailableDecisionSeparately() {
        decisions.denyApplication("gateway");
        assertOAuthError("access_denied", validRequest());

        decisions.unavailable = true;
        OAuthException unavailable = assertThrows(OAuthException.class,
                () -> facade.authorize(
                        validRequest(), "alice-sub", "sso-session-1"));
        assertEquals("temporarily_unavailable", unavailable.oauthError());
    }

    @Test
    void refusesAuthorizationWhenMembershipIsInactiveOrUnavailable() {
        memberships.remove("alice-sub", "tenant-a", CLIENT_ID);
        assertOAuthError("access_denied", validRequest());

        memberships.put(new TenantMembershipPort.TenantMembership(
                "alice-sub", "tenant-a", "tenant-user-a", "Tenant A",
                TenantMembershipPort.MembershipStatus.DISABLED));
        assertOAuthError("access_denied", validRequest());
    }

    @Test
    void requiresExactRedirectActiveClientStateNonceAndS256() {
        assertOAuthError("invalid_request",
                validRequest().withRedirectUri(REDIRECT_URI + "/"));
        clients.put(clients.get(CLIENT_ID).withStatus(OAuthClient.Status.DISABLED));
        assertOAuthError("unauthorized_client", validRequest());
        clients.put(clients.get(CLIENT_ID).withStatus(OAuthClient.Status.ACTIVE));
        assertOAuthError("invalid_request", validRequest().withState(""));
        assertOAuthError("invalid_request", validRequest().withNonce(""));
        assertOAuthError("invalid_request",
                validRequest().withCodeChallengeMethod("plain"));
        assertOAuthError("invalid_request",
                validRequest().withResponseType("token"));
    }

    @Test
    void usesTheCurrentAuthorizationCodeTtlForEachNewGrant() {
        facade = new AuthorizationFacade(
                clients, codes, policy(), Clock.fixed(NOW, ZoneOffset.UTC),
                () -> RAW_CODE, () -> Duration.ofSeconds(90));

        AuthorizationFacade.AuthorizationResult result = facade.authorize(
                validRequest(), "alice-sub", "sso-session-1");

        assertEquals(NOW.plusSeconds(90), result.expiresAt());
        assertEquals(Duration.ofSeconds(90), codes.singleTtl());
    }

    private AuthorizationFacade facade() {
        return new AuthorizationFacade(
                clients, codes, policy(), Clock.fixed(NOW, ZoneOffset.UTC),
                () -> RAW_CODE);
    }

    private UserResourceAccessPolicy policy() {
        return new UserResourceAccessPolicy(resources, memberships, decisions);
    }

    private AuthorizationRequest validRequest() {
        return new AuthorizationRequest(
                "code", CLIENT_ID, REDIRECT_URI, RESOURCE, "tenant-a",
                "state-123", "nonce-123", s256(VERIFIER), "S256");
    }

    private ResourceServer resource(
            String appCode, String uri, ResourceServerStatus status) {
        return new ResourceServer(
                "platform-" + appCode + "-prod", URI.create(uri), "platform",
                appCode, "prod", appCode + "-service", appCode,
                appCode + ":access", Duration.ofMinutes(5), status, 7L);
    }

    private void assertOAuthError(String expectedError, AuthorizationRequest request) {
        OAuthException exception = assertThrows(OAuthException.class,
                () -> facade.authorize(request, "alice-sub", "sso-session-1"));
        assertEquals(expectedError, exception.oauthError());
    }

    private void assertOAuthGrantFailure(Runnable action) {
        OAuthException exception = assertThrows(OAuthException.class, action::run);
        assertEquals("invalid_grant", exception.oauthError());
    }

    private static String s256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class FakeOAuthClientStore implements OAuthClientStore {
        private final Map<String, OAuthClient> values = new HashMap<>();

        @Override
        public Optional<OAuthClient> findById(String clientId) {
            return Optional.ofNullable(values.get(clientId));
        }

        void put(OAuthClient client) {
            values.put(client.clientId(), client);
        }

        OAuthClient get(String clientId) {
            return values.get(clientId);
        }
    }

    private static final class FakeAuthorizationCodeStore implements AuthorizationCodeStore {
        private final Map<String, AuthorizationCode> values = new HashMap<>();
        private final Map<String, Duration> timeouts = new HashMap<>();

        @Override
        public void put(String codeDigest, AuthorizationCode code, Duration ttl) {
            values.put(codeDigest, code);
            timeouts.put(codeDigest, ttl);
        }

        @Override
        public AuthorizationCode consume(String codeDigest) {
            return values.remove(codeDigest);
        }

        String singleDigest() {
            return values.keySet().iterator().next();
        }

        Duration singleTtl() {
            return timeouts.values().iterator().next();
        }
    }

    private static final class FakeTenantMembershipPort implements TenantMembershipPort {
        private final Map<String, TenantMembership> values = new HashMap<>();

        @Override
        public TenantMembership resolve(String identitySub, String tenantId, String clientId) {
            TenantMembership membership = values.get(key(identitySub, tenantId, clientId));
            if (membership == null) {
                throw new TenantMembershipException("membership unavailable");
            }
            return membership;
        }

        @Override
        public List<TenantMembership> list(String identitySub, String clientId) {
            return values.values().stream()
                    .filter(value -> value.identitySub().equals(identitySub)).toList();
        }

        void put(TenantMembership membership) {
            values.put(key(membership.identitySub(), membership.tenantId(), CLIENT_ID),
                    membership);
        }

        void remove(String identitySub, String tenantId, String clientId) {
            values.remove(key(identitySub, tenantId, clientId));
        }

        private String key(String identitySub, String tenantId, String clientId) {
            return identitySub + ':' + tenantId + ':' + clientId;
        }
    }

    private static final class FakeResourceServerStore implements ResourceServerStore {
        private final Map<URI, ResourceServer> values = new HashMap<>();
        private final Map<String, ClientResourceGrant> grants = new HashMap<>();

        void put(ResourceServer resource) {
            values.put(resource.resourceUri(), resource);
        }

        void grant(String clientId, String resourceServerId) {
            grants.put(clientId + ':' + resourceServerId,
                    new ClientResourceGrant(clientId, resourceServerId,
                            ResourceGrantType.USER_DELEGATION, null, Set.of(),
                            ClientResourceGrant.Status.ACTIVE, 3L));
        }

        void revoke(String clientId, String resourceServerId) {
            grants.remove(clientId + ':' + resourceServerId);
        }

        @Override
        public Optional<ResourceServer> findById(String resourceServerId) {
            return values.values().stream()
                    .filter(value -> value.resourceServerId().equals(resourceServerId))
                    .findFirst();
        }

        @Override
        public Optional<ResourceServer> findByUri(URI resourceUri) {
            return Optional.ofNullable(values.get(resourceUri));
        }

        @Override
        public Optional<ResourceServer> findByScope(
                String bizCode, String appCode, String environment) {
            return values.values().stream()
                    .filter(value -> value.matches(bizCode, appCode, environment))
                    .findFirst();
        }

        @Override
        public Optional<ResourceServer> findByManagementClientId(String clientId) {
            return values.values().stream()
                    .filter(value -> value.managementClientId().equals(clientId))
                    .findFirst();
        }

        @Override
        public Optional<ClientResourceGrant> findGrant(
                String clientId, String resourceServerId,
                ResourceGrantType grantType, String tenantId) {
            return Optional.ofNullable(grants.get(clientId + ':' + resourceServerId));
        }
    }

    private static final class FakeResourceDecisionPort
            implements UserResourceAccessAuthorizationPort {
        private final Set<String> deniedApplications = new java.util.HashSet<>();
        private boolean unavailable;

        @Override
        public AccessDecision decide(AccessRequest request) {
            if (unavailable) {
                throw new AccessUnavailableException("RBAC3 unavailable");
            }
            if (deniedApplications.contains(request.rbacApplicationCode())) {
                return new AccessDecision(Decision.DENY, "ENTRY_PERMISSION_DENIED",
                        43L, 2L, 18L);
            }
            return new AccessDecision(Decision.ALLOW, "ALLOW", 43L, 2L, 18L);
        }

        void denyApplication(String applicationCode) {
            deniedApplications.add(applicationCode);
            unavailable = false;
        }
    }
}
