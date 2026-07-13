package ${package}.application.user.result;

import java.util.List;

public record PermissionTreeResult(String userId, List<String> permissionCodes) {
    public PermissionTreeResult {
        permissionCodes = List.copyOf(permissionCodes);
    }
}
