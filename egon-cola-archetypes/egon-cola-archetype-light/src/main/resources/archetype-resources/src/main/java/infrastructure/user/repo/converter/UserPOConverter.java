package ${package}.infrastructure.user.repo.converter;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.vos.UserId;
import ${package}.infrastructure.user.repo.po.UserPO;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class UserPOConverter {
    public UserPO toPO(User user) {
        return new UserPO(
                user.id().value(), user.externalId(), user.name(), user.email(),
                user.status().name(), Instant.now());
    }

    public User toDomain(UserPO user) {
        return new User(
                new UserId(user.getId()), user.getExternalId(), user.getName(), user.getEmail(),
                UserStatus.valueOf(user.getStatus()));
    }
}
