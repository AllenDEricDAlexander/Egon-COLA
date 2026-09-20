package top.egon.cola.component.outbox.store;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum OutboxStatus implements EgonEnum {
    PENDING(0, "PENDING"),
    PROCESSING(1, "PROCESSING"),
    RETRY_WAIT(2, "RETRY_WAIT"),
    SUCCEEDED(3, "SUCCEEDED"),
    DEAD(4, "DEAD");

    private final int code;
    private final String message;

    OutboxStatus(int code, String message) {
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
