package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleEmptyChainException extends RuleEngineException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleEmptyChainException(String message) {
        super(message);
    }

    public RuleEmptyChainException(String message, Throwable cause) {
        super(message, cause);
    }
}
