package top.egon.cola.archetype.source.serviceopen.domain.common;

import top.egon.cola.archetype.source.serviceopen.common.exceptions.EvaluationError;

public enum EvaluationDomainErrorCode implements EvaluationError {
    VALIDATION_FAILED,
    COURSE_INACTIVE,
    COURSE_CODE_DUPLICATED,
    SCHEDULE_CONFLICT,
    EXAM_NOT_PUBLISHABLE,
    SCORE_OUT_OF_RANGE,
    SCORE_DUPLICATED,
    PORT_FAILURE;

    @Override
    public String code() {
        return name();
    }
}
