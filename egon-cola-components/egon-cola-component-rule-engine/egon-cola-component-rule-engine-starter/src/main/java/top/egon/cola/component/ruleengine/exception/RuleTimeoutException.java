package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleTimeoutException extends RuleEngineException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleTimeoutException(String message) {
        super(message);
    }

    public RuleTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
