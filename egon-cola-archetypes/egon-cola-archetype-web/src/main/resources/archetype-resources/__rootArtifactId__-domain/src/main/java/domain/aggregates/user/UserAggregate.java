package ${package}.domain.aggregates.user;

import ${package}.domain.entities.user.Role;
import ${package}.domain.entities.user.User;
import ${package}.domain.enums.user.RoleStatus;
import ${package}.domain.enums.user.UserStatus;
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
