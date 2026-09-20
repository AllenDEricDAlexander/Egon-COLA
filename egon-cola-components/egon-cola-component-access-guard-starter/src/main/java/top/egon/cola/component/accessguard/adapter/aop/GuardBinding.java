package top.egon.cola.component.accessguard.adapter.aop;

import top.egon.cola.component.common.core.enums.EgonEnum;

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

    public enum Kind implements EgonEnum {
        ACCESS(0, "ACCESS"),
        ALLOW_LIST(1, "ALLOW_LIST"),
        RATE_LIMIT(2, "RATE_LIMIT"),
        TIME_LIMIT(3, "TIME_LIMIT");

        private final int code;

        private final String message;

        Kind(int code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
