package top.egon.cola.platform.rbac3.admin.authorization.domain.dto;

/**
 * User entry-permission request bound to an IdP subject and tenant.
 */
public record ResourceAccessRequestDTO(
        String identitySub,
        String tenantId,
        String rbacApplicationCode,
        String entryPermissionCode) {

    public ResourceAccessRequestDTO {
        identitySub = required(identitySub, "identitySub");
        tenantId = required(tenantId, "tenantId");
        rbacApplicationCode = required(rbacApplicationCode, "rbacApplicationCode");
        entryPermissionCode = required(entryPermissionCode, "entryPermissionCode");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
