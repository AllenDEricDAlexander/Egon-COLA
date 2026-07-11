package ${package}.domain.service.user.impl;

import ${package}.domain.entities.user.User;
import ${package}.domain.enums.user.UserStatus;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;
import ${package}.domain.service.user.UserDomainService;
import ${package}.domain.vos.user.UserId;

public final class UserDomainServiceImpl implements UserDomainService {

    @Override
    public User create(UserId userId, String name, String email) {
        return new User(userId, name, email, UserStatus.ACTIVE);
    }

    @Override
    public User assignClass(User user, String schoolClassId) {
        if (user.hasSchoolClass(schoolClassId)) {
            throw new OrganizationDomainException(
                OrganizationDomainErrorCode.DOMAIN_REJECTED, "user already assigned to school class");
        }
        user.assignToClass(schoolClassId);
        return user;
    }
}
