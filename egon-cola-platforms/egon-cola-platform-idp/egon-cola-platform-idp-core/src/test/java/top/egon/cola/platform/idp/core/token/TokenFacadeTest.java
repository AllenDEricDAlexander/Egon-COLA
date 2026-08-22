package top.egon.cola.platform.idp.core.token;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.contract.PrincipalType;
import top.egon.cola.platform.idp.core.identity.AuthenticatedIdentity;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;
import top.egon.cola.platform.idp.core.port.TenantMembershipPort;
import top.egon.cola.platform.idp.core.port.TokenSigner;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenFacadeTest {

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");

    private FakeSigner signer;
    private FakeRefreshStore refreshStore;
    private FakeUserStore users;
    private FakeMembership memberships;
    private TokenFacade facade;

    @BeforeEach
    void setUp() {
        signer = new FakeSigner();
        refreshStore = new FakeRefreshStore();
        users = new FakeUserStore();
        users.put(activeUser());
        memberships = new FakeMembership();
        AtomicInteger ids = new AtomicInteger();
        facade = new TokenFacade(
                signer, refreshStore, users, memberships,
                Clock.fixed(NOW, ZoneOffset.UTC),
                () -> "id-" + ids.incrementAndGet(), "platform");
    }

    @Test
    void issueUsesFiveMinuteUserAccessTokenAndNoAuthorizationFacts() {
        UserTokenPair pair = issue();
        AccessTokenClaims claims = signer.access.get(pair.accessToken());

        assertEquals(PrincipalType.USER, claims.principalType());
        assertEquals("alice-sub", claims.subject());
        assertEquals("tenant-a", claims.tenantId());
        assertEquals("platform", claims.audience());
        assertEquals(NOW.plus(Duration.ofMinutes(5)), claims.expiresAt());
        assertEquals("PASSWORD", claims.authenticationContext().acr());
        assertFalse(claims.asMap().containsKey("sid"));
        assertFalse(claims.asMap().containsKey("client_id"));
        assertFalse(claims.asMap().containsKey("token_version"));
        assertFalse(claims.asMap().containsKey("roles"));
        assertFalse(claims.asMap().containsKey("permissions"));
    }

    @Test
    void issuePersistsOnlyRefreshDigestAndAbsoluteExpiration() {
        UserTokenPair pair = issue();
        RefreshTokenRecord record = refreshStore.records.values().stream()
                .findFirst().orElseThrow();

        assertNotEquals(pair.refreshToken(), record.tokenDigest());
        assertEquals("alice-sub", record.identitySub());
        assertEquals("tenant-a", record.tenantId());
        assertEquals(pair.refreshExpiresAt(), record.expiresAt());
        assertEquals(RefreshTokenRecord.Status.ACTIVE, record.status());
    }

    @Test
    void refreshReturnsSameRefreshTokenAndRechecksMembership() {
        UserTokenPair first = issue();
        UserTokenPair second = facade.refresh(first.refreshToken());

        assertNotEquals(first.accessToken(), second.accessToken());
        assertEquals(first.refreshToken(), second.refreshToken());
        assertEquals(first.refreshExpiresAt(), second.refreshExpiresAt());

        memberships.status = TenantMembershipPort.MembershipStatus.DISABLED;
        assertThrows(TokenException.class, () -> facade.refresh(first.refreshToken()));
    }

    @Test
    void validateRefreshReturnsOnlyCurrentOnlineIdentityAndExpiry() {
        UserTokenPair pair = issue();

        RefreshTokenStatus status = facade.validateRefresh(pair.refreshToken());

        assertEquals("alice-sub", status.subject());
        assertEquals("tenant-a", status.tenantId());
        assertEquals(pair.refreshExpiresAt(), status.expiresAt());
    }

    @Test
    void validateRefreshRejectsRevokedRefreshToken() {
        UserTokenPair pair = issue();
        facade.revoke(pair.refreshToken());

        assertThrows(TokenException.class,
                () -> facade.validateRefresh(pair.refreshToken()));
    }

    @Test
    void revokeAndSubjectRevokeInvalidateRefreshRecords() {
        UserTokenPair first = issue();
        facade.revoke(first.refreshToken());
        assertTrue(refreshStore.revokedDigests.contains(
                refreshStore.digest(first.refreshToken())));

        UserTokenPair second = issue();
        facade.revokeSubject("alice-sub");
        assertTrue(refreshStore.subjectRevoked);
        assertTrue(refreshStore.revokedDigests.contains(
                refreshStore.digest(second.refreshToken())));
    }

    @Test
    void invalidUserAndTenantMembershipFailClosed() {
        users.put(activeUser().withStatus(IdentityUserStatus.DISABLED));
        assertThrows(TokenException.class, this::issue);
        users.put(activeUser());
        memberships.status = TenantMembershipPort.MembershipStatus.DISABLED;
        assertThrows(TokenException.class, this::issue);
    }

    private UserTokenPair issue() {
        return facade.issue(
                new AuthenticatedIdentity("alice-sub", "alice", "Alice", false),
                "tenant-a", Duration.ofDays(7));
    }

    private static IdentityUser activeUser() {
        return new IdentityUser(
                "alice-sub", "alice", "alice", "Alice",
                IdentityUserStatus.ACTIVE, 0, null, null, 0);
    }

    private static final class FakeSigner implements TokenSigner {
        private final Map<String, AccessTokenClaims> access = new HashMap<>();
        private final Map<String, RefreshTokenClaims> refresh = new HashMap<>();
        private int refreshSequence;

        @Override
        public String signAccess(AccessTokenClaims claims) {
            String token = "at-" + claims.tokenId();
            access.put(token, claims);
            return token;
        }

        @Override
        public String signServiceAccess(ServiceAccessTokenClaims claims) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String signRefresh(RefreshTokenClaims claims) {
            String token = "rt-" + (++refreshSequence);
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
    }

    private static final class FakeRefreshStore implements RefreshTokenStore {
        private final Map<String, RefreshTokenRecord> records = new HashMap<>();
        private final java.util.Set<String> revokedDigests = new java.util.HashSet<>();
        private boolean subjectRevoked;

        @Override
        public void create(RefreshTokenRecord record) {
            records.put(record.tokenDigest(), record);
        }

        @Override
        public Optional<RefreshTokenRecord> findValid(String tokenDigest, Instant now) {
            return Optional.ofNullable(records.get(tokenDigest))
                    .filter(record -> record.status() == RefreshTokenRecord.Status.ACTIVE)
                    .filter(record -> record.expiresAt().isAfter(now));
        }

        @Override
        public void revokeToken(String tokenDigest, String reason, Instant now) {
            revokedDigests.add(tokenDigest);
            RefreshTokenRecord record = records.get(tokenDigest);
            if (record != null) {
                records.put(tokenDigest, new RefreshTokenRecord(
                        record.tokenDigest(), record.identitySub(), record.tenantId(),
                        record.issuedAt(), record.expiresAt(), RefreshTokenRecord.Status.REVOKED));
            }
        }

        @Override
        public void revokeSubject(String identitySub, String reason, Instant now) {
            subjectRevoked = true;
            records.values().stream()
                    .filter(record -> record.identitySub().equals(identitySub))
                    .forEach(record -> revokedDigests.add(record.tokenDigest()));
        }

        @Override
        public void expire(Instant now) {
        }

        String digest(String raw) {
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                    sha256(raw));
        }

        private byte[] sha256(String raw) {
            try {
                return java.security.MessageDigest.getInstance("SHA-256")
                        .digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (java.security.NoSuchAlgorithmException exception) {
                throw new AssertionError(exception);
            }
        }
    }

    private static final class FakeUserStore implements IdentityUserStore {
        private final Map<String, IdentityUser> values = new HashMap<>();

        @Override
        public Optional<IdentityUser> findByNormalizedUsername(String normalizedUsername) {
            return values.values().stream()
                    .filter(user -> user.normalizedUsername().equals(normalizedUsername))
                    .findFirst();
        }

        @Override
        public Optional<IdentityUser> findById(String identitySub) {
            return Optional.ofNullable(values.get(identitySub));
        }

        @Override
        public IdentityUser save(IdentityUser user, long expectedVersion) {
            values.put(user.id(), user);
            return user;
        }

        void put(IdentityUser user) {
            values.put(user.id(), user);
        }
    }

    private static final class FakeMembership implements TenantMembershipPort {
        private MembershipStatus status = MembershipStatus.ACTIVE;

        @Override
        public TenantMembership resolve(String identitySub, String tenantId) {
            return new TenantMembership(
                    identitySub, tenantId, "Tenant A", status);
        }

        @Override
        public List<TenantMembership> list(String identitySub) {
            return List.of(resolve(identitySub, "tenant-a"));
        }
    }
}
