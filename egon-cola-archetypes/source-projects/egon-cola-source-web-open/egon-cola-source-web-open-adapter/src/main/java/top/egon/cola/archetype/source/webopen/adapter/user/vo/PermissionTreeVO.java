package top.egon.cola.archetype.source.webopen.adapter.user.vo;

import java.util.List;

public record PermissionTreeVO(String userId, List<String> permissionCodes) {
    public PermissionTreeVO {
        permissionCodes = List.copyOf(permissionCodes);
    }
}
