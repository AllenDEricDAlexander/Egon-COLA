package ${package}.infrastructure.repo.user.converter;

import ${package}.domain.entities.user.Permission;
import ${package}.domain.enums.user.PermissionStatus;
import ${package}.domain.enums.user.PermissionType;
import ${package}.domain.vos.user.PermissionCode;
import ${package}.infrastructure.repo.user.po.PermissionPO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component("permissionPOConverter")
public final class PermissionPOConverter {
    public PermissionPO toPO(Permission permission) {
        return new PermissionPO(permission.id(), permission.code().value(), permission.name(),
            permission.type().name(), permission.status().name(), LocalDateTime.now());
    }

    public Permission toEntity(PermissionPO permissionPO) {
        return new Permission(permissionPO.getId(), new PermissionCode(permissionPO.getCode()),
            permissionPO.getName(), PermissionType.valueOf(permissionPO.getType()),
            PermissionStatus.valueOf(permissionPO.getStatus()));
    }
}
