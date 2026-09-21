package top.egon.cola.archetype.source.service.infrastructure.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import top.egon.cola.archetype.source.service.common.exception.EvaluationPortException;
import top.egon.cola.component.common.core.validation.BaseValidator;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.util.Locale;

/**
 * Read-only translation of a persistence integrity failure into the port exception. It inspects the
 * original cause chain only; no statement is re-issued and no cache or MQ side effect is written.
 */
@Component("evaluationPersistenceValidator")
@RequiredArgsConstructor
@Slf4j
public class EvaluationPersistenceValidator extends BaseValidator {

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    protected ValidationUtils getValidationUtils() {
        return validationUtils;
    }

    public EvaluationPortException translate(String operation, DataIntegrityViolationException failure) {
        String constraint = constraintName(failure);
        String message = constraint == null
                ? "persistence operation failed"
                : "persistence constraint violated: " + constraint;
        return new EvaluationPortException(operation, message, failure);
    }

    private String constraintName(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                for (String constraint : new String[] {
                        "uk_course_code", "uk_exam_paper_exam", "uk_score_exam_student"}) {
                    if (lower.contains(constraint)) {
                        return constraint;
                    }
                }
            }
            current = current.getCause();
        }
        return null;
    }
}
