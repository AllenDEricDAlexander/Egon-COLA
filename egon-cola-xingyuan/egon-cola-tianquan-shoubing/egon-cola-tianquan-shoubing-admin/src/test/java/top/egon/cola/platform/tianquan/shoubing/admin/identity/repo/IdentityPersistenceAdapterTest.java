package top.egon.cola.platform.tianquan.shoubing.admin.identity.repo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repository.IdentityCredentialRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.repository.IdentityUserRepository;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUser;
import top.egon.cola.platform.tianquan.shoubing.core.identity.IdentityUserStatus;
import top.egon.cola.platform.tianquan.shoubing.core.identity.PasswordCredential;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityPersistenceAdapterTest {

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");

    @Mock
    private IdentityUserRepository users;
    @Mock
    private IdentityCredentialRepository credentials;

    private IdentityPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new IdentityPersistenceAdapter(users, credentials);
    }

    @Test
    void delegatesCorePortOperationsAndPreservesExternalIdentityKeys() {
        IdentityUser user = user(0L);
        PasswordCredential credential = credential(0L);
        when(users.findByIdentitySub("1001")).thenReturn(Optional.of(user));
        when(users.findByNormalizedUsername("alice")).thenReturn(Optional.of(user));
        when(credentials.findByIdentitySub("1001")).thenReturn(Optional.of(credential));
        when(users.save(user, 0L)).thenReturn(user);
        when(credentials.save(credential, 0L)).thenReturn(credential);

        assertEquals(user, adapter.findById(" 1001 ").orElseThrow());
        assertEquals(user, adapter.findByNormalizedUsername(" alice ").orElseThrow());
        assertEquals(credential, adapter.findActive("1001").orElseThrow());
        assertEquals(user, adapter.save(user, 0L));
        assertEquals(credential, adapter.save(credential, 0L));

        verify(users).findByIdentitySub("1001");
        verify(users).findByNormalizedUsername("alice");
        verify(users).save(user, 0L);
        verify(credentials).findByIdentitySub("1001");
        verify(credentials).save(credential, 0L);
    }

    @Test
    void hidesRevokedCredentialAndPreservesOptimisticFailure() {
        PasswordCredential revoked = new PasswordCredential(
                "1001", "{argon2}encoded", NOW, false,
                PasswordCredential.Status.REVOKED, 1L
        );
        when(credentials.findByIdentitySub("1001")).thenReturn(Optional.of(revoked));
        assertTrue(adapter.findActive("1001").isEmpty());

        IdentityUser stale = user(2L);
        when(users.save(stale, 0L)).thenThrow(
                new OptimisticLockingFailureException("identity 1001 version conflict")
        );
        OptimisticLockingFailureException failure = assertThrows(
                OptimisticLockingFailureException.class,
                () -> adapter.save(stale, 0L)
        );
        assertTrue(failure.getMessage().contains("1001"));
    }

    private IdentityUser user(long version) {
        return new IdentityUser(
                "1001", "Alice", "alice", "Alice",
                IdentityUserStatus.ACTIVE, 0, null, null, version
        );
    }

    private PasswordCredential credential(long version) {
        return new PasswordCredential(
                "1001", "{argon2}encoded", NOW, false,
                PasswordCredential.Status.ACTIVE, version
        );
    }
}
