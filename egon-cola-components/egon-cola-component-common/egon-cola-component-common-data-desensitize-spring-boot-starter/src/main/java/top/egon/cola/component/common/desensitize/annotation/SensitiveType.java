package top.egon.cola.component.common.desensitize.annotation;

public enum SensitiveType {

    MOBILE("手机号"),
    EMAIL("邮箱"),
    ID_CARD("身份证"),
    BANK_CARD("银行卡"),
    NAME("姓名"),
    ADDRESS("地址"),
    FULL("全部隐藏");

    private final String desc;

    SensitiveType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
