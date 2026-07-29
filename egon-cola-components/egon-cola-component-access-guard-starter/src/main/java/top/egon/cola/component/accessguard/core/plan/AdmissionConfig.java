package top.egon.cola.component.accessguard.core.plan;

import top.egon.cola.component.accessguard.policy.PolicyConfig;
import top.egon.cola.component.accessguard.policy.allow.AllowListMode;

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

    public record DenyListConfig(boolean enabled, String dataVersion) implements PolicyConfig {

        public DenyListConfig {
            dataVersion = requireVersion(dataVersion);
        }

        public DenyListConfig(boolean enabled) {
            this(enabled, "v1");
        }
    }

    public record AllowListConfig(boolean enabled, AllowListMode mode, String dataVersion) implements PolicyConfig {

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
    ) implements PolicyConfig {

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
    ) implements PolicyConfig {

        public RateLimitConfig {
            algorithm = Objects.requireNonNull(algorithm, "algorithm");
            refillPeriod = Objects.requireNonNull(refillPeriod, "refillPeriod");
        }
    }

    public enum RateLimitAlgorithm {
        TOKEN_BUCKET
    }

    private static String requireVersion(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("dataVersion must not be blank");
        }
        return value.trim();
    }
}
