package top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OAuthTokenTypeEnum {
    CODE("AUTHORIZATION_CODE"),
    ACCESS("ACCESS"),
    REFRESH("REFRESH"),
    ID("ID_TOKEN"),
    CONSENT_STATE("CONSENT_STATE");

    @EnumValue
    private final String code;

    OAuthTokenTypeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    @JsonValue
    public String wireValue() {
        return name();
    }

    @JsonCreator
    public static OAuthTokenTypeEnum fromJson(String value) {
        for (OAuthTokenTypeEnum tokenType : values()) {
            if (tokenType.name().equals(value)) {
                return tokenType;
            }
        }
        throw new IllegalArgumentException("UNKNOWN_OAUTH_TOKEN_TYPE");
    }

    public static OAuthTokenTypeEnum fromCode(String code) {
        for (OAuthTokenTypeEnum tokenType : values()) {
            if (tokenType.code.equals(code)) {
                return tokenType;
            }
        }
        throw new IllegalArgumentException("UNKNOWN_OAUTH_TOKEN_TYPE_CODE");
    }
}
