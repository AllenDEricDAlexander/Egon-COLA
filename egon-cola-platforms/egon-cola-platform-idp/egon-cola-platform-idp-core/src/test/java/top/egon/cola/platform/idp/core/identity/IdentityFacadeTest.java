package top.egon.cola.platform.idp.core.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.idp.contract.IdpErrorCode;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEvent;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.idp.core.port.IdentityUserStatePort;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.PasswordCredentialStore;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;
import top.egon.cola.platform.idp.core.token.RefreshTokenRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityFacadeTest {

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");

    private FakeUserStore users;
    private FakeCredentialStore credentials;
    private FakePasswordHash hashes;
    private FakeState state;
    private FakeEvents events;
    private FakeRefreshStore refresh;
    private IdentityFacade facade;

    @BeforeEach
    void setUp() {
        users = new FakeUserStore();
        credentials = new FakeCredentialStore();
        hashes = new FakePasswordHash();
        state = new FakeState();
        events = new FakeEvents();
        refresh = new FakeRefreshStore();
        facade = new IdentityFacade(
                users, credentials, hashes, state, events, refresh,
                new UsernameNormalizer(), 3, Duration.ofMinutes(15));
        users.put(activeUser());
        credentials.put(new PasswordCredential(
                "alice-sub", hashes.encoded("old-password-1"), NOW.minusSeconds(1),
                false, PasswordCredential.Status.ACTIVE, 0));
    }

    @Test
    void unknownAndWrongPasswordHaveSamePublicFailure() {
        IdpErrorCode missing = publicCode(() -> facade.authenticate(
                "missing", chars("wrong"), "local", NOW));
        IdpErrorCode wrong = publicCode(() -> facade.authenticate(
                "alice", chars("wrong"), "local", NOW));

        assertEquals(IdpErrorCode.INVALID_CREDENTIALS, missing);
        assertEquals(missing, wrong);
        assertEquals(2, hashes.matchCalls);
    }

    @Test
    void successfulAuthenticationDoesNotExposeTokenVersion() {
        AuthenticatedIdentity result = facade.authenticate(
                " ＡLICE ", chars("old-password-1"), "local", NOW);

        assertEquals("alice-sub", result.identitySub());
        assertEquals(false, result.mustChangePassword());
        assertEquals(0, users.get("alice-sub").failedLoginCount());
        assertEquals(NOW, users.get("alice-sub").lastLoginAt());
        assertEquals("alice-sub", state.latest.subject());
        assertEquals(IdentityUserState.Status.ACTIVE, state.latest.status());
    }

    @Test
    void passwordChangeRevokesAllStableRefreshTokens() {
        char[] oldPassword = chars("old-password-1");
        char[] newPassword = chars("new-password-2");

        facade.changePassword("alice-sub", oldPassword, newPassword, NOW);

        assertTrue(refresh.subjectRevoked);
        assertEquals("IDENTITY_TOKEN_REVOKED", events.values.getFirst().eventType());
        assertEquals("PASSWORD_CHANGED", events.values.getFirst().reason());
        assertArrayEquals(new char[oldPassword.length], oldPassword);
        assertArrayEquals(new char[newPassword.length], newPassword);
        assertThrows(IdentityException.class, () -> facade.authenticate(
                "alice", chars("old-password-1"), "local", NOW));
    }

    private static IdentityUser activeUser() {
        return new IdentityUser(
                "alice-sub", "Alice", "alice", "Alice",
                IdentityUserStatus.ACTIVE, 0, null, null, 0);
    }

    private IdpErrorCode publicCode(Runnable action) {
        return assertThrows(IdentityException.class, action::run).code();
    }

    private char[] chars(String value) {
        return value.toCharArray();
    }

    private static final class FakeUserStore implements IdentityUserStore {
        private final Map<String, IdentityUser> values = new HashMap<>();

        @Override
        public Optional<IdentityUser> findByNormalizedUsername(String value) {
            return values.values().stream()
                    .filter(user -> user.normalizedUsername().equals(value)).findFirst();
        }

        @Override
        public Optional<IdentityUser> findById(String value) {
            return Optional.ofNullable(values.get(value));
        }

        @Override
        public IdentityUser save(IdentityUser user, long expectedVersion) {
            values.put(user.id(), user);
            return user;
        }

        void put(IdentityUser user) {
            values.put(user.id(), user);
        }

        IdentityUser get(String id) {
            return values.get(id);
        }
    }

    private static final class FakeCredentialStore implements PasswordCredentialStore {
        private final Map<String, PasswordCredential> values = new HashMap<>();

        @Override
        public Optional<PasswordCredential> findActive(String identitySub) {
            return Optional.ofNullable(values.get(identitySub))
                    .filter(value -> value.status() == PasswordCredential.Status.ACTIVE);
        }

        @Override
        public PasswordCredential save(PasswordCredential credential, long expectedVersion) {
            values.put(credential.identitySub(), credential);
            return credential;
        }

        void put(PasswordCredential credential) {
            values.put(credential.identitySub(), credential);
        }
    }

    private static final class FakePasswordHash implements PasswordHashPort {
        private int matchCalls;

        @Override
        public boolean matches(char[] rawPassword, String encodedPassword) {
            matchCalls++;
            return encoded(rawPassword).equals(encodedPassword);
        }

        @Override
        public String encode(char[] rawPassword) {
            return encoded(rawPassword);
        }

        @Override
        public String dummyHash() {
            return encoded("dummy-password");
        }

        @Override
        public boolean needsUpgrade(String encodedPassword) {
            return false;
        }

        String encoded(String value) {
            return "hash:" + value;
        }

        private String encoded(char[] value) {
            return encoded(new String(value));
        }
    }

    private static final class FakeState implements IdentityUserStatePort {
        private IdentityUserState latest;

        @Override
        public void publish(IdentityUserState value) {
            latest = value;
        }
    }

    private static final class FakeEvents implements IdentitySecurityEventPort {
        private final List<IdentitySecurityEvent> values = new ArrayList<>();

        @Override
        public void append(IdentitySecurityEvent event) {
            values.add(event);
        }
    }

    private static final class FakeRefreshStore implements RefreshTokenStore {
        private boolean subjectRevoked;

        @Override
        public void create(RefreshTokenRecord record) {
        }

        @Override
        public Optional<RefreshTokenRecord> findValid(String tokenDigest, Instant now) {
            return Optional.empty();
        }

        @Override
        public void revokeToken(String tokenDigest, String reason, Instant now) {
        }

        @Override
        public void revokeSubject(String identitySub, String reason, Instant now) {
            subjectRevoked = true;
        }

        @Override
        public void expire(Instant now) {
        }
    }
}
