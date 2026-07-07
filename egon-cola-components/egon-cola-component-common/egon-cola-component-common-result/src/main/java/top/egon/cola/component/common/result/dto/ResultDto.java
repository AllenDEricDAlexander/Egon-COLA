package top.egon.cola.component.common.result.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * External single-object response DTO.
 */
public class ResultDto<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean success;

    private int code;

    private String status;

    private String message;

    private T data;

    private String traceId;

    private Long timestamp;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
