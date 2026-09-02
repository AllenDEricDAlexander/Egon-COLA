package top.egon.cola.archetype.source.serviceopen.domain.course.vos;

import top.egon.cola.archetype.source.serviceopen.common.constants.EvaluationConstants;
import top.egon.cola.archetype.source.serviceopen.domain.common.EvaluationDomainErrorCode;
import top.egon.cola.archetype.source.serviceopen.domain.common.EvaluationDomainException;
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
