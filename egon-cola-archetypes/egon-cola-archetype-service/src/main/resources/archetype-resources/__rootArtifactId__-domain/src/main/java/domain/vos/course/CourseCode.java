#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.vos.course;

import ${package}.common.constants.EvaluationConstants;
import ${package}.domain.common.EvaluationDomainErrorCode;
import ${package}.domain.common.EvaluationDomainException;
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
