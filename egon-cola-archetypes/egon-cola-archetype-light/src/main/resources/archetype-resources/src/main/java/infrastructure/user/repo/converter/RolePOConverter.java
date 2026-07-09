package ${package}.infrastructure.user.repo.converter;

import ${package}.domain.user.entities.Role;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.vos.RoleCode;
import ${package}.infrastructure.user.repo.po.RolePO;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RolePOConverter {
    public RolePO toPO(Role role) {
        return new RolePO(role.code().value(), role.name(), role.status().name(), Instant.now());
    }

    public Role toDomain(RolePO role) {
        return new Role(
                new RoleCode(role.getCode()), role.getName(), RoleStatus.valueOf(role.getStatus()));
    }
}
