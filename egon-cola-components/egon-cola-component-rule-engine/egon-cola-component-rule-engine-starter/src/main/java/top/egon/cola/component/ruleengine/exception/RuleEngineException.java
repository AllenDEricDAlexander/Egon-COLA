package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleEngineException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleEngineException(String message) {
        super(message);
    }

    public RuleEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
