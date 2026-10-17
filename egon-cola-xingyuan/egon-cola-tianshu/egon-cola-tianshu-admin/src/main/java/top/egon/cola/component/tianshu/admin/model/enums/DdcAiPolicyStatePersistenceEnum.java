package top.egon.cola.component.tianshu.admin.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

/** 优化策略的闭环状态。 / Closed-loop state of an optimization policy. */
public enum DdcAiPolicyStatePersistenceEnum implements EgonEnum {
    ARMED("ARMED", 530, "armed"),
    AWAITING_PUBLISH("AWAITING_PUBLISH", 531, "awaiting publish"),
    CONVERGED("CONVERGED", 532, "converged"),
    EXHAUSTED("EXHAUSTED", 533, "exhausted"),
    PAUSED("PAUSED", 534, "paused");

    @EnumValue
    private final String value;
    private final int code;
    private final String message;

    DdcAiPolicyStatePersistenceEnum(String value, int code, String message) {
        this.value = value;
        this.code = code;
        this.message = message;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /** 只有 ARMED 与 AWAITING_PUBLISH 参与周期评估。 / Only ARMED and AWAITING_PUBLISH take part in scheduled evaluation. */
    public boolean evaluable() {
        return this == ARMED || this == AWAITING_PUBLISH;
    }

    @JsonCreator
    public static DdcAiPolicyStatePersistenceEnum fromValue(String value) {
        for (DdcAiPolicyStatePersistenceEnum item : values()) {
            if (item.value.equals(value)) return item;
        }
        throw new IllegalArgumentException("Unknown AI optimization policy state");
    }
}
