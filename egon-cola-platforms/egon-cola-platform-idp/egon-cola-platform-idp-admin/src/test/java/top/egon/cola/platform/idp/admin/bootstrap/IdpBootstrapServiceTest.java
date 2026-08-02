package top.egon.cola.platform.idp.admin.bootstrap;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.idp.core.identity.IdentityUser;
import top.egon.cola.platform.idp.core.identity.IdentityUserStatus;
import top.egon.cola.platform.idp.core.identity.PasswordCredential;
import top.egon.cola.platform.idp.core.identity.UsernameNormalizer;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.PasswordCredentialStore;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdpBootstrapServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");

    @Test
    void createsOneActiveIdentityWithAnEncodedEnvironmentPassword() {
        InMemoryStores stores = new InMemoryStores();
        RecordingPasswordHash hashes = new RecordingPasswordHash();
        LongIdGenerator ids = () -> 42L;
        IdpBootstrapService service = new IdpBootstrapService(
                stores,
                stores,
                hashes,
                ids,
                new UsernameNormalizer(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        char[] password = "strong-password-1".toCharArray();

        service.bootstrap(" Ａlice ", password);

        assertEquals("42", stores.user.id());
        assertEquals("alice", stores.user.normalizedUsername());
        assertEquals(IdentityUserStatus.ACTIVE, stores.user.status());
        assertEquals("{argon2}encoded", stores.credential.passwordHash());
        assertEquals(NOW, stores.credential.passwordChangedAt());
        assertArrayEquals(new char[password.length], password);
    }

    @Test
    void refusesToReplaceAnExistingBootstrapIdentity() {
        InMemoryStores stores = new InMemoryStores();
        stores.user = new IdentityUser(
                "existing",
                "alice",
                "alice",
                "alice",
                IdentityUserStatus.ACTIVE,
                0L,
                0,
                null,
                null,
                0L
        );
        IdpBootstrapService service = new IdpBootstrapService(
                stores,
                stores,
                new RecordingPasswordHash(),
                () -> 42L,
                new UsernameNormalizer(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        char[] password = "strong-password-1".toCharArray();

        assertThrows(
                IllegalStateException.class,
                () -> service.bootstrap("alice", password)
        );
        assertArrayEquals(new char[password.length], password);
        assertEquals("existing", stores.user.id());
    }

    private static final class InMemoryStores
            implements IdentityUserStore, PasswordCredentialStore {
        private IdentityUser user;
        private PasswordCredential credential;

        @Override
        public Optional<IdentityUser> findByNormalizedUsername(String value) {
            return user != null && user.normalizedUsername().equals(value)
                    ? Optional.of(user)
                    : Optional.empty();
        }

        @Override
        public Optional<IdentityUser> findById(String identitySub) {
            return user != null && user.id().equals(identitySub)
                    ? Optional.of(user)
                    : Optional.empty();
        }

        @Override
        public IdentityUser save(IdentityUser value, long expectedVersion) {
            user = value;
            return value;
        }

        @Override
        public Optional<PasswordCredential> findActive(String identitySub) {
            return Optional.ofNullable(credential);
        }

        @Override
        public PasswordCredential save(
                PasswordCredential value,
                long expectedVersion
        ) {
            credential = value;
            return value;
        }
    }

    private static final class RecordingPasswordHash
            implements PasswordHashPort {

        @Override
        public boolean matches(char[] rawPassword, String encodedPassword) {
            return false;
        }

        @Override
        public String encode(char[] rawPassword) {
            return "{argon2}encoded";
        }

        @Override
        public String dummyHash() {
            return "{argon2}dummy";
        }

        @Override
        public boolean needsUpgrade(String encodedPassword) {
            return false;
        }
    }
}
