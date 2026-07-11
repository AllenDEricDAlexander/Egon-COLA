package ${package}.domain.entities.user;

import ${package}.domain.enums.user.PermissionStatus;
import ${package}.domain.enums.user.PermissionType;
import ${package}.domain.vos.user.PermissionCode;

public record Permission(
        String id,
        PermissionCode code,
        String name,
        PermissionType type,
        PermissionStatus status) {
}
