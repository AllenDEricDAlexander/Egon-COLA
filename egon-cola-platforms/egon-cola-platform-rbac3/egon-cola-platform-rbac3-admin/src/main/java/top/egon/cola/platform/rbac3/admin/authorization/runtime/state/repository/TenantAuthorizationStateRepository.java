package top.egon.cola.platform.rbac3.admin.authorization.runtime.state.repository;

import top.egon.cola.platform.rbac3.admin.authorization.runtime.state.domain.po.TenantAuthorizationStatePO;

import java.util.Objects;

/** Persistence boundary for RBAC tenant-scoped authorization versions. */
public interface TenantAuthorizationStateRepository {

    TenantAuthorizationStatePO require(Long tenantId);

    TenantAuthorizationStatePO requireForUpdate(Long tenantId);

    TenantAuthorizationStatePO ensureVerifiedTenant(
            VerifiedTenant tenant,
            String actorId
    );

    long increment(Long tenantId, String actorId);

    /** Typed marker created only after an upstream IdP membership gate succeeds. */
    record VerifiedTenant(Long tenantId) {
        public VerifiedTenant {
            if (tenantId == null || tenantId <= 0L) {
                throw new IllegalArgumentException("tenantId is invalid");
            }
        }

        public static VerifiedTenant of(Long tenantId) {
            return new VerifiedTenant(Objects.requireNonNull(tenantId, "tenantId"));
        }
    }
}
