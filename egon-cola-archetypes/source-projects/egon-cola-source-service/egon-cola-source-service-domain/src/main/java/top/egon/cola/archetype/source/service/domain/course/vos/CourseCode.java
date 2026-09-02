package top.egon.cola.archetype.source.service.domain.course.vos;

import top.egon.cola.archetype.source.service.common.constants.EvaluationConstants;
import top.egon.cola.archetype.source.service.domain.common.EvaluationDomainErrorCode;
import top.egon.cola.archetype.source.service.domain.common.EvaluationDomainException;
import java.util.Locale;

public record CourseCode(String value) {

    public CourseCode {
        if (value == null || value.isBlank()) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED, "course code must not be blank");
        }
        value = value.trim().toUpperCase(Locale.ROOT);
        if (value.length() > EvaluationConstants.MAX_COURSE_CODE_LENGTH) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED, "course code is too long");
        }
    }
}
