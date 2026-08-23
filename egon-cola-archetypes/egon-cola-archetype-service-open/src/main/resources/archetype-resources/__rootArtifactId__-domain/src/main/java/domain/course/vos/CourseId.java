#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.course.vos;

import ${package}.domain.common.EvaluationDomainErrorCode;
import ${package}.domain.common.EvaluationDomainException;

public record CourseId(long value) {

    public CourseId {
        if (value <= 0) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED, "course id must be positive");
        }
    }
}
