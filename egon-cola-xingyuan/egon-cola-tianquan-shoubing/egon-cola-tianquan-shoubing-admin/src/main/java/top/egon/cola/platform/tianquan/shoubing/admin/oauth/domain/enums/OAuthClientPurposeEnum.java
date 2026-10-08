package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OAuthClientPurposeEnum {
    USER("USER"),
    SERVICE("SERVICE");

    @EnumValue
    @JsonValue
    private final String code;

    OAuthClientPurposeEnum(String code) {
        this.code = code;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OAuthClientPurposeEnum fromCode(String code) {
        for (OAuthClientPurposeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("OAUTH_CLIENT_PURPOSE_INVALID");
    }
}
