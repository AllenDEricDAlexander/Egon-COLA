package top.egon.cola.component.bytecode.agent;

import java.util.Locale;

public enum AgentFailurePolicy {
    SKIP_CLASS,
    DISABLE_FEATURE,
    MARK_FATAL;

    static AgentFailurePolicy parse(String value) {
        if (value == null || value.isBlank()) {
            return SKIP_CLASS;
        }
        return valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    }
}
