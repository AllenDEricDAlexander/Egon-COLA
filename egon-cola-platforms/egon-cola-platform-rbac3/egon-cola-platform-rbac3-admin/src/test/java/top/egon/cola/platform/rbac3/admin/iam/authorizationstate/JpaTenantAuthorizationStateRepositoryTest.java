package top.egon.cola.platform.rbac3.admin.iam.authorizationstate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.admin.iam.authorizationstate.domain.po.TenantAuthorizationStatePO;
import top.egon.cola.platform.rbac3.admin.iam.authorizationstate.repository.TenantAuthorizationStateRepository;
import top.egon.cola.platform.rbac3.admin.iam.authorizationstate.repository.jpa.JpaTenantAuthorizationStateRepository;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaTenantAuthorizationStateRepositoryTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T03:00:00Z");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final DatabaseClock clock = mock(DatabaseClock.class);
    private final JpaTenantAuthorizationStateRepository repository =
            new JpaTenantAuthorizationStateRepository(entityManager, clock);

    @Test
    void requiresExistingStateWithPessimisticLock() {
        TenantAuthorizationStatePO state =
                new TenantAuthorizationStatePO(10001L, "migration", NOW);
        when(entityManager.find(
                TenantAuthorizationStatePO.class,
                10001L,
                LockModeType.PESSIMISTIC_WRITE
        )).thenReturn(state);

        assertThat(repository.requireForUpdate(10001L)).isSameAs(state);
        verify(entityManager).find(
                TenantAuthorizationStatePO.class,
                10001L,
                LockModeType.PESSIMISTIC_WRITE
        );
    }

    @Test
    void rejectsMissingStateOutsideVerifiedEnsurePath() {
        when(entityManager.find(
                TenantAuthorizationStatePO.class,
                10001L,
                LockModeType.PESSIMISTIC_WRITE
        )).thenReturn(null);

        assertThatThrownBy(() -> repository.requireForUpdate(10001L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("tenant authorization state not found");
    }

    @Test
    void ensureAndIncrementUseDatabaseClockAndMonotonicPolicyVersion() {
        when(clock.transactionNow()).thenReturn(NOW);
        when(entityManager.find(TenantAuthorizationStatePO.class, 10001L))
                .thenReturn(null);

        TenantAuthorizationStatePO created = repository.ensureVerifiedTenant(
                new TenantAuthorizationStateRepository.VerifiedTenant(10001L),
                "operator"
        );
        assertThat(created.getTenantId()).isEqualTo(10001L);
        assertThat(created.getPolicyVersion()).isZero();
        verify(entityManager).persist(created);

        TenantAuthorizationStatePO locked =
                new TenantAuthorizationStatePO(10001L, "migration", NOW);
        when(entityManager.find(
                TenantAuthorizationStatePO.class,
                10001L,
                LockModeType.PESSIMISTIC_WRITE
        )).thenReturn(locked);
        when(clock.transactionNow()).thenReturn(NOW.plusSeconds(1));

        assertThat(repository.increment(10001L, "operator"))
                .isEqualTo(1L);
        assertThat(locked.getPolicyVersion()).isEqualTo(1L);
    }
}
