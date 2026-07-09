package ${package}.infrastructure.user.repo.converter;

import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.enums.PermissionStatus;
import ${package}.domain.user.vos.PermissionCode;
import ${package}.infrastructure.user.repo.po.PermissionPO;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PermissionPOConverter {
    public PermissionPO toPO(Permission permission) {
        return new PermissionPO(
                permission.code().value(), permission.name(), permission.status().name(), Instant.now());
    }

    public Permission toDomain(PermissionPO permission) {
        return new Permission(
                new PermissionCode(permission.getCode()), permission.getName(),
                PermissionStatus.valueOf(permission.getStatus()));
    }
}
