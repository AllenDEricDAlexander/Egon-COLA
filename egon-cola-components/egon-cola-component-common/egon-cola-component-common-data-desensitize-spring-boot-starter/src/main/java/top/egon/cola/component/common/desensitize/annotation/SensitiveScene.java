package top.egon.cola.component.common.desensitize.annotation;

import top.egon.cola.component.common.core.enums.EgonEnum;

public enum SensitiveScene implements EgonEnum {

    RESPONSE(0, "前端返回"),
    LOG(1, "日志输出");

    private final int code;

    private final String desc;

    SensitiveScene(int code, String desc) {
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
