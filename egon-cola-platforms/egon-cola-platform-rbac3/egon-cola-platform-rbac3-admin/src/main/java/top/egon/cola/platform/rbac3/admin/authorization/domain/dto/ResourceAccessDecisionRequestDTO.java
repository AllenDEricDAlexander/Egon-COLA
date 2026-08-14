package top.egon.cola.platform.rbac3.admin.authorization.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Transport request for checking a user's application entry permission.
 */
public record ResourceAccessDecisionRequestDTO(
        @NotBlank String identitySub,
        @NotBlank String tenantId,
        @NotBlank String rbacApplicationCode,
        @NotBlank String entryPermissionCode) {

    public ResourceAccessDecisionRequestDTO {
        identitySub = required(identitySub, "identitySub");
        tenantId = required(tenantId, "tenantId");
        rbacApplicationCode = required(rbacApplicationCode, "rbacApplicationCode");
        entryPermissionCode = required(entryPermissionCode, "entryPermissionCode");
    }

    public ResourceAccessRequestDTO toCommand() {
        return new ResourceAccessRequestDTO(
                identitySub, tenantId, rbacApplicationCode, entryPermissionCode);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
