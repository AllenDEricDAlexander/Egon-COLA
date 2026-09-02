package top.egon.cola.archetype.source.light.domain.user.vos;

import top.egon.cola.archetype.source.light.domain.user.entities.User;
import top.egon.cola.archetype.source.light.domain.user.enums.UserStatus;

public record UserSnapshot(Long id, String name, String email, UserStatus status) {
    public static UserSnapshot from(User user) {
        return new UserSnapshot(user.id().value(), user.name(), user.email(), user.status());
    }

}
