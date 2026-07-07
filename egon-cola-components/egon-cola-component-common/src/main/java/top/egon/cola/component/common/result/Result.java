package top.egon.cola.component.common.result;

import top.egon.cola.component.common.exception.BusinessException;
import top.egon.cola.component.common.exception.ErrorCodes;
import top.egon.cola.component.common.exception.SystemException;
import top.egon.cola.component.common.trace.TraceContext;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用单对象返回结果，自动携带当前 MDC 中的 traceId。
 */
public class Result<T> implements Serializable {


    @Serial
    private static final long serialVersionUID = 1L;

    private boolean success;

    private int code;

    private String message;

    private T data;

    private String traceId;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.success = true;
        result.code = ErrorCodes.SUCCESS.getCode();
        result.message = ErrorCodes.SUCCESS.getMessage();
        result.data = data;
        result.traceId = TraceContext.getTraceId();
        return result;
    }

    public static <T> Result<T> failure(int code, String message) {
        Result<T> result = new Result<>();
        result.success = false;
        result.code = code;
        result.message = message;
        result.traceId = TraceContext.getTraceId();
        return result;
    }

    public static <T> Result<T> failure(Throwable throwable) {
        if (throwable instanceof BusinessException exception) {
            return failure(exception.getCode(), exception.getMessage());
        }
        if (throwable instanceof SystemException exception) {
            return failure(exception.getCode(), exception.getMessage());
        }
        String message = throwable == null ? "system error" : throwable.getMessage();
        return failure(ErrorCodes.SYSTEM_ERROR.getCode(), message);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
