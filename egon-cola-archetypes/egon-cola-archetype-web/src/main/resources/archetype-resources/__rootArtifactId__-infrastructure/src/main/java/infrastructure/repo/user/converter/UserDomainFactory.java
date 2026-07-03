package ${package}.infrastructure.repo.user.converter;

import ${package}.domain.entities.user.User;
import ${package}.domain.enums.UserStatus;
import ${package}.infrastructure.repo.user.po.UserPo;
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
