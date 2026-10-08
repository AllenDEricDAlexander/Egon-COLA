package top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OAuthKeyStatusEnum {
    PUBLISHED("PUBLISHED"),
    ACTIVE("ACTIVE"),
    VERIFY_ONLY("VERIFY_ONLY"),
    RETIRED("RETIRED"),
    COMPROMISED("COMPROMISED");

    @EnumValue
    private final String code;

    OAuthKeyStatusEnum(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static OAuthKeyStatusEnum fromCode(String code) {
        for (OAuthKeyStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("UNKNOWN_OAUTH_KEY_STATUS");
    }
}
