package top.egon.cola.archetype.source.web.adapter.user.vo;

import java.util.List;

public record PermissionTreeVO(Long userId, List<String> permissionCodes) {
    public PermissionTreeVO {
        permissionCodes = List.copyOf(permissionCodes);
    }
}
