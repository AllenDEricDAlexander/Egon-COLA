package top.egon.cola.platform.rbac3.core.rule;

import java.util.List;

public final class Rbac3RuleViolation extends RuntimeException {

    private final String reasonCode;
    private final List<String> evidenceIds;

    public Rbac3RuleViolation(String reasonCode) {
        this(reasonCode, List.of());
    }

    public Rbac3RuleViolation(String reasonCode, List<String> evidenceIds) {
        super(reasonCode);
        this.reasonCode = required(reasonCode);
        this.evidenceIds = List.copyOf(evidenceIds);
    }

    public String reasonCode() {
        return reasonCode;
    }

    public List<String> evidenceIds() {
        return evidenceIds;
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("reasonCode is required");
        }
        return value;
    }
}
