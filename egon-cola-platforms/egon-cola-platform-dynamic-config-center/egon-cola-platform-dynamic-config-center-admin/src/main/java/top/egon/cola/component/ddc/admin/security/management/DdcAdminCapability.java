package top.egon.cola.component.ddc.admin.security.management;

public enum DdcAdminCapability {

    READ("DDC_READ"),

    WRITE("DDC_WRITE"),

    PUBLISH("DDC_PUBLISH"),

    CACHE("DDC_CACHE"),

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
