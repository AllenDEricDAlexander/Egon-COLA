package top.egon.cola.component.ruleengine.result;

public enum RuleStatus {

    SUCCESS(0, "success"),
    STOPPED(600100, "rule stopped"),
    FAILED(500100, "rule failed"),
    TIMEOUT(500101, "rule execution timeout"),
    MAX_STEPS_EXCEEDED(500102, "rule max steps exceeded"),
    NO_ROUTE(500103, "rule tree no route"),
    EMPTY_CHAIN(500104, "rule chain is empty"),
    EMPTY_TREE(500105, "rule tree is empty"),
    NODE_ERROR(500106, "rule node error"),
    CONFIG_ERROR(500107, "rule config error");

    private final int code;

    private final String message;

    RuleStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
