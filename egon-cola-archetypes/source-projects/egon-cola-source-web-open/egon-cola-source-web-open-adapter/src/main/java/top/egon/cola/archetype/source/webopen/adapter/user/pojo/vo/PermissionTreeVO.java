package top.egon.cola.archetype.source.webopen.adapter.user.pojo.vo;

import java.util.List;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record PermissionTreeVO(Long userId, List<String> permissionCodes) implements BasePojo {
    public PermissionTreeVO {
        permissionCodes = List.copyOf(permissionCodes);
    }
}
