package top.egon.cola.component.accessguard.core.plan;

import top.egon.cola.component.accessguard.policy.allow.AllowListMode;
import top.egon.cola.component.common.core.enums.EgonEnum;

import java.time.Duration;
import java.util.Objects;

public record AdmissionConfig(
        DenyListConfig denyList,
        AllowListConfig allowList,
        PenaltyBoxConfig penaltyBox,
        RateLimitConfig rateLimit
) {

    public AdmissionConfig {
        denyList = Objects.requireNonNull(denyList, "denyList");
        allowList = Objects.requireNonNull(allowList, "allowList");
        penaltyBox = Objects.requireNonNull(penaltyBox, "penaltyBox");
        rateLimit = Objects.requireNonNull(rateLimit, "rateLimit");
    }

    public record DenyListConfig(boolean enabled, String dataVersion) {

        public DenyListConfig {
            dataVersion = requireVersion(dataVersion);
        }

        public DenyListConfig(boolean enabled) {
            this(enabled, "v1");
        }
    }

    public record AllowListConfig(boolean enabled, AllowListMode mode, String dataVersion) {

        public AllowListConfig {
            mode = Objects.requireNonNull(mode, "mode");
            dataVersion = requireVersion(dataVersion);
        }

        public AllowListConfig(boolean enabled, AllowListMode mode) {
            this(enabled, mode, "v1");
        }
    }

    public record PenaltyBoxConfig(
            boolean enabled,
            long threshold,
            Duration violationTtl,
            Duration penaltyTtl
    ) {

        public PenaltyBoxConfig {
            violationTtl = Objects.requireNonNull(violationTtl, "violationTtl");
            penaltyTtl = Objects.requireNonNull(penaltyTtl, "penaltyTtl");
        }
    }

    public record RateLimitConfig(
            boolean enabled,
            RateLimitAlgorithm algorithm,
            long capacity,
            long refillTokens,
            Duration refillPeriod,
            long requestedTokens
    ) {

        public RateLimitConfig {
            algorithm = Objects.requireNonNull(algorithm, "algorithm");
            refillPeriod = Objects.requireNonNull(refillPeriod, "refillPeriod");
        }
    }

    public enum RateLimitAlgorithm implements EgonEnum {
        TOKEN_BUCKET(0, "TOKEN_BUCKET"),
        LEAKY_BUCKET(1, "LEAKY_BUCKET"),
        SLIDING_WINDOW(2, "SLIDING_WINDOW");

        private final int code;

        private final String message;

        RateLimitAlgorithm(int code, String message) {
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

    private static String requireVersion(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("dataVersion must not be blank");
        }
        return value.trim();
    }
}
