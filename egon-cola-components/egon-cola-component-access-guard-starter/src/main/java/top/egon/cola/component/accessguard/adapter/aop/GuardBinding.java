package top.egon.cola.component.accessguard.adapter.aop;

public record GuardBinding(String ruleId, String key, Kind kind) {

    public GuardBinding {
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId must not be blank");
        }
        ruleId = ruleId.trim();
        key = key == null ? "" : key.trim();
        if (kind == null) {
            throw new IllegalArgumentException("kind is required");
        }
    }

    public enum Kind {
        ACCESS,
        ALLOW_LIST,
        RATE_LIMIT,
        TIME_LIMIT
    }
}
