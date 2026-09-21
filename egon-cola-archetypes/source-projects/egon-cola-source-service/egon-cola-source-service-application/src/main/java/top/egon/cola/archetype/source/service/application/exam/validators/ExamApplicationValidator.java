package top.egon.cola.archetype.source.service.application.exam.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.service.common.enums.ApplicationErrorCode;
import top.egon.cola.archetype.source.service.common.exception.ApplicationException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/** Exam and score use-case guard; it never writes Redis, MQ or persistence state. */
@Component("examApplicationValidator")
@RequiredArgsConstructor
@Slf4j
public class ExamApplicationValidator extends BaseValidator {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public void positive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new ApplicationException(
                    ApplicationErrorCode.VALIDATION_FAILED, field + " must be positive");
        }
    }
}
