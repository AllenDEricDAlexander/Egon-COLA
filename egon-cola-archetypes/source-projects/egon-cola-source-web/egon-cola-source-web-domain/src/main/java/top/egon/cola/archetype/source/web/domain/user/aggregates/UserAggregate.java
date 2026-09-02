package top.egon.cola.archetype.source.web.domain.user.aggregates;

import top.egon.cola.archetype.source.web.domain.user.entities.Role;
import top.egon.cola.archetype.source.web.domain.user.entities.User;
import top.egon.cola.archetype.source.web.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.web.domain.user.enums.UserStatus;
import top.egon.cola.archetype.source.web.domain.exceptions.OrganizationDomainErrorCode;
import top.egon.cola.archetype.source.web.domain.exceptions.OrganizationDomainException;

public final class UserAggregate {

    private final User user;

    public UserAggregate(User user) { this.user = user; }

    public void assignRole(Role role) {
        if (user.status() == UserStatus.DISABLED) {
            throw rejected(OrganizationDomainErrorCode.USER_DISABLED, "disabled user cannot receive roles");
        }
        if (role.status() == RoleStatus.ARCHIVED) {
            throw rejected(OrganizationDomainErrorCode.ROLE_ARCHIVED, "archived role cannot be assigned");
        }
        if (user.roleCodes().contains(role.code())) {
            throw rejected(OrganizationDomainErrorCode.DUPLICATE_ROLE_ASSIGNMENT, "role already assigned");
        }
        user.assignRole(role.code());
    }

    public User user() { return user; }

    private static OrganizationDomainException rejected(OrganizationDomainErrorCode code, String message) {
        return new OrganizationDomainException(code, message);
    }
}
