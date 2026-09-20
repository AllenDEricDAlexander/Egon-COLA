package top.egon.cola.component.ruleengine.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import java.io.Serial;

public class RuleEngineException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RuleEngineException(String message) {
        super(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getStatus(), message);
    }

    public RuleEngineException(String message, Throwable cause) {
        super(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getStatus(), message, cause);
    }
}
