package top.egon.cola.archetype.source.webopen.application.user.assemblers;

import top.egon.cola.archetype.source.webopen.application.user.result.PermissionTreeResult;
import top.egon.cola.archetype.source.webopen.domain.user.entities.Permission;

import java.util.List;

public final class PermissionAssembler {
    public PermissionTreeResult toResult(Long userId, List<Permission> permissions) {
        return new PermissionTreeResult(userId, permissions.stream()
            .map(permission -> permission.code().value()).distinct().sorted().toList());
    }
}
