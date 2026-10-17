package top.egon.cola.component.tianshu.admin.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

/** 优化运行的触发来源。 / Trigger source of an optimization run. */
public enum DdcAiRunTriggerPersistenceEnum implements EgonEnum {
    SCHEDULED("SCHEDULED", 550, "scheduled"),
    MANUAL("MANUAL", 551, "manual");

    @EnumValue
    private final String value;
    private final int code;
    private final String message;

    DdcAiRunTriggerPersistenceEnum(String value, int code, String message) {
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

    @JsonCreator
    public static DdcAiRunTriggerPersistenceEnum fromValue(String value) {
        for (DdcAiRunTriggerPersistenceEnum item : values()) {
            if (item.value.equals(value)) return item;
        }
        throw new IllegalArgumentException("Unknown AI optimization run trigger");
    }
}
