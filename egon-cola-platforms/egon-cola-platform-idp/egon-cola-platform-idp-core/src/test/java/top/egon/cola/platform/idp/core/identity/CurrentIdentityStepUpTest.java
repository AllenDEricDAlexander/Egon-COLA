package top.egon.cola.platform.idp.core.identity;

import org.junit.jupiter.api.Test;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentIdentityStepUpTest {

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");

    @Test
    void stepUpUsesOnlyCurrentSubjectAndPassword() {
        UserStore users = new UserStore();
        CredentialStore credentials = new CredentialStore();
        Hash hash = new Hash();
        users.put(new IdentityUser(
                "alice-sub", "alice", "alice", "Alice",
                IdentityUserStatus.ACTIVE, 0, null, null, 0));
        credentials.put(new PasswordCredential(
                "alice-sub", hash.encoded("password-1"), NOW,
                false, PasswordCredential.Status.ACTIVE, 0));
        IdentityFacade facade = new IdentityFacade(
                users, credentials, hash, new State(), new Events(),
                new Refresh(), new UsernameNormalizer(), 3, Duration.ofMinutes(15));

        AuthenticatedIdentity identity = facade.authenticateCurrent(
                "alice-sub", "password-1".toCharArray(), NOW);

        assertEquals("alice-sub", identity.identitySub());
        assertThrows(IdentityException.class, () -> facade.authenticateCurrent(
                "missing", "password-1".toCharArray(), NOW));
    }

    private static final class UserStore implements IdentityUserStore {
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
    }

    private static final class CredentialStore implements PasswordCredentialStore {
        private final Map<String, PasswordCredential> values = new HashMap<>();

        @Override
        public Optional<PasswordCredential> findActive(String identitySub) {
            return Optional.ofNullable(values.get(identitySub));
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

    private static final class Hash implements PasswordHashPort {
        @Override
        public boolean matches(char[] rawPassword, String encodedPassword) {
            return encoded(rawPassword).equals(encodedPassword);
        }

        @Override
        public String encode(char[] rawPassword) {
            return encoded(rawPassword);
        }

        @Override
        public String dummyHash() {
            return "hash:dummy";
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

    private static final class State implements IdentityUserStatePort {
        @Override
        public void publish(IdentityUserState state) {
        }
    }

    private static final class Events implements IdentitySecurityEventPort {
        @Override
        public void append(IdentitySecurityEvent event) {
        }
    }

    private static final class Refresh implements RefreshTokenStore {
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
        }

        @Override
        public void expire(Instant now) {
        }
    }
}
