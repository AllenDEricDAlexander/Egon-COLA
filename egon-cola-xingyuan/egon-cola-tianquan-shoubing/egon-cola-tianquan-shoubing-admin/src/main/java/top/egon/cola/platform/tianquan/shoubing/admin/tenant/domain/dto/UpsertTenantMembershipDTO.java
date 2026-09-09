package top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.dto;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.pojo.IdentityTenantMembershipEntity;

/** Idempotent membership target state and optional create/version token. */
public record UpsertTenantMembershipDTO(
        @NotNull IdentityTenantMembershipEntity.Status status,
        Long expectedVersion
) {
}
