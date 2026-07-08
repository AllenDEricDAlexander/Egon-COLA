package top.egon.cola.component.ruleengine.exception;

import java.io.Serial;

public class RuleMaxStepsExceededException extends RuleEngineException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleMaxStepsExceededException(String message) {
        super(message);
    }

    public RuleMaxStepsExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
