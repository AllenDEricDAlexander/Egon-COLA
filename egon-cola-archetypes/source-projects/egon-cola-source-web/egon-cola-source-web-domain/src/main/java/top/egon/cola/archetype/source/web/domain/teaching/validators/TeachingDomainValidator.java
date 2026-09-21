package top.egon.cola.archetype.source.web.domain.teaching.validators;

import jakarta.validation.Validation;
import top.egon.cola.archetype.source.web.common.enums.OrganizationDomainErrorCode;
import top.egon.cola.archetype.source.web.common.exception.OrganizationDomainException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Teaching relation rules; the native-constraint facade is inherited from the common base. */
public class TeachingDomainValidator extends BaseValidator {

    @Override
    protected ValidationUtils getValidationUtils() {
        return JakartaValidation.UTILS;
    }

    public static String normalizeName(String name, String field) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty() || normalized.length() > 120) {
            throw new OrganizationDomainException(
                    OrganizationDomainErrorCode.DOMAIN_REJECTED,
                    field + " must not be blank or exceed 120 characters");
        }
        return normalized;
    }

    /** The Domain layer stays framework-free, so the Jakarta bootstrap is created on first use only. */
    private static final class JakartaValidation {
        private static final ValidationUtils UTILS =
                new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator());
    }
}
