package top.egon.cola.platform.rbac3.contract.authorization;

public record PermissionRequest(
        String permissionCode,
        String resourceCode
) {

    public PermissionRequest {
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new IllegalArgumentException(
                    "permissionCode is required"
            );
        }
        permissionCode = permissionCode.trim();
        resourceCode = resourceCode == null ? null : required(
                resourceCode,
                "resourceCode"
        );
    }

    public static PermissionRequest of(String permissionCode) {
        return new PermissionRequest(permissionCode, null);
    }

    public static PermissionRequest api(
            String permissionCode,
            String resourceCode) {
        return new PermissionRequest(permissionCode, resourceCode);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
