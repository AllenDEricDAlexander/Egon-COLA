package top.egon.cola.component.common.desensitize.annotation;

public enum SensitiveScene {

    RESPONSE("前端返回"),
    LOG("日志输出");

    private final String desc;

    SensitiveScene(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
