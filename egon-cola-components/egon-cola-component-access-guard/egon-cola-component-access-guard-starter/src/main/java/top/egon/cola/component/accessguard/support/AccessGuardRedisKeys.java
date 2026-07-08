package top.egon.cola.component.accessguard.support;

public class AccessGuardRedisKeys {

    private static final String ALL_HASH = "all";

    private final String prefix;

    private final String app;

    private final String env;

    public AccessGuardRedisKeys(String prefix, String app, String env) {
        this.prefix = trimColon(prefix);
        this.app = app;
        this.env = env;
    }

    public String whiteList(String ruleName, String accessKeyHash) {
        return build(ruleName, accessKeyHash, "white-list");
    }

    public String limiter(String ruleName, String accessKeyHash) {
        return build(ruleName, accessKeyHash, "limiter");
    }

    public String blacklist(String ruleName, String accessKeyHash) {
        return build(ruleName, accessKeyHash, "blacklist");
    }

    public String rejectCount(String ruleName, String accessKeyHash) {
        return build(ruleName, accessKeyHash, "reject-count");
    }

    public String configVersion(String ruleName) {
        return build(ruleName, ALL_HASH, "config-version");
    }

    private String build(String ruleName, String accessKeyHash, String type) {
        return String.join(":", prefix, app, env, ruleName, accessKeyHash, type);
    }

    private String trimColon(String value) {
        if (value == null) {
            return "";
        }
        String result = value;
        while (result.startsWith(":")) {
            result = result.substring(1);
        }
        while (result.endsWith(":")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
