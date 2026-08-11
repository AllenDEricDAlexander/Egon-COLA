package top.egon.cola.platform.idp.core.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.contract.PrincipalType;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEvent;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.core.oauth.AuthorizationCode;
import top.egon.cola.platform.idp.core.oauth.OAuthClient;
import top.egon.cola.platform.idp.core.port.IdentityUserStatePort;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.OAuthClientStore;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;
import top.egon.cola.platform.idp.core.port.ResourceServerStore;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;
import top.egon.cola.platform.idp.core.port.TokenSigner;
import top.egon.cola.platform.idp.core.port.UserResourceAccessAuthorizationPort;
import top.egon.cola.platform.idp.core.resource.ClientResourceGrant;
import top.egon.cola.platform.idp.core.resource.ResourceGrantType;
import top.egon.cola.platform.idp.core.resource.ResourceServer;
import top.egon.cola.platform.idp.core.resource.ResourceServerStatus;
import top.egon.cola.platform.idp.core.resource.UserResourceAccessPolicy;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenFacadeTest {

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");
    private static final Duration ACCESS_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final String RESOURCE =
            "https://api.egon.internal/prod/platform/gateway";

    private FakeTokenSigner signer;
    private FakeRefreshTokenStore refreshTokens;
    private FakeIdentityUserStore users;
    private FakeIdentityUserStatePort states;
    private FakeSecurityEventPort events;
    private FakeOAuthClientStore clients;
    private FakeResourceServerStore resources;
    private FakeMembershipPort memberships;
    private FakeResourceDecisionPort decisions;
    private TokenFacade facade;

    @BeforeEach
    void setUp() {
        signer = new FakeTokenSigner();
        refreshTokens = new FakeRefreshTokenStore();
        users = new FakeIdentityUserStore();
        users.put(activeUser());
        states = new FakeIdentityUserStatePort();
        events = new FakeSecurityEventPort();
        clients = new FakeOAuthClientStore();
        clients.client = new OAuthClient(
                "gateway-admin-web", OAuthClient.ClientType.PUBLIC,
                OAuthClient.Status.ACTIVE, true,
                List.of("https://gateway.example.test/oauth/callback"));
        resources = new FakeResourceServerStore();
        resources.resource = new ResourceServer(
                "platform-gateway-prod", URI.create(RESOURCE), "platform",
                "gateway", "prod", "gateway-service", "gateway",
                "gateway:access", Duration.ofMinutes(5),
                ResourceServerStatus.ACTIVE, 9L);
        resources.grant = new ClientResourceGrant(
                "gateway-admin-web", "platform-gateway-prod",
                ResourceGrantType.USER_DELEGATION, null, Set.of(),
                ClientResourceGrant.Status.ACTIVE, 3L);
        memberships = new FakeMembershipPort();
        memberships.status = TenantMembershipPort.MembershipStatus.ACTIVE;
        decisions = new FakeResourceDecisionPort();
        AtomicInteger ids = new AtomicInteger();
        facade = new TokenFacade(
                signer,
                refreshTokens,
                users,
                states,
                events,
                clients,
                policy(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> "token-id-" + ids.incrementAndGet()
        );
    }

    @Test
    void accessTokenCarriesIdentityButNoAuthorizationFacts() {
        TokenFacade.TokenPair pair = issue();
        AccessTokenClaims claims = signer.accessClaims(pair.accessToken());

        assertEquals("alice-sub", claims.subject());
        assertEquals("tenant-a", claims.tenantId());
        assertEquals("sso-session-1", claims.sessionId());
        assertEquals("gateway-admin-web", claims.clientId());
        assertEquals(PrincipalType.USER, claims.principalType());
        assertEquals(4L, claims.tokenVersion());
        assertEquals(9L, claims.resourceVersion());
        assertEquals(RESOURCE, claims.audience());
        assertEquals("nonce-123", claims.nonce());
        assertEquals(NOW.plus(ACCESS_TTL), claims.expiresAt());
        assertFalse(claims.asMap().containsKey("roles"));
        assertFalse(claims.asMap().containsKey("permissions"));
        assertFalse(claims.asMap().containsKey("data_scopes"));
        assertFalse(claims.asMap().containsKey("field_policies"));
        assertFalse(claims.asMap().containsKey("scope"));
    }

    @Test
    void initialIssueCreatesDigestOnlyActiveFamily() {
        TokenFacade.TokenPair pair = issue();
        RefreshFamily family = refreshTokens.family(pair.familyId());

        assertEquals("sso-session-1", pair.sessionId());
        assertNotEquals(pair.familyId(), pair.sessionId());
        assertEquals(RefreshFamily.Status.ACTIVE, family.status());
        assertEquals("alice-sub", family.identitySub());
        assertEquals(0L, family.generation());
        assertNotEquals(pair.refreshToken(), family.currentTokenDigest());
        assertEquals(NOW.plus(REFRESH_TTL), family.expiresAt());
    }

    @Test
    void issueNormalizesJwtAndRefreshFamilyTimeToWholeSeconds() {
        Deque<String> ids = new ArrayDeque<>(List.of(
                "millisecond-family",
                "millisecond-access",
                "millisecond-refresh"
        ));
        TokenFacade millisecondFacade = new TokenFacade(
                signer,
                refreshTokens,
                users,
                states,
                events,
                clients,
                policy(),
                Clock.fixed(NOW.plusMillis(321), ZoneOffset.UTC),
                ids::removeFirst
        );

        TokenFacade.TokenPair pair = millisecondFacade.issue(
                authorizationCode(),
                ACCESS_TTL,
                REFRESH_TTL
        );

        assertEquals(
                pair.refreshExpiresAt().truncatedTo(ChronoUnit.SECONDS),
                pair.refreshExpiresAt()
        );
        assertEquals(
                pair.refreshExpiresAt(),
                refreshTokens.family(pair.familyId()).expiresAt()
        );
    }

    @Test
    void refreshRotatesTokenAndKeepsTenantSessionAndFamilyStable() {
        TokenFacade.TokenPair first = issue();

        TokenFacade.TokenPair second = facade.refresh(
                first.refreshToken(),
                "gateway-admin-web",
                RESOURCE,
                ACCESS_TTL
        );

        assertNotEquals(first.refreshToken(), second.refreshToken());
        assertEquals(first.familyId(), second.familyId());
        assertEquals(first.sessionId(), second.sessionId());
        assertEquals(1L, refreshTokens.family(first.familyId()).generation());
        AccessTokenClaims claims = signer.accessClaims(second.accessToken());
        assertEquals("tenant-a", claims.tenantId());
        assertEquals(4L, claims.tokenVersion());
    }

    @Test
    void replayOfConsumedRefreshRevokesFamilyAndBumpsTokenVersion() {
        TokenFacade.TokenPair first = issue();
        facade.refresh(
                first.refreshToken(),
                "gateway-admin-web",
                RESOURCE,
                ACCESS_TTL
        );

        assertThrows(RefreshReplayException.class, () -> facade.refresh(
                first.refreshToken(),
                "gateway-admin-web",
                RESOURCE,
                ACCESS_TTL
        ));

        assertFalse(refreshTokens.family(first.familyId()).active());
        assertEquals(5L, users.get("alice-sub").tokenVersion());
        assertEquals(5L, states.latest.tokenVersion());
        assertEquals("REFRESH_TOKEN_REPLAY", states.reason);
        assertEquals("IDENTITY_TOKEN_REVOKED", events.single().eventType());
    }

    @Test
    void wrongClientAndDisabledUserFailClosedWithoutRotation() {
        TokenFacade.TokenPair pair = issue();
        assertThrows(TokenException.class, () -> facade.refresh(
                pair.refreshToken(),
                "ddc-admin-web",
                RESOURCE,
                ACCESS_TTL
        ));
        assertEquals(0L, refreshTokens.family(pair.familyId()).generation());

        users.put(activeUser().withStatus(IdentityUserStatus.DISABLED));
        assertThrows(TokenException.class, () -> facade.refresh(
                pair.refreshToken(),
                "gateway-admin-web",
                RESOURCE,
                ACCESS_TTL
        ));
        assertEquals(0L, refreshTokens.family(pair.familyId()).generation());
    }

    @Test
    void familyLogoutRevokesRefreshWithoutChangingGlobalTokenVersion() {
        TokenFacade.TokenPair pair = issue();

        facade.revoke(pair.refreshToken(), "gateway-admin-web");

        assertFalse(refreshTokens.family(pair.familyId()).active());
        assertEquals(4L, users.get("alice-sub").tokenVersion());
        assertThrows(TokenException.class, () -> facade.refresh(
                pair.refreshToken(),
                "gateway-admin-web",
                RESOURCE,
                ACCESS_TTL
        ));
    }

    @Test
    void globalLogoutRevokesAllFamiliesAndPublishesVersion() {
        issue();

        facade.logoutAll("alice-sub");

        assertTrue(refreshTokens.subjectRevoked);
        assertEquals(5L, users.get("alice-sub").tokenVersion());
        assertEquals(5L, states.latest.tokenVersion());
        assertEquals("GLOBAL_LOGOUT", states.reason);
    }

    @Test
    void refreshRechecksResourceGrantMembershipAndRbacEntry() {
        TokenFacade.TokenPair resourceToken = issue();
        resources.resource = resource(ResourceServerStatus.DISABLED);
        assertInvalidGrant(resourceToken);

        resources.resource = resource(ResourceServerStatus.ACTIVE);
        resourceToken = issue();
        resources.grant = null;
        assertInvalidGrant(resourceToken);

        resources.grant = userGrant();
        resourceToken = issue();
        memberships.status = TenantMembershipPort.MembershipStatus.DISABLED;
        assertInvalidGrant(resourceToken);

        memberships.status = TenantMembershipPort.MembershipStatus.ACTIVE;
        resourceToken = issue();
        decisions.decision = UserResourceAccessAuthorizationPort.Decision.DENY;
        assertInvalidGrant(resourceToken);
    }

    @Test
    void refreshRejectsDifferentResourceAndReportsRbacUnavailable() {
        TokenFacade.TokenPair pair = issue();
        TokenFacade.TokenPair mismatched = pair;
        assertThrows(TokenException.class, () -> facade.refresh(
                mismatched.refreshToken(), "gateway-admin-web",
                "https://api.egon.internal/prod/platform/ddc", ACCESS_TTL));

        pair = issue();
        decisions.unavailable = true;
        TokenFacade.TokenPair current = pair;
        TokenException unavailable = assertThrows(TokenException.class,
                () -> facade.refresh(current.refreshToken(),
                        "gateway-admin-web", RESOURCE, ACCESS_TTL));
        assertEquals("temporarily_unavailable", unavailable.oauthError());
    }

    private TokenFacade.TokenPair issue() {
        return facade.issue(
                authorizationCode(),
                ACCESS_TTL,
                REFRESH_TTL
        );
    }

    private AuthorizationCode authorizationCode() {
        return new AuthorizationCode(
                "alice-sub",
                "tenant-a",
                "tenant-user-a",
                "sso-session-1",
                "gateway-admin-web",
                URI.create(RESOURCE),
                "platform-gateway-prod",
                9L,
                "https://gateway.example.test/oauth/callback",
                "nonce-123",
                "challenge-123",
                NOW.minusSeconds(10),
                NOW.plusSeconds(50)
        );
    }

    private UserResourceAccessPolicy policy() {
        return new UserResourceAccessPolicy(resources, memberships, decisions);
    }

    private ResourceServer resource(ResourceServerStatus status) {
        return new ResourceServer(
                "platform-gateway-prod", URI.create(RESOURCE), "platform",
                "gateway", "prod", "gateway-service", "gateway",
                "gateway:access", Duration.ofMinutes(5), status, 9L);
    }

    private ClientResourceGrant userGrant() {
        return new ClientResourceGrant(
                "gateway-admin-web", "platform-gateway-prod",
                ResourceGrantType.USER_DELEGATION, null, Set.of(),
                ClientResourceGrant.Status.ACTIVE, 3L);
    }

    private void assertInvalidGrant(TokenFacade.TokenPair pair) {
        TokenException exception = assertThrows(TokenException.class,
                () -> facade.refresh(pair.refreshToken(),
                        "gateway-admin-web", RESOURCE, ACCESS_TTL));
        assertEquals("invalid_grant", exception.oauthError());
    }

    private IdentityUser activeUser() {
        return new IdentityUser(
                "alice-sub",
                "alice",
                "alice",
                "Alice",
                IdentityUserStatus.ACTIVE,
                4L,
                0,
                null,
                null,
                0L
        );
    }

    private static final class FakeTokenSigner implements TokenSigner {
        private final Map<String, AccessTokenClaims> access = new HashMap<>();
        private final Map<String, RefreshTokenClaims> refresh = new HashMap<>();

        @Override
        public String signAccess(AccessTokenClaims claims) {
            String token = "access:" + claims.tokenId();
            access.put(token, claims);
            return token;
        }

        @Override
        public String signServiceAccess(ServiceAccessTokenClaims claims) {
            throw new UnsupportedOperationException(
                    "SERVICE token signing is not used by TokenFacadeTest"
            );
        }

        @Override
        public String signRefresh(RefreshTokenClaims claims) {
            String token = "refresh:" + claims.tokenId();
            refresh.put(token, claims);
            return token;
        }

        @Override
        public RefreshTokenClaims verifyRefresh(String rawRefreshToken) {
            RefreshTokenClaims claims = refresh.get(rawRefreshToken);
            if (claims == null) {
                throw new TokenException("invalid_grant");
            }
            return claims;
        }

        AccessTokenClaims accessClaims(String token) {
            return access.get(token);
        }
    }

    private static final class FakeRefreshTokenStore
            implements RefreshTokenStore {
        private final Map<String, RefreshFamily> families = new HashMap<>();
        private final Map<String, String> usedDigests = new HashMap<>();
        private boolean subjectRevoked;

        @Override
        public void create(RefreshFamily family) {
            families.put(family.familyId(), family);
        }

        @Override
        public RotationResult rotate(RotationCommand command) {
            RefreshFamily family = families.get(command.familyId());
            if (family == null) {
                return new RotationResult(RotationOutcome.MISSING, null);
            }
            if (!family.active()) {
                return new RotationResult(RotationOutcome.REVOKED, family);
            }
            if (usedDigests.containsKey(command.currentTokenDigest())) {
                RefreshFamily compromised = family.compromised(command.now());
                families.put(family.familyId(), compromised);
                return new RotationResult(
                        RotationOutcome.REPLAY,
                        compromised
                );
            }
            if (!family.currentTokenDigest().equals(
                    command.currentTokenDigest()
            )) {
                return new RotationResult(RotationOutcome.MISSING, family);
            }
            usedDigests.put(
                    command.currentTokenDigest(),
                    family.familyId()
            );
            RefreshFamily rotated = family.rotated(
                    command.successorTokenDigest(),
                    command.successorGeneration(),
                    command.now()
            );
            families.put(family.familyId(), rotated);
            return new RotationResult(RotationOutcome.ROTATED, rotated);
        }

        @Override
        public void revokeFamily(
                String familyId,
                String reason,
                Instant now
        ) {
            RefreshFamily family = families.get(familyId);
            if (family != null) {
                families.put(familyId, family.revoked(now));
            }
        }

        @Override
        public void revokeSubject(
                String identitySub,
                String reason,
                Instant now
        ) {
            subjectRevoked = true;
            new ArrayList<>(families.values()).stream()
                    .filter(value -> value.identitySub().equals(identitySub))
                    .forEach(value -> families.put(
                            value.familyId(),
                            value.revoked(now)
                    ));
        }

        RefreshFamily family(String familyId) {
            return families.get(familyId);
        }
    }

    private static final class FakeIdentityUserStore
            implements IdentityUserStore {
        private final Map<String, IdentityUser> values = new HashMap<>();

        @Override
        public Optional<IdentityUser> findByNormalizedUsername(
                String normalizedUsername
        ) {
            return values.values().stream()
                    .filter(value -> value.normalizedUsername().equals(
                            normalizedUsername
                    ))
                    .findFirst();
        }

        @Override
        public Optional<IdentityUser> findById(String identitySub) {
            return Optional.ofNullable(values.get(identitySub));
        }

        @Override
        public IdentityUser save(IdentityUser user, long expectedVersion) {
            IdentityUser current = values.get(user.id());
            if (current == null || current.version() != expectedVersion) {
                throw new IllegalStateException("optimistic lock failed");
            }
            values.put(user.id(), user);
            return user;
        }

        void put(IdentityUser user) {
            values.put(user.id(), user);
        }

        IdentityUser get(String identitySub) {
            return values.get(identitySub);
        }
    }

    private static final class FakeIdentityUserStatePort
            implements IdentityUserStatePort {
        private IdentityUserState latest;
        private String reason;

        @Override
        public void publish(IdentityUserState state) {
            latest = state;
        }

        @Override
        public void revokeFamilies(
                String identitySub,
                long tokenVersion,
                String revokeReason
        ) {
            reason = revokeReason;
        }
    }

    private static final class FakeSecurityEventPort
            implements IdentitySecurityEventPort {
        private final List<IdentitySecurityEvent> values = new ArrayList<>();

        @Override
        public void append(IdentitySecurityEvent event) {
            values.add(event);
        }

        IdentitySecurityEvent single() {
            return values.getFirst();
        }
    }

    private static final class FakeOAuthClientStore implements OAuthClientStore {
        private OAuthClient client;

        @Override
        public Optional<OAuthClient> findById(String clientId) {
            return client != null && client.clientId().equals(clientId)
                    ? Optional.of(client) : Optional.empty();
        }
    }

    private static final class FakeResourceServerStore implements ResourceServerStore {
        private ResourceServer resource;
        private ClientResourceGrant grant;

        @Override
        public Optional<ResourceServer> findById(String resourceServerId) {
            return resource != null && resource.resourceServerId().equals(resourceServerId)
                    ? Optional.of(resource) : Optional.empty();
        }

        @Override
        public Optional<ResourceServer> findByUri(URI resourceUri) {
            return resource != null && resource.resourceUri().equals(resourceUri)
                    ? Optional.of(resource) : Optional.empty();
        }

        @Override
        public Optional<ResourceServer> findByScope(
                String bizCode, String appCode, String environment) {
            return resource != null && resource.matches(bizCode, appCode, environment)
                    ? Optional.of(resource) : Optional.empty();
        }

        @Override
        public Optional<ResourceServer> findByManagementClientId(String clientId) {
            return resource != null && resource.managementClientId().equals(clientId)
                    ? Optional.of(resource) : Optional.empty();
        }

        @Override
        public Optional<ClientResourceGrant> findGrant(
                String clientId, String resourceServerId,
                ResourceGrantType grantType, String tenantId) {
            return grant != null
                    && grant.clientId().equals(clientId)
                    && grant.resourceServerId().equals(resourceServerId)
                    && grant.grantType() == grantType
                    ? Optional.of(grant) : Optional.empty();
        }
    }

    private static final class FakeMembershipPort implements TenantMembershipPort {
        private MembershipStatus status;

        @Override
        public TenantMembership resolve(String identitySub, String tenantId, String clientId) {
            return new TenantMembership(identitySub, tenantId, "tenant-user-a",
                    "Tenant A", status);
        }

        @Override
        public List<TenantMembership> list(String identitySub, String clientId) {
            return List.of(resolve(identitySub, "tenant-a", clientId));
        }
    }

    private static final class FakeResourceDecisionPort
            implements UserResourceAccessAuthorizationPort {
        private Decision decision = Decision.ALLOW;
        private boolean unavailable;

        @Override
        public AccessDecision decide(AccessRequest request) {
            if (unavailable) {
                throw new AccessUnavailableException("RBAC3 unavailable");
            }
            return new AccessDecision(decision,
                    decision == Decision.ALLOW ? "ALLOW" : "ENTRY_PERMISSION_DENIED",
                    43L, 2L, 18L);
        }
    }
}
