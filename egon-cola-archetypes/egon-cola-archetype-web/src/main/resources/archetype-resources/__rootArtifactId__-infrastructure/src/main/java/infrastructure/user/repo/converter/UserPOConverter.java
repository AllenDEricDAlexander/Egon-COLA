package ${package}.infrastructure.user.repo.converter;

import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.vos.UserId;
import ${package}.domain.user.vos.RoleCode;
import ${package}.infrastructure.user.repo.po.UserPO;
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
