package top.egon.cola.archetype.source.webopen.domain.validators;

import jakarta.validation.Validation;
import top.egon.cola.archetype.source.webopen.common.enums.OrganizationDomainErrorCode;
import top.egon.cola.archetype.source.webopen.common.exception.OrganizationDomainException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/** Code normalization shared by the value objects; the native-constraint facade is inherited. */
public class OrganizationCodeValidator extends BaseValidator {

    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");

    @Override
    protected ValidationUtils getValidationUtils() {
        return JakartaValidation.UTILS;
    }

    public static String normalize(String raw) {
        String normalized = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(normalized).matches()) {
            throw new OrganizationDomainException(
                    OrganizationDomainErrorCode.INVALID_CODE,
                    "code must match [A-Z][A-Z0-9_]{1,63}");
        }
        return normalized;
    }

    /** The Domain layer stays framework-free, so the Jakarta bootstrap is created on first use only. */
    private static final class JakartaValidation {
        private static final ValidationUtils UTILS =
                new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());
    }
}
