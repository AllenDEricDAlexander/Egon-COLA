package top.egon.cola.archetype.source.webopen.infrastructure.user.repo.converter;

import top.egon.cola.archetype.source.webopen.domain.user.entities.User;
import top.egon.cola.archetype.source.webopen.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.webopen.domain.user.vos.RoleCode;
import top.egon.cola.archetype.source.webopen.domain.user.vos.UserId;
import top.egon.cola.archetype.source.webopen.infrastructure.user.repo.po.UserPO;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.converter.BaseConverter;

import java.util.List;

@Component("userPOConverter")
public final class UserPOConverter implements BaseConverter<User, UserPO> {
    @Override
    public UserPO toTarget(User user) {
        UserPO target = UserPO.builder()
                .name(user.name())
                .email(user.email())
                .status(user.status().name())
                .build();
        target.setId(user.id().value());
        return target;
    }

    @Override
    public User toSource(UserPO target) {
        return toEntity(target, List.of());
    }

    public User toEntity(UserPO target, List<RoleCode> roleCodes) {
        return User.restore(new UserId(target.getId()), target.getName(), target.getEmail(),
                UserStatus.valueOf(target.getStatus()), roleCodes);
    }

    public UserPO toPO(User user) {
        return toTarget(user);
    }
}
