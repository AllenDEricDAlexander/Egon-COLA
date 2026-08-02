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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentityFacadeTest {

    @Test
    void readsCurrentLoginPolicyForEachAuthenticationAttempt() {
        AtomicInteger maximumFailures = new AtomicInteger(3);
        AtomicReference<Duration> lockDuration = new AtomicReference<>(
                Duration.ofMinutes(5)
        );
        FakeUserStore currentUsers = new FakeUserStore();
        FakeCredentialStore currentCredentials = new FakeCredentialStore();
        FakePasswordHashPort currentHashes = new FakePasswordHashPort();
        IdentityUser user = activeUser();
        currentUsers.put(user);
        currentCredentials.put(new PasswordCredential(
                user.id(),
                currentHashes.encoded("old-password-1"),
                NOW.minus(Duration.ofDays(1)),
                false,
                PasswordCredential.Status.ACTIVE,
                0L
        ));
        IdentityFacade facade = IdentityFacade.dynamicPolicy(
                currentUsers,
                currentCredentials,
                currentHashes,
                new FakeUserStatePort(),
                new FakeSecurityEventPort(),
                new UsernameNormalizer(),
                maximumFailures::get,
                lockDuration::get
        );

        maximumFailures.set(1);
        lockDuration.set(Duration.ofMinutes(17));

        assertThrows(IdentityException.class, () -> facade.authenticate(
                "alice",
                "wrong-password".toCharArray(),
                "127.0.0.1",
                NOW
        ));
        IdentityUser locked = currentUsers.get(user.id());
        assertEquals(IdentityUserStatus.LOCKED, locked.status());
        assertEquals(NOW.plus(Duration.ofMinutes(17)), locked.lockedUntil());
    }

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");

    private FakeUserStore users;
    private FakeCredentialStore credentials;
    private FakePasswordHashPort hashes;
    private FakeUserStatePort states;
    private FakeSecurityEventPort events;
    private IdentityFacade facade;

    @BeforeEach
    void setUp() {
        users = new FakeUserStore();
        credentials = new FakeCredentialStore();
        hashes = new FakePasswordHashPort();
        states = new FakeUserStatePort();
        events = new FakeSecurityEventPort();
        facade = new IdentityFacade(
                users,
                credentials,
                hashes,
                states,
                events,
                new UsernameNormalizer(),
                3,
                Duration.ofMinutes(15)
        );
        users.put(activeUser());
        credentials.put(new PasswordCredential(
                "alice-sub",
                hashes.encoded("old-password-1"),
                NOW.minus(Duration.ofDays(1)),
                false,
                PasswordCredential.Status.ACTIVE,
                0L
        ));
    }

    @Test
    void unknownUserAndWrongPasswordHaveSamePublicFailure() {
        IdpErrorCode missing = publicCode(() -> facade.authenticate(
                "missing",
                chars("wrong"),
                "local",
                NOW
        ));
        IdpErrorCode wrong = publicCode(() -> facade.authenticate(
                "alice",
                chars("wrong"),
                "local",
                NOW
        ));

        assertEquals(IdpErrorCode.INVALID_CREDENTIALS, missing);
        assertEquals(missing, wrong);
        assertEquals(2, hashes.matchCalls);
    }

    @Test
    void locksAccountAtConfiguredFailureThreshold() {
        for (int attempt = 0; attempt < 3; attempt++) {
            assertThrows(IdentityException.class, () -> facade.authenticate(
                    "alice",
                    chars("wrong"),
                    "local",
                    NOW
            ));
        }

        IdentityUser locked = users.get("alice-sub");
        assertEquals(IdentityUserStatus.LOCKED, locked.status());
        assertEquals(NOW.plus(Duration.ofMinutes(15)), locked.lockedUntil());
        IdentityException exception = assertThrows(
                IdentityException.class,
                () -> facade.authenticate(
                        "alice",
                        chars("old-password-1"),
                        "local",
                        NOW.plusSeconds(1)
                )
        );
        assertEquals(IdpErrorCode.USER_LOCKED, exception.code());
    }

    @Test
    void successfulAuthenticationClearsFailuresAndPasswordInput() {
        users.put(activeUser().withLoginFailure(
                1,
                null,
                1L
        ));
        char[] password = chars("old-password-1");

        AuthenticatedIdentity result = facade.authenticate(
                " ＡLICE ",
                password,
                "local",
                NOW
        );

        assertEquals("alice-sub", result.identitySub());
        assertEquals(0, users.get("alice-sub").failedLoginCount());
        assertEquals(NOW, users.get("alice-sub").lastLoginAt());
        assertEquals("alice-sub", states.latest.subject());
        assertEquals(IdentityUserState.Status.ACTIVE, states.latest.status());
        assertEquals(4L, states.latest.tokenVersion());
        assertArrayEquals(new char[password.length], password);
    }

    @Test
    void passwordChangeIncrementsTokenVersionAndPublishesRevocation() {
        char[] oldPassword = chars("old-password-1");
        char[] newPassword = chars("new-password-2");

        facade.changePassword(
                "alice-sub",
                oldPassword,
                newPassword,
                NOW
        );

        assertEquals(5L, users.get("alice-sub").tokenVersion());
        assertEquals(IdentityUserState.Status.ACTIVE,
                states.latest.status());
        assertEquals(5L, states.latest.tokenVersion());
        assertEquals("IDENTITY_TOKEN_REVOKED", events.single().eventType());
        assertEquals("PASSWORD_CHANGED", events.single().reason());
        assertFalse(hashes.matches(
                chars("old-password-1"),
                credentials.get("alice-sub").passwordHash()
        ));
        assertArrayEquals(new char[oldPassword.length], oldPassword);
        assertArrayEquals(new char[newPassword.length], newPassword);
    }

    private IdentityUser activeUser() {
        return new IdentityUser(
                "alice-sub",
                "Alice",
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

    private IdpErrorCode publicCode(Runnable action) {
        return assertThrows(IdentityException.class, action::run).code();
    }

    private char[] chars(String value) {
        return value.toCharArray();
    }

    private static final class FakeUserStore implements IdentityUserStore {
        private final Map<String, IdentityUser> byId = new HashMap<>();

        @Override
        public Optional<IdentityUser> findByNormalizedUsername(
                String normalizedUsername
        ) {
            return byId.values().stream()
                    .filter(user -> user.normalizedUsername().equals(
                            normalizedUsername
                    ))
                    .findFirst();
        }

        @Override
        public Optional<IdentityUser> findById(String identitySub) {
            return Optional.ofNullable(byId.get(identitySub));
        }

        @Override
        public IdentityUser save(IdentityUser user, long expectedVersion) {
            IdentityUser existing = byId.get(user.id());
            if (existing != null && existing.version() != expectedVersion) {
                throw new IllegalStateException("optimistic lock failed");
            }
            byId.put(user.id(), user);
            return user;
        }

        void put(IdentityUser user) {
            byId.put(user.id(), user);
        }

        IdentityUser get(String identitySub) {
            return byId.get(identitySub);
        }
    }

    private static final class FakeCredentialStore
            implements PasswordCredentialStore {
        private final Map<String, PasswordCredential> values = new HashMap<>();

        @Override
        public Optional<PasswordCredential> findActive(String identitySub) {
            return Optional.ofNullable(values.get(identitySub))
                    .filter(value -> value.status()
                            == PasswordCredential.Status.ACTIVE);
        }

        @Override
        public PasswordCredential save(
                PasswordCredential credential,
                long expectedVersion
        ) {
            PasswordCredential existing = values.get(credential.identitySub());
            if (existing != null && existing.version() != expectedVersion) {
                throw new IllegalStateException("optimistic lock failed");
            }
            values.put(credential.identitySub(), credential);
            return credential;
        }

        void put(PasswordCredential credential) {
            values.put(credential.identitySub(), credential);
        }

        PasswordCredential get(String identitySub) {
            return values.get(identitySub);
        }
    }

    private static final class FakePasswordHashPort implements PasswordHashPort {
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

    private static final class FakeUserStatePort
            implements IdentityUserStatePort {
        private IdentityUserState latest;

        @Override
        public void publish(IdentityUserState state) {
            latest = state;
        }

        @Override
        public void revokeFamilies(
                String identitySub,
                long tokenVersion,
                String reason
        ) {
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
            assertEquals(1, values.size());
            return values.getFirst();
        }
    }
}
