package top.egon.cola.component.tianshu.model.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DdcAiKindEnum {
    PROMPT("PROMPT"),
    SKILL("SKILL");

    private final String code;

    DdcAiKindEnum(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static DdcAiKindEnum fromCode(String code) {
        for (DdcAiKindEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown AI resource kind");
    }
}
