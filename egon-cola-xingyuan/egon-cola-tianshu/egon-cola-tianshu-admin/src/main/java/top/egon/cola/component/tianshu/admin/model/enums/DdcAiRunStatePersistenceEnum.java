package top.egon.cola.component.tianshu.admin.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

/** 优化运行的生命周期状态。 / Lifecycle state of an optimization run. */
public enum DdcAiRunStatePersistenceEnum implements EgonEnum {
    PENDING("PENDING", 540, "pending"),
    RUNNING("RUNNING", 541, "running"),
    SUCCEEDED("SUCCEEDED", 542, "succeeded"),
    FAILED("FAILED", 543, "failed"),
    SKIPPED("SKIPPED", 544, "skipped");

    @EnumValue
    private final String value;
    private final int code;
    private final String message;

    DdcAiRunStatePersistenceEnum(String value, int code, String message) {
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

    /** 是否为不可再变化的终态。 / Whether this is an immutable terminal state. */
    public boolean terminal() {
        return this == SUCCEEDED || this == FAILED || this == SKIPPED;
    }

    @JsonCreator
    public static DdcAiRunStatePersistenceEnum fromValue(String value) {
        for (DdcAiRunStatePersistenceEnum item : values()) {
            if (item.value.equals(value)) return item;
        }
        throw new IllegalArgumentException("Unknown AI optimization run state");
    }
}
