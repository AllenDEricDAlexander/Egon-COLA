#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.facade.exceptions;

import ${package}.facade.enums.EvaluationFacadeErrorCode;

public final class EvaluationFacadeException extends RuntimeException {

    private final EvaluationFacadeErrorCode code;

    public EvaluationFacadeException(EvaluationFacadeErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public EvaluationFacadeErrorCode code() {
        return code;
    }
}
