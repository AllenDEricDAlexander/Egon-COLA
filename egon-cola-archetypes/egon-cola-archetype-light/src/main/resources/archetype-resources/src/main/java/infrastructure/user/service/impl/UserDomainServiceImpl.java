package ${package}.infrastructure.user.service.impl;

import ${package}.common.utils.IdUtils;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.service.UserDomainService;
import ${package}.domain.user.vos.UserId;
import org.springframework.stereotype.Service;

@Service("userDomainService")
public class UserDomainServiceImpl implements UserDomainService {
    @Override
    public User createUser(String externalId, String name, String email) {
        return new User(
                new UserId(IdUtils.nextId()), externalId, name, email, UserStatus.ACTIVE);
    }
}
