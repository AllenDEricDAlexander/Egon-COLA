package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleRouteException extends RuleEngineException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleRouteException(String message) {
        super(message);
    }

    public RuleRouteException(String message, Throwable cause) {
        super(message, cause);
    }
}
