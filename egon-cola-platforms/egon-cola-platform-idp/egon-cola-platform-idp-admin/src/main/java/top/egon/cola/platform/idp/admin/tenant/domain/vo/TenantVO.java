package top.egon.cola.platform.idp.admin.tenant.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantEntity;

import java.time.Instant;

/** Safe IdP tenant catalog view; no RBAC or credential fields are exposed. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TenantVO(
        String tenantId,
        String tenantCode,
        String tenantName,
        IdentityTenantEntity.Status status,
        JsonNode settings,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
