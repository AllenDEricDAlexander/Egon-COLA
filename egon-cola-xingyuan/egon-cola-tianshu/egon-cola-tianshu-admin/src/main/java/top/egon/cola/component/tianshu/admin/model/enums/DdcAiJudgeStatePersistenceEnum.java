package top.egon.cola.component.tianshu.admin.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

/** 使用样本的自动评分状态。 / Automatic judge state of a usage sample. */
public enum DdcAiJudgeStatePersistenceEnum implements EgonEnum {
    NOT_REQUIRED("NOT_REQUIRED", 520, "not required"),
    PENDING("PENDING", 521, "pending"),
    JUDGING("JUDGING", 522, "judging"),
    DONE("DONE", 523, "done"),
    FAILED("FAILED", 524, "failed");

    @EnumValue
    private final String value;
    private final int code;
    private final String message;

    DdcAiJudgeStatePersistenceEnum(String value, int code, String message) {
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
    public static DdcAiJudgeStatePersistenceEnum fromValue(String value) {
        for (DdcAiJudgeStatePersistenceEnum item : values()) {
            if (item.value.equals(value)) return item;
        }
        throw new IllegalArgumentException("Unknown AI judge state");
    }
}
