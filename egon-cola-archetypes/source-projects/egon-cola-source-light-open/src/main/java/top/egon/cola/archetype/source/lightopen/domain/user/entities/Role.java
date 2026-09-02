package top.egon.cola.archetype.source.lightopen.domain.user.entities;

import top.egon.cola.archetype.source.lightopen.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.lightopen.domain.user.vos.RoleCode;

import java.util.Objects;

public record Role(RoleCode code, String name, RoleStatus status) {
    public Role {
        Objects.requireNonNull(code, "code must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(status, "status must not be null");
    }
}
