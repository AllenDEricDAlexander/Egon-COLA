package top.egon.cola.archetype.source.serviceopen.common.enums;

import top.egon.cola.component.common.core.enums.ErrorStatus;

/** Stable wire code for an application use-case rejection. */
public enum ApplicationErrorCode implements ErrorStatus {

    COURSE_NOT_FOUND(0, "course not found"),
    COURSE_CODE_DUPLICATED(1, "course code already exists"),
    EXAM_NOT_FOUND(2, "exam not found"),
    EXAM_PAPER_NOT_FOUND(3, "exam paper not found"),
    SCORE_NOT_FOUND(4, "score not found"),
    VALIDATION_FAILED(5, "request validation failed"),
    BUSINESS_REJECTED(6, "business rule rejected the request"),
    INFRASTRUCTURE_FAILURE(7, "infrastructure failure");

    private final int code;

    private final String message;

    ApplicationErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getStatus() {
        return name();
    }
}
