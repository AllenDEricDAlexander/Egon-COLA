package top.egon.cola.archetype.source.service.application.course.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.service.common.enums.ApplicationErrorCode;
import top.egon.cola.archetype.source.service.common.exception.ApplicationException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Course use-case guard; it never writes Redis, MQ or persistence state. */
@Component("courseApplicationValidator")
@RequiredArgsConstructor
@Slf4j
public class CourseApplicationValidator extends BaseValidator {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public void require(boolean condition, String message) {
        if (!condition) {
            throw new ApplicationException(ApplicationErrorCode.VALIDATION_FAILED, message);
        }
    }
}
