package ${package}.infrastructure.repo.user.converter;

import ${package}.domain.entities.user.User;
import ${package}.domain.enums.user.UserStatus;
import ${package}.domain.vos.user.UserId;
import ${package}.domain.vos.user.RoleCode;
import ${package}.infrastructure.repo.user.po.UserPO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component("userPOConverter")
public class UserPOConverter {

    public UserPO toPO(User user) {
        return new UserPO(
            user.id().value(), user.name(), user.email(), user.status().name(), LocalDateTime.now());
    }

    public User toEntity(UserPO userPO, List<RoleCode> roleCodes) {
        return User.restore(
            new UserId(userPO.getId()), userPO.getName(), userPO.getEmail(),
            UserStatus.valueOf(userPO.getStatus()), roleCodes);
    }
}
