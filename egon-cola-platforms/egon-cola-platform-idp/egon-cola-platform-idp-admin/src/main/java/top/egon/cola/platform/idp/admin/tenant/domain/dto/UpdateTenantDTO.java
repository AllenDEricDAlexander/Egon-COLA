package top.egon.cola.platform.idp.admin.tenant.domain.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import top.egon.cola.platform.idp.admin.tenant.domain.pojo.IdentityTenantEntity;

/** Versioned patch for an IdP-owned tenant catalog row. */
public record UpdateTenantDTO(
        @NotNull
        @PositiveOrZero
        Long expectedVersion,
        @Size(max = 200)
        String tenantName,
        JsonNode settings,
        @NotNull
        IdentityTenantEntity.Status status
) {
}
