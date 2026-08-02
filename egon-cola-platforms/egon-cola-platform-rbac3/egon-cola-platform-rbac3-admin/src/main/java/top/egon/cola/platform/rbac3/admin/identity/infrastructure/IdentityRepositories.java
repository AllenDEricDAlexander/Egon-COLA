package top.egon.cola.platform.rbac3.admin.identity.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.auth.application.PasswordIdentityAuthenticator;
import top.egon.cola.platform.rbac3.admin.identity.domain.TenantEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.ExternalIdentityEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.UserCredentialEntity;
import top.egon.cola.platform.rbac3.admin.identity.domain.UserEntity;
import top.egon.cola.platform.rbac3.admin.identity.application.IdentityMappingFacade;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Tenant-safe identity persistence adapter. Credential reads use a database row lock.
 */
@Repository
public class IdentityRepositories implements PasswordIdentityAuthenticator.CredentialStore,
        IdentityMappingFacade.MappingStore {

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
    @Transactional(readOnly = true)
    public Optional<IdentityMappingFacade.Mapping> find(
            String tenantId, String identitySub) {
        return entityManager.createQuery("""
                        select i from ExternalIdentityEntity i
                         where i.tenantId = :tenantId
                           and i.identitySub = :identitySub
                        """, ExternalIdentityEntity.class)
                .setParameter("tenantId", Long.valueOf(tenantId))
                .setParameter("identitySub", identitySub)
                .getResultStream()
                .findFirst()
                .map(IdentityRepositories::toMapping);
    }

    @Override
    @Transactional
    public IdentityMappingFacade.Mapping create(
            long mappingId,
            String tenantId,
            String identitySub,
            String rbac3UserId,
            String actorId,
            java.time.Instant now) {
        Long numericTenantId = Long.valueOf(tenantId);
        Long numericUserId = Long.valueOf(rbac3UserId);
        requireActiveTenantAndUser(numericTenantId, numericUserId);
        ExternalIdentityEntity entity = ExternalIdentityEntity.idpMapping(
                mappingId, numericTenantId, identitySub, numericUserId, actorId, now);
        entityManager.persist(entity);
        entityManager.flush();
        return toMapping(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityMappingFacade.ResolvedMembership> resolve(
            String tenantId, String identitySub) {
        return activeMemberships(identitySub, Long.valueOf(tenantId)).stream()
                .findFirst()
                .map(IdentityRepositories::toResolvedMembership);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdentityMappingFacade.TenantMembership> tenants(String identitySub) {
        return activeMemberships(identitySub, null).stream()
                .map(row -> new IdentityMappingFacade.TenantMembership(
                        row.tenant().getId().toString(), row.tenant().getCode(),
                        row.tenant().getName(), row.user().getId().toString(),
                        row.user().getDisplayName()))
                .toList();
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

    private List<MembershipRow> activeMemberships(
            String identitySub, Long tenantId) {
        String tenantPredicate = tenantId == null ? "" : " and t.id = :tenantId";
        var query = entityManager.createQuery("""
                        select i, t, u
                          from ExternalIdentityEntity i, TenantEntity t, UserEntity u
                         where i.identitySub = :identitySub
                           and i.status = :identityStatus
                           and t.id = i.tenantId
                           and t.status = :tenantStatus
                           and u.tenantId = i.tenantId
                           and u.id = i.userId
                           and u.status = :userStatus
                        """ + tenantPredicate + " order by t.id", Object[].class)
                .setParameter("identitySub", identitySub)
                .setParameter("identityStatus", ExternalIdentityEntity.Status.ACTIVE)
                .setParameter("tenantStatus", TenantEntity.Status.ACTIVE)
                .setParameter("userStatus", UserEntity.Status.ACTIVE);
        if (tenantId != null) {
            query.setParameter("tenantId", tenantId);
        }
        return query.getResultList().stream()
                .map(values -> new MembershipRow(
                        (ExternalIdentityEntity) values[0],
                        (TenantEntity) values[1],
                        (UserEntity) values[2]))
                .toList();
    }

    private void requireActiveTenantAndUser(Long tenantId, Long userId) {
        TenantEntity tenant = entityManager.find(
                TenantEntity.class, tenantId, LockModeType.PESSIMISTIC_READ);
        UserEntity user = entityManager.find(
                UserEntity.class, userId, LockModeType.PESSIMISTIC_READ);
        if (tenant == null || tenant.getStatus() != TenantEntity.Status.ACTIVE
                || user == null || !tenantId.equals(user.getTenantId())
                || user.getStatus() != UserEntity.Status.ACTIVE) {
            throw new IllegalStateException("active tenant user is required");
        }
    }

    private static IdentityMappingFacade.Mapping toMapping(
            ExternalIdentityEntity entity) {
        return new IdentityMappingFacade.Mapping(
                entity.getId().toString(), entity.getTenantId().toString(),
                entity.getIdentitySub(), entity.getUserId().toString(),
                entity.getStatus() == ExternalIdentityEntity.Status.ACTIVE,
                entity.getUpdatedAt());
    }

    private static IdentityMappingFacade.ResolvedMembership toResolvedMembership(
            MembershipRow row) {
        return new IdentityMappingFacade.ResolvedMembership(
                row.tenant().getId().toString(), row.tenant().getCode(),
                row.tenant().getName(), row.identity().getIdentitySub(),
                row.user().getId().toString(), row.user().getDisplayName(), true,
                row.user().getAuthVersion(), row.tenant().getPolicyVersion());
    }

    private record MembershipRow(
            ExternalIdentityEntity identity,
            TenantEntity tenant,
            UserEntity user
    ) {
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
