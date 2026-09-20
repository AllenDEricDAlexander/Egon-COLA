package top.egon.cola.component.common.desensitize.annotation;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum SensitiveType implements EgonEnum {

    MOBILE(0, "手机号"),
    EMAIL(1, "邮箱"),
    ID_CARD(2, "身份证"),
    BANK_CARD(3, "银行卡"),
    NAME(4, "姓名"),
    ADDRESS(5, "地址"),
    FULL(6, "全部隐藏");

    private final int code;

    private final String desc;

    SensitiveType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return desc;
    }

    public String getDesc() {
        return desc;
    }
}
