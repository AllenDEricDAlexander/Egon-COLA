package top.egon.cola.component.common.result.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Internal single-object operation result model.
 */
public class ResultModel<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean success;

    private int code;

    private String status;

    private String message;

    private T data;

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
}
