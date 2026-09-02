package top.egon.cola.archetype.source.webopen.infrastructure.user.repo.converter;

import top.egon.cola.archetype.source.webopen.domain.user.entities.Permission;
import top.egon.cola.archetype.source.webopen.domain.user.enums.PermissionStatus;
import top.egon.cola.archetype.source.webopen.domain.user.enums.PermissionType;
import top.egon.cola.archetype.source.webopen.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.po.PermissionPO;
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
