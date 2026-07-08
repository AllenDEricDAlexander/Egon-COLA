package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleConfigException extends RuleEngineException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleConfigException(String message) {
        super(message);
    }

    public RuleConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
