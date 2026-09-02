package top.egon.cola.archetype.source.web.infrastructure.user.repo.converter;

import top.egon.cola.archetype.source.web.domain.user.entities.Role;
import top.egon.cola.archetype.source.web.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.web.domain.user.vos.PermissionCode;
import top.egon.cola.archetype.source.web.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.web.infrastructure.user.repo.po.RolePO;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.util.List;

@Component("rolePOConverter")
public final class RolePOConverter implements BaseConverter<Role, RolePO> {
    @Override
    public RolePO toTarget(Role role) {
        RolePO target = RolePO.builder().code(role.code().value()).name(role.name())
                .status(role.status().name()).build();
        target.setId(role.id());
        return target;
    }

    @Override
    public Role toSource(RolePO target) {
        return toEntity(target, List.of());
    }

    public Role toEntity(RolePO target, List<PermissionCode> permissionCodes) {
        return new Role(target.getId(), new RoleCode(target.getCode()), target.getName(),
                RoleStatus.valueOf(target.getStatus()), permissionCodes);
    }

    public RolePO toPO(Role role) {
        return toTarget(role);
    }
}
