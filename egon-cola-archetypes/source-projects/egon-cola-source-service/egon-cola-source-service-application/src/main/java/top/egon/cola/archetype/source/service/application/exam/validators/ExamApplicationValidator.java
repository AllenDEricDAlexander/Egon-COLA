package top.egon.cola.archetype.source.service.application.exam.validators;

import top.egon.cola.archetype.source.service.application.exceptions.ApplicationErrorCode;
import top.egon.cola.archetype.source.service.application.exceptions.ApplicationException;
import org.springframework.stereotype.Component;

@Component
public class ExamApplicationValidator {
    public void positive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new ApplicationException(
                    ApplicationErrorCode.VALIDATION_FAILED, field + " must be positive");
        }
    }
}
