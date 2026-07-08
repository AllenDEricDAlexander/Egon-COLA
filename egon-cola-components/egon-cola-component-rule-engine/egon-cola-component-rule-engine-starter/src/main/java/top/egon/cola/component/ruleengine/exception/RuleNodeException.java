package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleNodeException extends RuleEngineException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleNodeException(String message) {
        super(message);
    }

    public RuleNodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
