package top.egon.cola.component.bytecode.api.observation;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum ObservationResult implements EgonEnum {
    SUCCESS(0, "SUCCESS"),
    ERROR(1, "ERROR");

    private final int code;
    private final String message;

    ObservationResult(int code, String message) {
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
