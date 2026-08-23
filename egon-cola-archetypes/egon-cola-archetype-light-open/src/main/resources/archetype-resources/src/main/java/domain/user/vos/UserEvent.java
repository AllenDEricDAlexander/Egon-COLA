package ${package}.domain.user.vos;

import java.time.Instant;

public record UserEvent(String type, String aggregateId, Instant occurredAt) {
    public static UserEvent created(long userId) {
        return new UserEvent("user.created", Long.toString(userId), Instant.now());
    }

    public static UserEvent roleAssigned(long userId) {
        return new UserEvent("user.role-assigned", Long.toString(userId), Instant.now());
    }

    public static UserEvent permissionGranted(String roleCode) {
        return new UserEvent("authorization.permission-granted", roleCode, Instant.now());
    }
}
