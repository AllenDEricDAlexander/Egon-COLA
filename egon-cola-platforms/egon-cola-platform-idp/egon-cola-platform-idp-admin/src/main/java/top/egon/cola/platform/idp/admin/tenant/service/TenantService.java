package top.egon.cola.platform.idp.admin.tenant.service;

import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantEntity;

import java.time.Instant;
import java.util.List;

/** Application service for IdP-owned tenant catalog lifecycle. */
public interface TenantService {

    List<TenantView> list();

    TenantView get(String tenantId);

    TenantView create(CreateTenantCommand command);

    TenantView update(String tenantId, UpdateTenantCommand command);

    /** Input for a new tenant catalog row. */
    record CreateTenantCommand(
            String tenantCode,
            String tenantName,
            String settings,
            String operatorSub
    ) {
    }

    /** Versioned tenant catalog patch. Null name/settings preserve current values. */
    record UpdateTenantCommand(
            long expectedVersion,
            String tenantName,
            String settings,
            IdentityTenantEntity.Status status,
            String operatorSub
    ) {
    }

    /** Transport-neutral safe tenant view. */
    record TenantView(
            String tenantId,
            String tenantCode,
            String tenantName,
            IdentityTenantEntity.Status status,
            String settings,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
