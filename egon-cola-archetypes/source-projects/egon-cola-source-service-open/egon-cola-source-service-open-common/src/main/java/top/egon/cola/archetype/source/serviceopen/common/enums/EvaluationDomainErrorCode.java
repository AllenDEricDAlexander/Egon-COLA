package top.egon.cola.archetype.source.serviceopen.common.enums;

/** Domain invariant violations raised inside the evaluation aggregates. */
public enum EvaluationDomainErrorCode implements EvaluationError {

    VALIDATION_FAILED(0, "request validation failed"),
    COURSE_INACTIVE(1, "course is not active"),
    COURSE_CODE_DUPLICATED(2, "course code already exists"),
    SCHEDULE_CONFLICT(3, "schedule conflicts with an existing entry"),
    EXAM_NOT_PUBLISHABLE(4, "exam is not publishable"),
    SCORE_OUT_OF_RANGE(5, "score is out of range"),
    SCORE_DUPLICATED(6, "score already recorded"),
    PORT_FAILURE(7, "domain port invocation failed");

    private final int code;

    private final String message;

    EvaluationDomainErrorCode(int code, String message) {
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
    public String code() {
        return name();
    }
}
