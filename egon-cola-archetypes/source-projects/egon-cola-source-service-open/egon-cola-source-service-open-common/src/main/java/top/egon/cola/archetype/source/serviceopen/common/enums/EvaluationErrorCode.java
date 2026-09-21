package top.egon.cola.archetype.source.serviceopen.common.enums;

/** Facade-facing evaluation error codes. */
public enum EvaluationErrorCode implements EvaluationError {

    COURSE_NOT_FOUND(0, "course not found"),
    COURSE_CODE_DUPLICATED(1, "course code already exists"),
    SCHEDULE_CONFLICT(2, "schedule conflicts with an existing entry"),
    EXAM_NOT_FOUND(3, "exam not found"),
    EXAM_PAPER_NOT_FOUND(4, "exam paper not found"),
    EXAM_NOT_PUBLISHABLE(5, "exam is not publishable"),
    SCORE_NOT_FOUND(6, "score not found"),
    SCORE_DUPLICATED(7, "score already recorded"),
    VALIDATION_FAILED(8, "request validation failed"),
    INFRASTRUCTURE_FAILURE(9, "infrastructure failure");

    private final int code;

    private final String message;

    EvaluationErrorCode(int code, String message) {
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
