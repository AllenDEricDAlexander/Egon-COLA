package ${package}.domain.user.aggregates;

import ${package}.domain.user.entities.Role;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.exceptions.OrganizationDomainErrorCode;
import ${package}.domain.exceptions.OrganizationDomainException;

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
