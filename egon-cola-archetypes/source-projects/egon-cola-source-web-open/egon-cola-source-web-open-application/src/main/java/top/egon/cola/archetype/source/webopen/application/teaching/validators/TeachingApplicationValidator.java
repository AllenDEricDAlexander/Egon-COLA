package top.egon.cola.archetype.source.webopen.application.teaching.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.archetype.source.webopen.application.context.OrganizationRequestContextHolder;
import top.egon.cola.archetype.source.webopen.common.enums.OrganizationFailureType;
import top.egon.cola.archetype.source.webopen.common.exception.OrganizationApplicationException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Teaching use-case guard; it never writes Redis, MQ or persistence state. */
@RequiredArgsConstructor
@Slf4j
public class TeachingApplicationValidator extends BaseValidator {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public void requireTeachingAdmin() {
        if (OrganizationRequestContextHolder.current()
                .filter(context -> context.hasRole("TEACHING_ADMIN"))
                .isEmpty()) {
            throw new OrganizationApplicationException(
                    OrganizationFailureType.FORBIDDEN, "ORG_FORBIDDEN", "TEACHING_ADMIN is required");
        }
    }
}
