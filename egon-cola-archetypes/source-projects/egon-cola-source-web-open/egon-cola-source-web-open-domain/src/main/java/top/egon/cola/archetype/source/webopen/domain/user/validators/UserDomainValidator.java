package top.egon.cola.archetype.source.webopen.domain.user.validators;

import jakarta.validation.Validation;
import top.egon.cola.archetype.source.webopen.common.enums.OrganizationDomainErrorCode;
import top.egon.cola.archetype.source.webopen.common.exception.OrganizationDomainException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/** User invariants; the native-constraint facade is inherited from the common base. */
public class UserDomainValidator extends BaseValidator {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Override
    protected ValidationUtils getValidationUtils() {
        return JakartaValidation.UTILS;
    }

    public static String normalizeName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isBlank() || name.length() > 120) {
            throw new OrganizationDomainException(
                    OrganizationDomainErrorCode.INVALID_USER_NAME, "user name must contain 1 to 120 characters");
        }
        return name;
    }

    public static String normalizeEmail(String raw) {
        String email = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 160 || !EMAIL.matcher(email).matches()) {
            throw new OrganizationDomainException(
                    OrganizationDomainErrorCode.INVALID_EMAIL, "invalid user email");
        }
        return email;
    }

    /** The Domain layer stays framework-free, so the Jakarta bootstrap is created on first use only. */
    private static final class JakartaValidation {
        private static final ValidationUtils UTILS =
                new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());
    }
}
