#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.vos.exam;

import ${package}.domain.common.EvaluationDomainErrorCode;
import ${package}.domain.common.EvaluationDomainException;

public record ScoreValue(int value) {

    public ScoreValue {
        if (value < 0) {
            throw new EvaluationDomainException(
                    EvaluationDomainErrorCode.SCORE_OUT_OF_RANGE, "score must not be negative");
        }
    }
}
