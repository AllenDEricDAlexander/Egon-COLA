package top.egon.cola.component.common.core.code;

/**
 * Common enterprise status definitions shared by Egon COLA components.
 */
public enum CommonStatus implements ErrorStatus {

    SUCCESS(0, "SUCCESS", "success"),
    BAD_REQUEST(400000, "BAD_REQUEST", "bad request"),
    UNAUTHORIZED(401000, "UNAUTHORIZED", "unauthorized"),
    FORBIDDEN(403000, "FORBIDDEN", "forbidden"),
    NOT_FOUND(404000, "NOT_FOUND", "not found"),
    BUSINESS_ERROR(500001, "BUSINESS_ERROR", "business error"),
    SYSTEM_ERROR(500000, "SYSTEM_ERROR", "system error");

    private final int code;

    private final String status;

    private final String message;

    CommonStatus(int code, String status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
