#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.common.exceptions;

public final class EvaluationNotFoundException extends EvaluationBizException {

    public EvaluationNotFoundException(EvaluationError code, String message) {
        super(code, message);
    }
}
