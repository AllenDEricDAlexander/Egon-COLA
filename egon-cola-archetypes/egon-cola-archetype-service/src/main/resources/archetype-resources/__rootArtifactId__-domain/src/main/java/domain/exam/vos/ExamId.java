#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.exam.vos;

import ${package}.common.utils.EvaluationIdUtils;
import ${package}.domain.common.EvaluationDomainErrorCode;
import ${package}.domain.common.EvaluationDomainException;

public record ExamId(String value) {

    public ExamId {
        if (value == null || value.isBlank()) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.VALIDATION_FAILED, "exam id must not be blank");
        }
    }

    public static ExamId newId() {
        return new ExamId(EvaluationIdUtils.nextId());
    }
}
