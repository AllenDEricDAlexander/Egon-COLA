package top.egon.cola.archetype.source.serviceopen.domain.exam.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Exam lifecycle values with an explicit, stable code. */
public enum ExamStatus implements EgonEnum {

    DRAFT(0, "exam is a draft"),
    PUBLISHED(1, "exam is published"),
    CLOSED(2, "exam is closed");

    private final int code;

    private final String message;

    ExamStatus(int code, String message) {
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
