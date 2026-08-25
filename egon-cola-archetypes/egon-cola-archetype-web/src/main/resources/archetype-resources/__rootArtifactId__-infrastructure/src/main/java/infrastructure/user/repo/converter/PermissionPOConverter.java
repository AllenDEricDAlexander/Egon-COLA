package ${package}.infrastructure.user.repo.converter;

import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.enums.PermissionStatus;
import ${package}.domain.user.enums.PermissionType;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.infrastructure.user.repo.po.PermissionPO;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseConverter;

@Component("permissionPOConverter")
public final class PermissionPOConverter implements BaseConverter<Permission, PermissionPO> {
    @Override
    public PermissionPO toTarget(Permission permission) {
        PermissionPO target = PermissionPO.builder().code(permission.code().value()).name(permission.name())
                .type(permission.type().name()).status(permission.status().name()).build();
        target.setId(permission.id());
        return target;
    }

    @Override
    public Permission toSource(PermissionPO target) {
        return new Permission(target.getId(), new PermissionCode(target.getCode()), target.getName(),
                PermissionType.valueOf(target.getType()), PermissionStatus.valueOf(target.getStatus()));
    }

    public PermissionPO toPO(Permission permission) {
        return toTarget(permission);
    }
}
