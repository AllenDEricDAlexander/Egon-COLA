package top.egon.cola.archetype.source.webopen.application.user.result;

import java.util.List;

public record PermissionTreeResult(Long userId, List<String> permissionCodes) {
    public PermissionTreeResult {
        permissionCodes = List.copyOf(permissionCodes);
    }
}
