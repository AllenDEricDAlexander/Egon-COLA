package top.egon.cola.component.common.enums;

/**
 * Common result code definitions shared by Egon COLA components.
 */
public enum ResultCode implements ErrorStatus {

    SUCCESS(10000, "SUCCESS", "success"),

    INVALID_USER_KEY(10001, "INVALID_USER_KEY", "key is invalid or expired"),
    INSUFFICIENT_PRIVILEGES(10012, "INSUFFICIENT_PRIVILEGES", "permission denied"),
    QPS_HAS_EXCEEDED_THE_LIMIT(10019, "QPS_HAS_EXCEEDED_THE_LIMIT", "service QPS limit exceeded"),
    INVALID_REQUEST(10026, "INVALID_REQUEST", "invalid request"),

    INVALID_PARAMS(20000, "INVALID_PARAMS", "request parameters are invalid"),
    MISSING_REQUIRED_PARAMS(20001, "MISSING_REQUIRED_PARAMS", "required parameter is missing"),
    ILLEGAL_REQUEST(20002, "ILLEGAL_REQUEST", "request protocol is illegal"),
    UNKNOWN_ERROR(20003, "UNKNOWN_ERROR", "unknown error"),

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
    BUSINESS_ERROR(600000, "BUSINESS_ERROR", "business error"),

    QUOTA_PLAN_RUN_OUT(70000, "QUOTA_PLAN_RUN_OUT", "quota plan run out"),
    SERVICE_EXPIRED(70001, "SERVICE_EXPIRED", "service expired"),
    ABROAD_QUOTA_PLAN_RUN_OUT(70002, "ABROAD_QUOTA_PLAN_RUN_OUT", "abroad quota plan run out");

    private final int code;

    private final String status;

    private final String message;

    ResultCode(int code, String status, String message) {
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
