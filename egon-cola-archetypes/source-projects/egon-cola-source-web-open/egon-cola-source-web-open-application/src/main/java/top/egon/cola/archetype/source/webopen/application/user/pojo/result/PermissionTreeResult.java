package top.egon.cola.archetype.source.webopen.application.user.pojo.result;

import java.util.List;
import top.egon.cola.component.common.core.pojo.BasePojo;

public record PermissionTreeResult(Long userId, List<String> permissionCodes) implements BasePojo {
    public PermissionTreeResult {
        permissionCodes = List.copyOf(permissionCodes);
    }
}
