package top.egon.cola.archetype.source.service.domain.exam.enums;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Score lifecycle values with an explicit, stable code. */
public enum ScoreStatus implements EgonEnum {

    RECORDED(0, "score is recorded"),
    CANCELLED(1, "score is cancelled");

    private final int code;

    private final String message;

    ScoreStatus(int code, String message) {
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
