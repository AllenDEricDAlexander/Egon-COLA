package top.egon.cola.platform.idp.admin.tenant.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantMembershipEntity;

import java.time.Instant;

/** Safe tenant membership view with an optional path-owner tenant id. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TenantMembershipVO(
        String tenantId,
        String identitySub,
        String displayName,
        IdentityTenantMembershipEntity.Status status,
        long version,
        Instant updatedAt
) {
}
