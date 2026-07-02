package top.egon.fable-web.infrastructure.repo.user.converter;

import top.egon.fable-web.domain.entities.user.User;
import top.egon.fable-web.domain.enums.UserStatus;
import top.egon.fable-web.infrastructure.repo.user.po.UserPo;

import java.time.LocalDateTime;
import java.util.List;

public final class UserPoConverter {
    private UserPoConverter() {
    }

    public static UserPo toPo(User user) {
        return new UserPo(user.getId(), user.getName(), user.getEmail(), user.getStatus().name(), LocalDateTime.now());
    }

    public static User toEntity(UserPo userPo, List<String> schoolClassIds) {
        return User.restore(
                userPo.getId(),
                userPo.getName(),
                userPo.getEmail(),
                UserStatus.valueOf(userPo.getStatus()),
                schoolClassIds);
    }
}
