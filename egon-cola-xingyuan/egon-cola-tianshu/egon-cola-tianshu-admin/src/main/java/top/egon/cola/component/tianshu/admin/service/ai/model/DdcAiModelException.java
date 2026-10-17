package top.egon.cola.component.tianshu.admin.service.ai.model;

import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunErrorCodeEnum;

import java.util.Objects;

/**
 * 模型调用失败；消息不含提示正文或令牌。
 * / Model call failure; the message never contains prompt text or tokens.
 */
public class DdcAiModelException extends RuntimeException {

    private final DdcAiRunErrorCodeEnum code;

    private final int httpStatus;

    public DdcAiModelException(DdcAiRunErrorCodeEnum code, int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
        this.httpStatus = httpStatus;
    }

    public DdcAiRunErrorCodeEnum getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
