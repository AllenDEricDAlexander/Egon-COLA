#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.course.vos;

import ${package}.common.utils.EvaluationIdUtils;
import ${package}.domain.common.EvaluationDomainErrorCode;
import ${package}.domain.common.EvaluationDomainException;

public record CourseId(String value) {

    public CourseId {
        if (value == null || value.isBlank()) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED, "course id must not be blank");
        }
    }

    public static CourseId newId() {
        return new CourseId(EvaluationIdUtils.nextId());
    }
}
