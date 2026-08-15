package top.egon.cola.platform.rbac3.admin.iam.role.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import top.egon.cola.platform.rbac3.admin.iam.business.repository.UserBusinessAccessRepository;
import top.egon.cola.platform.rbac3.admin.iam.business.service.ApplicationCatalogEntry;
import top.egon.cola.platform.rbac3.admin.iam.business.service.DdcCatalogGateway;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves whether a role's local Application and DDC Business are currently usable.
 *
 * <p>The rule is deliberately fail-closed: a missing local scope, missing Business grant,
 * disabled DDC record, or DDC lookup failure never produces an effective role.</p>
 */
@Service
public final class RoleEligibilityService {

    private final EntityManager entityManager;
    private final UserBusinessAccessRepository businessAccessStore;
    private final DdcCatalogGateway catalog;

    public RoleEligibilityService(
            EntityManager entityManager,
            UserBusinessAccessRepository businessAccessStore,
            DdcCatalogGateway catalog) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.businessAccessStore = Objects.requireNonNull(
                businessAccessStore, "businessAccessStore");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    /**
     * Returns whether the local Application is active, has an effective Business grant,
     * and still points to enabled DDC Application and Business records.
     */
    public boolean isEffective(
            String tenantId,
            String userId,
            String applicationId,
            Instant at) {
        try {
            long tenant = Long.parseLong(required(tenantId, "tenantId"));
            long user = Long.parseLong(required(userId, "userId"));
            LocalApplication application = findApplication(tenant, applicationId)
                    .orElse(null);
            if (application == null || !"ACTIVE".equals(application.status())) {
                return false;
            }
            Set<String> businesses = businessAccessStore.effectiveBusinessIds(
                    tenant, user, Objects.requireNonNull(at, "at"));
            if (!businesses.contains(application.ddcBusinessId())) {
                return false;
            }
            ApplicationCatalogEntry ddcApplication = catalog.findApplication(
                            application.ddcApplicationId())
                    .orElse(null);
            return ddcApplication != null
                    && ddcApplication.applicationEnabled()
                    && ddcApplication.businessEnabled()
                    && application.ddcBusinessId().equals(ddcApplication.ddcBusinessId());
        } catch (RuntimeException unavailableOrInvalid) {
            return false;
        }
    }

    /** Returns whether an application code currently resolves to an effective scope. */
    public boolean isEffectiveApplicationCode(
            String tenantId,
            String userId,
            String applicationCode,
            Instant at) {
        try {
            long tenant = Long.parseLong(required(tenantId, "tenantId"));
            LocalApplication application = findApplicationByCode(tenant, applicationCode)
                    .orElse(null);
            return application != null && isEffective(
                    tenantId, userId, Long.toString(application.id()), at);
        } catch (RuntimeException unavailableOrInvalid) {
            return false;
        }
    }

    /** Rejects a role assignment whose Application/Business chain is not effective. */
    public void requireEffectiveRole(
            String tenantId,
            String userId,
            String roleId,
            Instant at) {
        try {
            long tenant = Long.parseLong(required(tenantId, "tenantId"));
            long role = Long.parseLong(required(roleId, "roleId"));
            List<?> result = entityManager.createNativeQuery("""
                            select r.application_id
                              from rbac3_role r
                             where r.tenant_id = :tenantId and r.id = :roleId
                            """)
                    .setParameter("tenantId", tenant)
                    .setParameter("roleId", role)
                    .getResultList();
            if (result.size() != 1
                    || !isEffective(tenantId, userId,
                    Long.toString(number(result.getFirst())), at)) {
                throw new Rbac3RuleViolation("BUSINESS_ACCESS_REQUIRED");
            }
        } catch (Rbac3RuleViolation violation) {
            throw violation;
        } catch (RuntimeException unavailableOrInvalid) {
            throw new Rbac3RuleViolation("BUSINESS_ACCESS_REQUIRED");
        }
    }

    private Optional<LocalApplication> findApplication(long tenantId, String applicationId) {
        long id = Long.parseLong(required(applicationId, "applicationId"));
        List<?> result = entityManager.createNativeQuery("""
                        select a.id, a.ddc_application_id, a.ddc_business_id, a.status
                          from rbac3_application a
                         where a.tenant_id = :tenantId and a.id = :applicationId
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("applicationId", id)
                .getResultList();
        return result.stream().findFirst().map(RoleEligibilityService::application);
    }

    private Optional<LocalApplication> findApplicationByCode(
            long tenantId,
            String applicationCode) {
        String code = required(applicationCode, "applicationCode");
        List<?> result = entityManager.createNativeQuery("""
                        select a.id, a.ddc_application_id, a.ddc_business_id, a.status
                          from rbac3_application a
                         where a.tenant_id = :tenantId and a.application_code = :applicationCode
                        """)
                .setParameter("tenantId", tenantId)
                .setParameter("applicationCode", code)
                .getResultList();
        return result.stream().findFirst().map(RoleEligibilityService::application);
    }

    private static LocalApplication application(Object value) {
        Object[] row = (Object[]) value;
        return new LocalApplication(
                number(row[0]),
                String.valueOf(row[1]),
                String.valueOf(row[2]),
                String.valueOf(row[3]));
    }

    private static long number(Object value) {
        return ((Number) value).longValue();
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private record LocalApplication(
            long id,
            String ddcApplicationId,
            String ddcBusinessId,
            String status) {
    }
}
