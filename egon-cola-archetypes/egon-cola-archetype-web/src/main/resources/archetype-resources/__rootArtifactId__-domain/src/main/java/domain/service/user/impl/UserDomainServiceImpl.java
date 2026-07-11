package ${package}.domain.service.user.impl;

import ${package}.domain.entities.user.User;
import ${package}.domain.enums.user.UserStatus;
import ${package}.domain.service.user.UserDomainService;
import ${package}.domain.vos.user.UserId;

public final class UserDomainServiceImpl implements UserDomainService {

    @Override
    public User create(UserId userId, String name, String email) {
        return new User(userId, name, email, UserStatus.ACTIVE);
    }

}
