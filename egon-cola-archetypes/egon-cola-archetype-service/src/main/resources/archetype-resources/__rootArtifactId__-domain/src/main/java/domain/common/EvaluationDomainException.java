#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.common;

import ${package}.common.exceptions.EvaluationBizException;

public class EvaluationDomainException extends EvaluationBizException {

    public EvaluationDomainException(EvaluationDomainErrorCode code, String message) {
        super(code, message);
    }

    public EvaluationDomainException(
            EvaluationDomainErrorCode code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
