#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.domain.common;

public final class EvaluationPortException extends EvaluationDomainException {

    private final String operation;

    public EvaluationPortException(String operation, String message, Throwable cause) {
        super(EvaluationDomainErrorCode.PORT_FAILURE, message, cause);
        this.operation = operation;
    }

    public String operation() {
        return operation;
    }
}
