package top.egon.cola.component.tianshu.admin.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

public enum DdcAiKindPersistenceEnum implements EgonEnum {
    PROMPT("PROMPT", 201, "Prompt"),
    SKILL("SKILL", 202, "Skill");

    @EnumValue
    private final String value;
    private final int code;
    private final String message;

    DdcAiKindPersistenceEnum(String value, int code, String message) {
        this.value = value;
        this.code = code;
        this.message = message;
    }

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

    public static DdcAiKindPersistenceEnum fromValue(String value) {
        for (DdcAiKindPersistenceEnum kind : values()) {
            if (kind.value.equals(value)) return kind;
        }
        throw new IllegalArgumentException("Unknown AI kind persistence value");
    }
}
