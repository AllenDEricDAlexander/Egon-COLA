package ${package}.infrastructure.repo.user.converter;

import ${package}.domain.entities.user.Role;
import ${package}.domain.enums.user.RoleStatus;
import ${package}.domain.vos.user.PermissionCode;
import ${package}.domain.vos.user.RoleCode;
import ${package}.infrastructure.repo.user.po.RolePO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component("rolePOConverter")
public final class RolePOConverter {
    public RolePO toPO(Role role) {
        return new RolePO(role.id(), role.code().value(), role.name(), role.status().name(), LocalDateTime.now());
    }

    public Role toEntity(RolePO rolePO, List<PermissionCode> permissionCodes) {
        return new Role(rolePO.getId(), new RoleCode(rolePO.getCode()), rolePO.getName(),
            RoleStatus.valueOf(rolePO.getStatus()), permissionCodes);
    }
}
