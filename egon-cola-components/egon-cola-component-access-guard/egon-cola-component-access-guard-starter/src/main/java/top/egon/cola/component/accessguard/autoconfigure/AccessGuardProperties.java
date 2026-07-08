package top.egon.cola.component.accessguard.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.egon.cola.component.accessguard.annotation.FailStrategy;
import top.egon.cola.component.accessguard.annotation.TimeoutExecutorType;
import top.egon.cola.component.accessguard.annotation.WhiteListMode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ConfigurationProperties(prefix = "egon.cola.component.access-guard", ignoreInvalidFields = true)
@Getter
@Setter
public class AccessGuardProperties {

    private boolean enabled = true;

    private Storage storage = Storage.REDISSON;

    private String keyPrefix = "egon:access-guard";

    private FailStrategy failStrategy = FailStrategy.FAIL_OPEN;

    private KeyResolveFailureStrategy keyResolveFailureStrategy = KeyResolveFailureStrategy.USE_ALL;

    private Aop aop = new Aop();

    private Redisson redisson = new Redisson();

    private WhiteList whiteList = new WhiteList();

    private RateLimiter rateLimiter = new RateLimiter();

    private Blacklist blacklist = new Blacklist();

    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    private ThreadPool threadPool = new ThreadPool();

    private Dynamic dynamic = new Dynamic();

    private LocalFallback localFallback = new LocalFallback();

    private List<Rule> rules = new ArrayList<>();

    public enum Storage {
        REDISSON
    }

    public enum KeyResolveFailureStrategy {
        USE_ALL,
        REJECT
    }

    public enum WhiteListEmptyListStrategy {
        DENY_ALL,
        ALLOW_ALL
    }

    @Getter
    @Setter
    public static class Aop {

        private int order = -100;
    }

    @Getter
    @Setter
    public static class Redisson {

        private String clientBeanName = "redissonClient";

        private boolean autoCreateClient = false;
    }

    @Getter
    @Setter
    public static class WhiteList {

        private WhiteListEmptyListStrategy emptyListStrategy = WhiteListEmptyListStrategy.DENY_ALL;

        private WhiteListMode mode = WhiteListMode.GATEKEEPER;

        private List<String> defaultUsers = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class RateLimiter {

        private long defaultPermits = 1L;

        private long defaultInterval = 1L;

        private TimeUnit defaultIntervalUnit = TimeUnit.SECONDS;
    }

    @Getter
    @Setter
    public static class Blacklist {

        private long defaultCount = 0L;

        private Duration defaultTimeout = Duration.ofHours(24);
    }

    @Getter
    @Setter
    public static class CircuitBreaker {

        private Duration defaultTimeout = Duration.ofMillis(350);

        private TimeoutExecutorType executor = TimeoutExecutorType.THREAD_POOL;

        private boolean fallbackOnException = false;

        private boolean cancelRunningTask = true;
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
    public static class Dynamic {

        private boolean enabled = false;

        private String providerBeanName = "";
    }

    @Getter
    @Setter
    public static class LocalFallback {

        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Rule {

        private boolean enabled = true;

        private String name = "";

        private String key = "all";

        private String keyExpression = "";

        private RuleWhiteList whiteList = new RuleWhiteList();

        private RuleRateLimiter rateLimiter = new RuleRateLimiter();

        private RuleCircuitBreaker circuitBreaker = new RuleCircuitBreaker();

        private String fallbackMethod = "";

        private String returnJson = "";

        private FailStrategy failStrategy = FailStrategy.GLOBAL_DEFAULT;
    }

    @Getter
    @Setter
    public static class RuleWhiteList {

        private boolean enabled = false;

        private List<String> users = new ArrayList<>();

        private WhiteListMode mode = WhiteListMode.GATEKEEPER;
    }

    @Getter
    @Setter
    public static class RuleRateLimiter {

        private boolean enabled = false;

        private long permits = 1L;

        private long interval = 1L;

        private TimeUnit intervalUnit = TimeUnit.SECONDS;

        private boolean blacklistEnabled = false;

        private long blacklistCount = 0L;

        private Duration blacklistTimeout = Duration.ofHours(24);

        private boolean enableBlacklistForAllKey = false;
    }

    @Getter
    @Setter
    public static class RuleCircuitBreaker {

        private boolean enabled = false;

        private Duration timeout = Duration.ofMillis(350);

        private TimeoutExecutorType executor = TimeoutExecutorType.GLOBAL_DEFAULT;

        private boolean fallbackOnException = false;

        private boolean cancelRunningTask = true;
    }
}
