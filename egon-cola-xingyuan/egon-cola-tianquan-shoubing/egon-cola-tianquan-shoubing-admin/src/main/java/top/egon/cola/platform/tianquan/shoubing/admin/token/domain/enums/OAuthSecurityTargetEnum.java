package top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OAuthSecurityTargetEnum {
    REALM("REALM"),
    CLIENT("CLIENT"),
    SUBJECT("SUBJECT"),
    JTI("JTI"),
    AUTHORIZATION("AUTHORIZATION"),
    KID("KID");

    @EnumValue
    private final String code;

    OAuthSecurityTargetEnum(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public boolean isSecurityStateTarget() {
        return this == REALM || this == CLIENT || this == SUBJECT;
    }

    public boolean isRevocationTarget() {
        return this == JTI || this == AUTHORIZATION || this == KID;
    }

    @JsonCreator
    public static OAuthSecurityTargetEnum fromCode(String code) {
        for (OAuthSecurityTargetEnum target : values()) {
            if (target.code.equals(code)) {
                return target;
            }
        }
        throw new IllegalArgumentException("UNKNOWN_OAUTH_SECURITY_TARGET");
    }
}
