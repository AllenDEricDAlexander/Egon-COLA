package top.egon.cola.archetype.source.light.domain.user.vos;

import java.time.Instant;

public record UserEvent(String type, Long aggregateId, Instant occurredAt) {
    public static UserEvent created(Long userId) {
        return new UserEvent("user.created", userId, Instant.now());
    }

    public static UserEvent roleAssigned(Long userId) {
        return new UserEvent("user.role-assigned", userId, Instant.now());
    }

    public static UserEvent permissionGranted(Long roleId) {
        return new UserEvent("authorization.permission-granted", roleId, Instant.now());
    }

    public static UserEvent permissionGranted() {
        return new UserEvent("authorization.permission-granted", null, Instant.now());
    }

}
