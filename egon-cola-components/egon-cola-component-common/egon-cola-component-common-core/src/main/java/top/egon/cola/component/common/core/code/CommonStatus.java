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
    CONCURRENCY_ERROR(409000, "CONCURRENCY_ERROR", "concurrency error"),
    VALIDATION_ERROR(422000, "VALIDATION_ERROR", "validation error"),
    TOO_MANY_REQUESTS(429000, "TOO_MANY_REQUESTS", "too many requests"),

    SYSTEM_ERROR(500000, "SYSTEM_ERROR", "system error"),
    REMOTE_CALL_ERROR(510000, "REMOTE_CALL_ERROR", "remote call error"),
    MIDDLEWARE_ERROR(520000, "MIDDLEWARE_ERROR", "middleware error"),

    BUSINESS_ERROR(600000, "BUSINESS_ERROR", "business error");

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
