package ${package}.domain.user.validators;

import ${package}.domain.user.entities.Permission;
import ${package}.domain.user.entities.Role;
import ${package}.domain.user.entities.User;
import ${package}.domain.user.enums.PermissionStatus;
import ${package}.domain.user.enums.RoleStatus;
import ${package}.domain.user.enums.UserStatus;
import ${package}.domain.user.exceptions.UserDomainException;

public final class UserDomainValidator {
    private UserDomainValidator() {
    }

    public static void requireActive(User user) {
        if (user.status() != UserStatus.ACTIVE) {
            throw new UserDomainException("USER_NOT_ACTIVE", "User must be active");
        }
    }

    public static void requireActive(Role role) {
        if (role.status() != RoleStatus.ACTIVE) {
            throw new UserDomainException("ROLE_NOT_ACTIVE", "Role must be active");
        }
    }

    public static void requireAssignable(Role role, Permission permission) {
        requireActive(role);
        if (permission.status() != PermissionStatus.ACTIVE) {
            throw new UserDomainException("PERMISSION_NOT_ACTIVE", "Permission must be active");
        }
    }
}
