package top.egon.cola.platform.rbac3.contract.authorization;

public record PermissionRequest(
        String permissionCode
) {

    public PermissionRequest {
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new IllegalArgumentException(
                    "permissionCode is required"
            );
        }
        permissionCode = permissionCode.trim();
    }

    public static PermissionRequest of(String permissionCode) {
        return new PermissionRequest(permissionCode);
    }
}
