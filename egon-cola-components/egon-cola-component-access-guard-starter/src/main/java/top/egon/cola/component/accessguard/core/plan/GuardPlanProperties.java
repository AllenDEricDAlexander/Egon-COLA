package top.egon.cola.component.accessguard.core.plan;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardEngine;
import top.egon.cola.component.accessguard.core.failure.FailurePolicy;
import top.egon.cola.component.accessguard.execution.RejectionMode;
import top.egon.cola.component.accessguard.execution.TimeLimitMode;
import top.egon.cola.component.accessguard.execution.TimeLimiterType;
import top.egon.cola.component.accessguard.policy.allow.AllowListMode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties(
        prefix = GuardPlanProperties.PREFIX,
        ignoreInvalidFields = false,
        ignoreUnknownFields = false)
@Getter
@Setter
public class GuardPlanProperties {

    public static final String PREFIX = "egon.cola.component.access-guard";

    private boolean enabled = true;

    private AccessGuardEngine engine = AccessGuardEngine.AOP;

    private Storage storage = Storage.LOCAL;

    private Defaults defaults = new Defaults();

    private Key key = new Key();

    private Redisson redisson = new Redisson();

    private Local local = new Local();

    private ThreadPool threadPool = new ThreadPool();

    private Map<String, Rule> rules = new LinkedHashMap<>();

    public enum Storage {
        LOCAL,
        REDISSON
    }

    @Getter
    @Setter
    public static class Defaults {

        private RejectionMode rejection = RejectionMode.THROW;
    }

    @Getter
    @Setter
    public static class Key {

        private List<String> contributors = new ArrayList<>(List.of("ARGUMENT"));

        private List<String> trustedProxies = new ArrayList<>();

        private List<String> headers = new ArrayList<>();

        private String hmacSecret = "";

        private int maxPartLength = 1024;
    }

    @Getter
    @Setter
    public static class Redisson {

        private String clientBeanName = "redissonClient";

        private String keyPrefix = "egon:access-guard";
    }

    @Getter
    @Setter
    public static class Local {

        private int maxEntries = 100_000;

        private Duration cleanupInterval = Duration.ofMinutes(1);
    }

    @Getter
    @Setter
    public static class ThreadPool {

        private String name = "access-guard";

        private int corePoolSize = 4;

        private int maxPoolSize = 16;

        private int queueCapacity = 1024;
    }

    @Getter
    @Setter
    public static class Rule {

        private boolean enabled = true;

        private RuleKey key = new RuleKey();

        private DenyList denyList = new DenyList();

        private AllowList allowList = new AllowList();

        private PenaltyBox penaltyBox = new PenaltyBox();

        private RateLimit rateLimit = new RateLimit();

        private TimeLimit timeLimit = new TimeLimit();

        private Rejection rejection = new Rejection();

        private Failures failurePolicies = new Failures();

        private Observability observability = new Observability();
    }

    @Getter
    @Setter
    public static class RuleKey {

        private List<String> contributors = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class DenyList {

        private boolean enabled = false;

        private String dataVersion = "v1";
    }

    @Getter
    @Setter
    public static class AllowList {

        private boolean enabled = false;

        private AllowListMode mode = AllowListMode.GATE;

        private String dataVersion = "v1";
    }

    @Getter
    @Setter
    public static class PenaltyBox {

        private boolean enabled = false;

        private long threshold = 5;

        private Duration violationTtl = Duration.ofMinutes(1);

        private Duration penaltyTtl = Duration.ofMinutes(10);
    }

    @Getter
    @Setter
    public static class RateLimit {

        private boolean enabled = false;

        private AdmissionConfig.RateLimitAlgorithm algorithm = AdmissionConfig.RateLimitAlgorithm.TOKEN_BUCKET;

        private long capacity = 100;

        private long refillTokens = 100;

        private Duration refillPeriod = Duration.ofSeconds(1);

        private long requestedTokens = 1;
    }

    @Getter
    @Setter
    public static class TimeLimit {

        private boolean enabled = false;

        private TimeLimitMode mode = TimeLimitMode.DISABLED;

        private TimeLimiterType executor = TimeLimiterType.CALLER_THREAD;

        private Duration timeout = Duration.ofSeconds(1);

        private boolean cancelRunningTask = true;
    }

    @Getter
    @Setter
    public static class Rejection {

        private RejectionMode mode;

        private String fallbackMethod = "";

        private String returnJson = "";
    }

    @Getter
    @Setter
    public static class Failures {

        private FailurePolicy keyResolution = FailurePolicy.FAIL_CLOSED;

        private FailurePolicy denyListStore = FailurePolicy.FAIL_CLOSED;

        private FailurePolicy allowListStore = FailurePolicy.FAIL_CLOSED;

        private FailurePolicy penaltyStore = FailurePolicy.LOCAL_FALLBACK;

        private FailurePolicy rateLimitBackend = FailurePolicy.LOCAL_FALLBACK;

        private FailurePolicy execution = FailurePolicy.FAIL_CLOSED;

        private FailurePolicy observability = FailurePolicy.FAIL_OPEN;
    }

    @Getter
    @Setter
    public static class Observability {

        private boolean finalEvents = true;

        private boolean stageEvents = false;

        private boolean metrics = true;

        private boolean logging = true;

        private boolean endpoint = true;
    }
}
