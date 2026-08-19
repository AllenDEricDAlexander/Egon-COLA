package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleEmptyTreeException extends RuleEngineException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleEmptyTreeException(String message) {
        super(message);
    }

    public RuleEmptyTreeException(String message, Throwable cause) {
        super(message, cause);
    }
}
