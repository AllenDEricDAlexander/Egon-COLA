package top.egon.cola.component.agentflow.common.exception;

import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.CommonException;

import java.io.Serial;

/** Base unchecked exception for the Agent Flow component. */
public class AgentFlowException extends CommonException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AgentFlowException(String message) {
        super(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getStatus(), message);
    }

    public AgentFlowException(String message, Throwable cause) {
        super(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getStatus(), message, cause);
    }
}
