package ${package}.domain.user.vos;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;

public record UserSnapshot(String id, String name, String email, UserStatus status) {
    public static UserSnapshot from(User user) {
        return new UserSnapshot(user.id().value(), user.name(), user.email(), user.status());
    }
}
