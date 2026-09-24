package top.egon.cola.component.outbox.statemachine;

import top.egon.cola.component.common.core.enums.EgonEnum;

/** Stable control signals for the transactional outbox lifecycle state machine. */
public enum OutboxLifecycleSignalEnum implements EgonEnum {
    ENQUEUE(0, "ENQUEUE"),
    CLAIM(1, "CLAIM"),
    RECLAIM(2, "RECLAIM"),
    DELIVERY_SUCCEEDED(3, "DELIVERY_SUCCEEDED"),
    DELIVERY_RETRYABLE(4, "DELIVERY_RETRYABLE"),
    SCHEDULE_RETRY(5, "SCHEDULE_RETRY"),
    DELIVERY_PERMANENT(6, "DELIVERY_PERMANENT");

    private final int code;
    private final String message;

    OutboxLifecycleSignalEnum(int code, String message) {
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
