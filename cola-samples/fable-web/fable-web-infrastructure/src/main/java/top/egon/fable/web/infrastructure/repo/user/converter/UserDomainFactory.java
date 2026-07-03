package top.egon.fable.web.infrastructure.repo.user.converter;

import top.egon.fable.web.domain.entities.user.User;
import top.egon.fable.web.domain.enums.UserStatus;
import top.egon.fable.web.infrastructure.repo.user.po.UserPo;
import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("userDomainFactory")
public class UserDomainFactory {
    @ObjectFactory
    public User create(UserPo userPo) {
        return User.restore(userPo.getId(), userPo.getName(), userPo.getEmail(), UserStatus.valueOf(userPo.getStatus()), List.of());
    }
}
