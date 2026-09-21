package top.egon.cola.archetype.source.light.domain.user.validators;

import jakarta.validation.Validation;
import top.egon.cola.archetype.source.light.common.exception.UserDomainException;
import top.egon.cola.archetype.source.light.domain.user.entities.Permission;
import top.egon.cola.archetype.source.light.domain.user.entities.Role;
import top.egon.cola.archetype.source.light.domain.user.entities.User;
import top.egon.cola.archetype.source.light.domain.user.enums.PermissionStatus;
import top.egon.cola.archetype.source.light.domain.user.enums.RoleStatus;
import top.egon.cola.archetype.source.light.domain.user.enums.UserStatus;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** User invariants; the native-constraint facade is inherited from the common base. */
public class UserDomainValidator extends BaseValidator {

    @Override
    protected ValidationUtils getValidationUtils() {
        return JakartaValidation.UTILS;
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

    /** The Domain layer stays framework-free, so the Jakarta bootstrap is created on first use only. */
    private static final class JakartaValidation {
        private static final ValidationUtils UTILS =
                new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());
    }
}
