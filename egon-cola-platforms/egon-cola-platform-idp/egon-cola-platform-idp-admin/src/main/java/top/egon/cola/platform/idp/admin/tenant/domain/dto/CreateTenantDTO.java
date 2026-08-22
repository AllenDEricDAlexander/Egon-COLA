package top.egon.cola.platform.idp.admin.tenant.domain.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Input for creating an IdP-owned tenant catalog row. */
public record CreateTenantDTO(
        @NotBlank
        @Pattern(regexp = "^[a-z][a-z0-9-]{2,63}$")
        String tenantCode,
        @NotBlank
        @Size(max = 200)
        String tenantName,
        JsonNode settings
) {
}
