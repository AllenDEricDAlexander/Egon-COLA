package top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RealmTypeEnum {
    TENANT("TENANT"),
    PLATFORM("PLATFORM");

    @EnumValue
    @JsonValue
    private final String code;

    RealmTypeEnum(String code) {
        this.code = code;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static RealmTypeEnum fromCode(String code) {
        for (RealmTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("REALM_TYPE_INVALID");
    }
}
