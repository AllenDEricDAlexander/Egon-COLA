package top.egon.cola.component.tianshu.model.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 下游一次 Prompt/Skill 使用的业务结果。
 * / Business outcome of one downstream Prompt or Skill usage.
 */
public enum DdcAiOutcomeEnum {
    SUCCESS("SUCCESS"),
    FAILURE("FAILURE"),
    UNKNOWN("UNKNOWN");

    private final String code;

    DdcAiOutcomeEnum(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static DdcAiOutcomeEnum fromCode(String code) {
        for (DdcAiOutcomeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown AI usage outcome");
    }
}
