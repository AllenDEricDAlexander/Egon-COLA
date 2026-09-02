package top.egon.cola.archetype.source.service.common.exceptions;

public enum EvaluationErrorCode implements EvaluationError {
    COURSE_NOT_FOUND,
    COURSE_CODE_DUPLICATED,
    SCHEDULE_CONFLICT,
    EXAM_NOT_FOUND,
    EXAM_PAPER_NOT_FOUND,
    EXAM_NOT_PUBLISHABLE,
    SCORE_NOT_FOUND,
    SCORE_DUPLICATED,
    VALIDATION_FAILED,
    INFRASTRUCTURE_FAILURE;

    @Override
    public String code() {
        return name();
    }
}
