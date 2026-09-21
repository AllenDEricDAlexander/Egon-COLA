package top.egon.cola.archetype.source.service.domain.exam.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Exam paper lifecycle values with an explicit, stable code. */
public enum ExamPaperStatus implements EgonEnum {

    DRAFT(0, "paper is a draft"),
    PUBLISHED(1, "paper is published");

    private final int code;

    private final String message;

    ExamPaperStatus(int code, String message) {
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
}
