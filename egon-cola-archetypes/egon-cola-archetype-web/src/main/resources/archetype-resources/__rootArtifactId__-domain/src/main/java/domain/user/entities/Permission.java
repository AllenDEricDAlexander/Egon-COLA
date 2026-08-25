package ${package}.domain.user.entities;

import ${package}.domain.user.enums.PermissionStatus;
import ${package}.domain.user.enums.PermissionType;
import ${package}.domain.user.vos.PermissionCode;

public record Permission(
        Long id,
        PermissionCode code,
        String name,
        PermissionType type,
        PermissionStatus status) {
}
