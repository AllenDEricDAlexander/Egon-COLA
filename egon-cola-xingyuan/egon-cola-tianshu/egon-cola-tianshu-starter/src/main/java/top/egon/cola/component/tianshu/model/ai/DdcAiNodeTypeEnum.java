package top.egon.cola.component.tianshu.model.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DdcAiNodeTypeEnum {
    DIRECTORY("DIRECTORY"),
    FILE("FILE");

    private final String code;

    DdcAiNodeTypeEnum(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static DdcAiNodeTypeEnum fromCode(String code) {
        for (DdcAiNodeTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown AI node type");
    }
}
