package top.egon.cola.component.tianshu.admin.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import top.egon.cola.component.common.core.enums.EgonEnum;

/** 优化运行与评分失败的稳定错误码。 / Stable error codes for failed optimization runs and judgements. */
public enum DdcAiRunErrorCodeEnum implements EgonEnum {
    MODEL_AUTH("MODEL_AUTH", 560, "model auth"),
    MODEL_TIMEOUT("MODEL_TIMEOUT", 561, "model timeout"),
    MODEL_ERROR("MODEL_ERROR", 562, "model error"),
    MODEL_RATE_LIMITED("MODEL_RATE_LIMITED", 563, "model rate limited"),
    INVALID_OUTPUT("INVALID_OUTPUT", 564, "invalid output"),
    BASE_CHANGED("BASE_CHANGED", 565, "base changed"),
    DRAFT_DIRTY("DRAFT_DIRTY", 566, "draft dirty"),
    PUBLISH_CONFLICT("PUBLISH_CONFLICT", 567, "publish conflict"),
    LEASE_EXPIRED("LEASE_EXPIRED", 568, "lease expired"),
    NOT_PUBLISHED("NOT_PUBLISHED", 569, "not published");

    @EnumValue
    private final String value;
    private final int code;
    private final String message;

    DdcAiRunErrorCodeEnum(String value, int code, String message) {
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
    public static DdcAiRunErrorCodeEnum fromValue(String value) {
        for (DdcAiRunErrorCodeEnum item : values()) {
            if (item.value.equals(value)) return item;
        }
        throw new IllegalArgumentException("Unknown AI optimization run error code");
    }
}
