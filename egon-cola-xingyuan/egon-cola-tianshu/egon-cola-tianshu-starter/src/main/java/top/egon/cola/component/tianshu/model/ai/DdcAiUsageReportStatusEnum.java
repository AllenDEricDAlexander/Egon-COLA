package top.egon.cola.component.tianshu.model.ai;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 单条使用上报在天枢侧的受理结果。
 * / Tianshu acceptance status of one usage report.
 */
public enum DdcAiUsageReportStatusEnum {
    ACCEPTED("ACCEPTED"),
    ACCEPTED_DUPLICATE("ACCEPTED_DUPLICATE"),
    REJECTED("REJECTED");

    private final String code;

    DdcAiUsageReportStatusEnum(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static DdcAiUsageReportStatusEnum fromCode(String code) {
        for (DdcAiUsageReportStatusEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown AI usage report status");
    }
}
