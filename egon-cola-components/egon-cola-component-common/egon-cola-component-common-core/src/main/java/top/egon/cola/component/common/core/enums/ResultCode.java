package top.egon.cola.component.common.core.enums;

/**
 * Common result code definitions shared by Egon COLA components.
 */
public enum ResultCode implements ErrorStatus {

    SUCCESS(10000, "success"),

    INVALID_USER_KEY(10001, "key is invalid or expired"),
    INSUFFICIENT_PRIVILEGES(10012, "permission denied"),
    QPS_HAS_EXCEEDED_THE_LIMIT(10019, "service QPS limit exceeded"),
    INVALID_REQUEST(10026, "invalid request"),

    INVALID_PARAMS(20000, "request parameters are invalid"),
    MISSING_REQUIRED_PARAMS(20001, "required parameter is missing"),
    ILLEGAL_REQUEST(20002, "request protocol is illegal"),
    UNKNOWN_ERROR(20003, "unknown error"),

    BAD_REQUEST(400000, "bad request"),
    UNAUTHORIZED(401000, "unauthorized"),
    FORBIDDEN(403000, "forbidden"),
    NOT_FOUND(404000, "not found"),
    CONCURRENCY_ERROR(409000, "concurrency error"),
    VALIDATION_ERROR(422000, "validation error"),
    TOO_MANY_REQUESTS(429000, "too many requests"),

    SYSTEM_ERROR(500000, "system error"),
    REMOTE_CALL_ERROR(510000, "remote call error"),
    MIDDLEWARE_ERROR(520000, "middleware error"),
    BUSINESS_ERROR(600000, "business error"),

    QUOTA_PLAN_RUN_OUT(70000, "quota plan run out"),
    SERVICE_EXPIRED(70001, "service expired"),
    ABROAD_QUOTA_PLAN_RUN_OUT(70002, "abroad quota plan run out");

    private final int code;

    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getStatus() {
        return name();
    }

    @Override
    public String getMessage() {
        return message;
    }
}
