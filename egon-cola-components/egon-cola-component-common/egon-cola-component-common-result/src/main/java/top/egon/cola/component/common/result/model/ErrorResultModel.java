package top.egon.cola.component.common.result.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * Internal error description model.
 */
public class ErrorResultModel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int code;

    private String status;

    private String message;

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
}
