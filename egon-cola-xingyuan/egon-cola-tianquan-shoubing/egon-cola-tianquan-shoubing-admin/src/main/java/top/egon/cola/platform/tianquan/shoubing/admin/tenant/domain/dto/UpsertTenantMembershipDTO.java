package top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.dto;

import jakarta.validation.constraints.NotNull;
import top.egon.cola.platform.tianquan.shoubing.admin.tenant.domain.bo.IdentityTenantMembershipBO;

/** Idempotent membership target state and optional create/version token. */
public record UpsertTenantMembershipDTO(
        @NotNull IdentityTenantMembershipBO.Status status,
        Long expectedVersion
) {
}
