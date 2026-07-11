package ${package}.application.result.user;

import java.util.List;

public record PermissionTreeResult(String userId, List<String> permissionCodes) {
    public PermissionTreeResult {
        permissionCodes = List.copyOf(permissionCodes);
    }
}
