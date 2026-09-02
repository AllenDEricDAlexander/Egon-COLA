package top.egon.cola.archetype.source.web.domain.user.entities;

import top.egon.cola.archetype.source.web.domain.user.enums.PermissionStatus;
import top.egon.cola.archetype.source.web.domain.user.enums.PermissionType;
import top.egon.cola.archetype.source.web.domain.user.vos.PermissionCode;

public record Permission(
        Long id,
        PermissionCode code,
        String name,
        PermissionType type,
        PermissionStatus status) {
}
