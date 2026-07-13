package ${package}.domain.user.entities;

import ${package}.domain.user.enums.PermissionStatus;
import ${package}.domain.user.enums.PermissionType;
import ${package}.domain.user.vos.PermissionCode;

public record Permission(
        String id,
        PermissionCode code,
        String name,
        PermissionType type,
        PermissionStatus status) {
}
