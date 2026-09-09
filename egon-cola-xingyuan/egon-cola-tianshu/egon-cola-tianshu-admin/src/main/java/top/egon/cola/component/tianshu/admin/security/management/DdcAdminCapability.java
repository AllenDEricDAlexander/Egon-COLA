package top.egon.cola.component.tianshu.admin.security.management;

public enum DdcAdminCapability {

    READ("TIANSHU_READ"),

    WRITE("TIANSHU_WRITE"),

    PUBLISH("TIANSHU_PUBLISH"),

    CACHE("TIANSHU_CACHE"),

    ALL("*");

    private final String value;

    DdcAdminCapability(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public String authority() {
        return "CAP_" + value;
    }
}
