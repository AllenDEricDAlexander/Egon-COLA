package ${package}.adapter.vo.user;

import java.util.List;

public record PermissionTreeVO(String userId, List<String> permissionCodes) {
    public PermissionTreeVO {
        permissionCodes = List.copyOf(permissionCodes);
    }
}
