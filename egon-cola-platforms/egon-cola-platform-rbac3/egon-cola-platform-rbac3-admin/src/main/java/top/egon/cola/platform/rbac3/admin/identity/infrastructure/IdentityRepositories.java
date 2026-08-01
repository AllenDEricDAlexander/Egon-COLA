package top.egon.cola.platform.rbac3.admin.identity.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.auth.application.PasswordIdentityAuthenticator;
import top.egon.cola.platform.rbac3.admin.identity.domain.TenantEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.UserCredentialEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.UserEntity;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Tenant-safe identity persistence adapter. Credential reads use a database row lock.
 */
@Repository
public class IdentityRepositories implements PasswordIdentityAuthenticator.CredentialStore {

    private final EntityManager entityManager;
    private final DatabaseClock databaseClock;

    public IdentityRepositories(EntityManager entityManager, DatabaseClock databaseClock) {
        this.entityManager = entityManager;
        this.databaseClock = databaseClock;
    }

    @Transactional(readOnly = true)
    public Optional<TenantEntity> findTenantByCode(String tenantCode) {
        return entityManager.createQuery(
                        "select t from TenantEntity t where lower(t.code) = :code",
                        TenantEntity.class)
                .setParameter("code", tenantCode.toLowerCase(java.util.Locale.ROOT))
                .getResultStream()
                .findFirst();
    }

    @Override
    @Transactional
    public <T> T withCredential(
            String tenantCode,
            String normalizedUsername,
            Function<PasswordIdentityAuthenticator.PasswordCredential, T> action) {
        Objects.requireNonNull(action, "action");
        CredentialRow row = findCredential(tenantCode, normalizedUsername, LockModeType.PESSIMISTIC_WRITE)
                .orElse(null);
        return action.apply(row == null ? null : row.toPasswordCredential());
    }

    @Override
    @Transactional
    public void save(PasswordIdentityAuthenticator.PasswordCredential credential) {
        CredentialRow row = findCredential(
                credential.tenantCode(),
                credential.normalizedUsername(),
                LockModeType.PESSIMISTIC_WRITE).orElseThrow();
        var entity = row.credential();
        if (credential.failureCount() == 0 && credential.lockedUntil() == null) {
            entity.recordSuccess("authentication", databaseClock.transactionNow());
        } else {
            entity.recordFailure(
                    credential.failureCount(),
                    credential.lockedUntil(),
                    "authentication",
                    databaseClock.transactionNow());
        }
    }

    @Override
    @Transactional
    public void updatePasswordHash(
            PasswordIdentityAuthenticator.PasswordCredential credential,
            String passwordHash,
            java.time.Instant changedAt) {
        CredentialRow row = findCredential(
                credential.tenantCode(),
                credential.normalizedUsername(),
                LockModeType.PESSIMISTIC_WRITE).orElseThrow();
        row.credential().replacePasswordHash(
                passwordHash,
                "authentication-rehash",
                databaseClock.transactionNow());
    }

    private Optional<CredentialRow> findCredential(
            String tenantCode,
            String normalizedUsername,
            LockModeType lockMode) {
        List<Object[]> rows = entityManager.createQuery("""
                        select t, u, c
                          from TenantEntity t, UserEntity u, UserCredentialEntity c
                         where lower(t.code) = :tenantCode
                           and u.tenantId = t.id
                           and u.normalizedUsername = :username
                           and c.tenantId = t.id
                           and c.userId = u.id
                           and c.credentialType = :credentialType
                        """, Object[].class)
                .setParameter("tenantCode", tenantCode)
                .setParameter("username", normalizedUsername)
                .setParameter("credentialType", UserCredentialEntity.CredentialType.PASSWORD)
                .setLockMode(lockMode)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] values = rows.getFirst();
        return Optional.of(new CredentialRow(
                (TenantEntity) values[0],
                (UserEntity) values[1],
                (UserCredentialEntity) values[2]));
    }

    private record CredentialRow(
            TenantEntity tenant,
            UserEntity user,
            UserCredentialEntity credential
    ) {

        PasswordIdentityAuthenticator.PasswordCredential toPasswordCredential() {
            boolean active = tenant.getStatus() == TenantEntity.Status.ACTIVE
                    && user.getStatus() == UserEntity.Status.ACTIVE
                    && credential.getStatus() != UserCredentialEntity.Status.DISABLED
                    && credential.getStatus() != UserCredentialEntity.Status.EXPIRED;
            return new PasswordIdentityAuthenticator.PasswordCredential(
                    tenant.getCode(),
                    user.getNormalizedUsername(),
                    user.getId().toString(),
                    credential.getPasswordHash(),
                    credential.getFailedAttempts(),
                    credential.getLockedUntil(),
                    active);
        }
    }
}
