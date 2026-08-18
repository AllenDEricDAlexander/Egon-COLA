package top.egon.cola.platform.rbac3.admin.iam.application.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.enums.ApplicationStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.enums.TenantApplicationStatusEnum;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.po.ApplicationPO;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.po.TenantApplicationPO;
import top.egon.cola.platform.rbac3.admin.iam.application.domain.vo.ApplicationAuthorizationScopeVO;
import top.egon.cola.platform.rbac3.admin.iam.application.repository.ApplicationResourceRepository;
import top.egon.cola.platform.rbac3.admin.iam.business.service.ApplicationCatalogEntry;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** JPA adapter for tenant application entitlements and global catalog rows. */
@Repository
public class JpaTenantApplicationRepository implements ApplicationResourceRepository {

    private final EntityManager entityManager;
    private final LongIdGenerator idGenerator;
    private final DatabaseClock databaseClock;

    public JpaTenantApplicationRepository(
            EntityManager entityManager,
            LongIdGenerator idGenerator,
            DatabaseClock databaseClock) {
        this.entityManager = entityManager;
        this.idGenerator = idGenerator;
        this.databaseClock = databaseClock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationAuthorizationScopeVO> authorizationScopes(Long tenantId) {
        return entityManager.createQuery("""
                        select ta from TenantApplicationEntity ta
                         where ta.tenantId = :tenantId
                         order by ta.id
                        """, TenantApplicationPO.class)
                .setParameter("tenantId", requireTenant(tenantId))
                .getResultList().stream()
                .map(this::toAuthorizationScope)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApplicationAuthorizationScopeVO> authorizationScope(
            Long tenantId,
            Long applicationId) {
        TenantApplicationPO entitlement = entityManager.find(
                TenantApplicationPO.class, Objects.requireNonNull(applicationId, "applicationId"));
        if (entitlement == null || !requireTenant(tenantId).equals(entitlement.getTenantId())) {
            return Optional.empty();
        }
        return Optional.of(toAuthorizationScope(entitlement));
    }

    @Override
    @Transactional
    public ApplicationAuthorizationScopeVO admit(
            Long tenantId,
            ApplicationCatalogEntry catalog,
            int displayPriority,
            String actorId) {
        Objects.requireNonNull(catalog, "catalog");
        Long tenant = requireTenant(tenantId);
        ApplicationPO application = entityManager.createQuery("""
                        select a from ApplicationEntity a
                         where a.ddcApplicationId = :ddcApplicationId
                        """, ApplicationPO.class)
                .setParameter("ddcApplicationId", catalog.ddcApplicationId())
                .getResultStream().findFirst()
                .orElseGet(() -> {
                    ApplicationPO created = new ApplicationPO(
                            idGenerator.nextLongId(), tenant, catalog.ddcApplicationId(),
                            catalog.ddcBusinessId(), catalog.appCode(), catalog.appName(),
                            displayPriority, actorId, databaseClock.transactionNow());
                    entityManager.persist(created);
                    return created;
                });
        boolean exists = !entityManager.createQuery("""
                        select ta.id from TenantApplicationEntity ta
                         where ta.tenantId = :tenantId and ta.applicationId = :applicationId
                        """, Long.class)
                .setParameter("tenantId", tenant)
                .setParameter("applicationId", application.getId())
                .setMaxResults(1).getResultList().isEmpty();
        if (exists) {
            throw new IllegalStateException("tenant application already exists");
        }
        Instant now = databaseClock.transactionNow();
        TenantApplicationPO entitlement = new TenantApplicationPO(
                idGenerator.nextLongId(), tenant, application.getId(),
                TenantApplicationStatusEnum.ACTIVE, now, null,
                "DDC", catalog.ddcApplicationId(), null, null, actorId, now);
        entityManager.persist(entitlement);
        return toAuthorizationScope(entitlement, application);
    }

    @Override
    @Transactional
    public ApplicationAuthorizationScopeVO changeStatus(
            Long tenantId,
            Long applicationId,
            String status,
            long expectedVersion,
            String actorId) {
        TenantApplicationPO entitlement = requireEntitlement(
                tenantId, applicationId, LockModeType.PESSIMISTIC_WRITE);
        TenantApplicationStatusEnum next = parseStatus(status);
        entitlement.changeStatus(next, expectedVersion, actorId, databaseClock.transactionNow());
        return toAuthorizationScope(entitlement);
    }

    @Override
    @Transactional
    public void remove(
            Long tenantId,
            Long applicationId,
            long expectedVersion,
            String actorId) {
        TenantApplicationPO entitlement = requireEntitlement(
                tenantId, applicationId, LockModeType.PESSIMISTIC_WRITE);
        if (entitlement.getVersion() != expectedVersion) {
            throw new IllegalStateException("tenant application version conflict");
        }
        Long tenant = requireTenant(tenantId);
        boolean dependency = Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        select exists (select 1 from rbac3_role
                            where tenant_id = :tenantId and application_id = :applicationId)
                            or exists (select 1 from rbac3_user_active_role
                            where tenant_id = :tenantId and application_id = :applicationId)
                        """)
                .setParameter("tenantId", tenant)
                .setParameter("applicationId", entitlement.getApplicationId())
                .getSingleResult());
        if (dependency) {
            throw new IllegalStateException("tenant application has authorization dependencies");
        }
        entityManager.remove(entitlement);
    }

    private ApplicationAuthorizationScopeVO toAuthorizationScope(TenantApplicationPO entitlement) {
        ApplicationPO application = entityManager.find(ApplicationPO.class, entitlement.getApplicationId());
        if (application == null) {
            throw new IllegalStateException("global application not found");
        }
        return toAuthorizationScope(entitlement, application);
    }

    private ApplicationAuthorizationScopeVO toAuthorizationScope(
            TenantApplicationPO entitlement,
            ApplicationPO application) {
        return new ApplicationAuthorizationScopeVO(
                entitlement.getId().toString(), application.getDdcBusinessId(),
                application.getDdcApplicationId(), null, application.getApplicationCode(),
                application.getApplicationName(), entitlement.getStatus().name(),
                application.getDisplayPriority(), entitlement.getVersion());
    }

    private TenantApplicationPO requireEntitlement(
            Long tenantId,
            Long entitlementId,
            LockModeType lockMode) {
        TenantApplicationPO entitlement = entityManager.find(
                TenantApplicationPO.class, Objects.requireNonNull(entitlementId, "applicationId"), lockMode);
        if (entitlement == null || !requireTenant(tenantId).equals(entitlement.getTenantId())) {
            throw new IllegalStateException("tenant application not found");
        }
        return entitlement;
    }

    private static TenantApplicationStatusEnum parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        try {
            return TenantApplicationStatusEnum.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unsupported tenant application status", exception);
        }
    }

    private static Long requireTenant(Long tenantId) {
        return Objects.requireNonNull(tenantId, "tenantId");
    }
}
