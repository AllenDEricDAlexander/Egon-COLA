package ${package}.application.assemblers.user;

import ${package}.application.result.user.PermissionTreeResult;
import ${package}.domain.user.entities.Permission;

import java.util.List;

public final class PermissionAssembler {
    public PermissionTreeResult toResult(String userId, List<Permission> permissions) {
        return new PermissionTreeResult(userId, permissions.stream()
            .map(permission -> permission.code().value()).distinct().sorted().toList());
    }
}
