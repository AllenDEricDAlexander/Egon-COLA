package top.egon.cola.component.tianshu.admin.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

/** 下游一次使用的业务结果。 / Business outcome of one downstream usage. */
public enum DdcAiOutcomePersistenceEnum implements EgonEnum {
    SUCCESS("SUCCESS", 510, "success"),
    FAILURE("FAILURE", 511, "failure"),
    UNKNOWN("UNKNOWN", 512, "unknown");

    @EnumValue
    private final String value;
    private final int code;
    private final String message;

    DdcAiOutcomePersistenceEnum(String value, int code, String message) {
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
    public static DdcAiOutcomePersistenceEnum fromValue(String value) {
        for (DdcAiOutcomePersistenceEnum item : values()) {
            if (item.value.equals(value)) return item;
        }
        throw new IllegalArgumentException("Unknown AI usage outcome");
    }
}
