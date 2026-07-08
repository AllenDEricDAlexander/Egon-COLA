package top.egon.cola.component.accessguard.config;

import org.springframework.core.annotation.AnnotatedElementUtils;
import top.egon.cola.component.accessguard.annotation.AccessGuard;
import top.egon.cola.component.accessguard.annotation.DoHystrix;
import top.egon.cola.component.accessguard.annotation.DoRateLimiter;
import top.egon.cola.component.accessguard.annotation.DoWhiteList;
import top.egon.cola.component.accessguard.annotation.FailStrategy;
import top.egon.cola.component.accessguard.annotation.RateLimiterAccessInterceptor;
import top.egon.cola.component.accessguard.annotation.TimeoutCircuitBreaker;
import top.egon.cola.component.accessguard.annotation.TimeoutExecutorType;
import top.egon.cola.component.accessguard.annotation.WhiteListAccessInterceptor;
import top.egon.cola.component.accessguard.annotation.WhiteListMode;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterRuleConversion;
import top.egon.cola.component.accessguard.support.MethodSignatureKey;
import top.egon.cola.component.accessguard.support.SensitiveValueHasher;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AccessGuardAnnotationResolver {

    public AccessGuardRule resolve(Method method) {
        RuleSpec spec = RuleSpec.defaults(method);
        AccessGuard accessGuard = AnnotatedElementUtils.findMergedAnnotation(method, AccessGuard.class);
        if (accessGuard != null) {
            spec.name = chooseName(accessGuard.name(), method);
            spec.key = accessGuard.key();
            spec.keyExpression = accessGuard.keyExpression();
            spec.whiteListEnabled = accessGuard.whitelist();
            spec.rateLimiterEnabled = accessGuard.rateLimiter();
            spec.blacklistEnabled = accessGuard.blacklist();
            spec.timeoutEnabled = accessGuard.timeoutBreaker();
            spec.fallbackMethod = accessGuard.fallbackMethod();
            spec.returnJson = accessGuard.returnJson();
        }

        WhiteListAccessInterceptor whiteList = AnnotatedElementUtils.findMergedAnnotation(method, WhiteListAccessInterceptor.class);
        if (whiteList != null) {
            spec.name = chooseName(whiteList.name(), method);
            spec.key = whiteList.key();
            spec.keyExpression = whiteList.keyExpression();
            spec.whiteListEnabled = whiteList.enabled();
            spec.whiteListUsers = Arrays.asList(whiteList.users());
            spec.whiteListMode = whiteList.mode();
            spec.fallbackMethod = whiteList.fallbackMethod();
            spec.returnJson = whiteList.returnJson();
            spec.failStrategy = whiteList.failStrategy();
        }

        RateLimiterAccessInterceptor rateLimiter = AnnotatedElementUtils.findMergedAnnotation(method, RateLimiterAccessInterceptor.class);
        if (rateLimiter != null) {
            spec.name = chooseName(rateLimiter.name(), method);
            spec.key = rateLimiter.key();
            spec.keyExpression = rateLimiter.keyExpression();
            spec.rateLimiterEnabled = true;
            RateLimiterRuleConversion conversion = rateLimiter.permitsPerSecond() > 0
                    ? RateLimiterRuleConversion.fromPermitsPerSecond(rateLimiter.permitsPerSecond())
                    : new RateLimiterRuleConversion(rateLimiter.permits(), rateLimiter.interval(), rateLimiter.intervalUnit());
            spec.permits = conversion.permits();
            spec.interval = conversion.interval();
            spec.intervalUnit = conversion.intervalUnit();
            spec.blacklistEnabled = rateLimiter.blacklistCount() > 0;
            spec.blacklistCount = rateLimiter.blacklistCount();
            spec.blacklistTimeout = duration(rateLimiter.blacklistTimeout(), rateLimiter.blacklistTimeUnit());
            spec.enableBlacklistForAllKey = rateLimiter.enableBlacklistForAllKey();
            spec.fallbackMethod = rateLimiter.fallbackMethod();
            spec.returnJson = rateLimiter.returnJson();
            spec.failStrategy = rateLimiter.failStrategy();
        }

        TimeoutCircuitBreaker timeout = AnnotatedElementUtils.findMergedAnnotation(method, TimeoutCircuitBreaker.class);
        if (timeout != null) {
            spec.name = chooseName(timeout.name(), method);
            spec.timeoutEnabled = timeout.enabled();
            spec.timeout = duration(timeout.timeoutValue(), timeout.timeoutUnit());
            spec.timeoutExecutor = timeout.executor();
            spec.fallbackOnException = timeout.fallbackOnException();
            spec.cancelRunningTask = timeout.cancelRunningTask();
            spec.fallbackMethod = timeout.fallbackMethod();
            spec.returnJson = timeout.returnJson();
        }

        DoWhiteList doWhiteList = AnnotatedElementUtils.findMergedAnnotation(method, DoWhiteList.class);
        if (doWhiteList != null) {
            spec.name = chooseName("", method);
            spec.key = doWhiteList.key();
            spec.whiteListEnabled = true;
            spec.returnJson = doWhiteList.returnJson();
        }

        DoRateLimiter doRateLimiter = AnnotatedElementUtils.findMergedAnnotation(method, DoRateLimiter.class);
        if (doRateLimiter != null) {
            spec.name = chooseName("", method);
            spec.key = "all";
            spec.rateLimiterEnabled = true;
            RateLimiterRuleConversion conversion = RateLimiterRuleConversion.fromPermitsPerSecond(doRateLimiter.permitsPerSecond());
            spec.permits = conversion.permits();
            spec.interval = conversion.interval();
            spec.intervalUnit = conversion.intervalUnit();
            spec.blacklistEnabled = false;
            spec.returnJson = doRateLimiter.returnJson();
        }

        DoHystrix doHystrix = AnnotatedElementUtils.findMergedAnnotation(method, DoHystrix.class);
        if (doHystrix != null) {
            spec.name = chooseName("", method);
            spec.timeoutEnabled = true;
            spec.timeout = Duration.ofMillis(doHystrix.timeoutValue());
            spec.fallbackOnException = false;
            spec.returnJson = doHystrix.returnJson();
        }
        return spec.toRule();
    }

    private String chooseName(String configuredName, Method method) {
        if (configuredName != null && !configuredName.isBlank()) {
            return configuredName;
        }
        return SensitiveValueHasher.sha256Hex(MethodSignatureKey.from(method).asString()).substring(0, 16);
    }

    private Duration duration(long value, TimeUnit unit) {
        return Duration.ofNanos(unit.toNanos(value));
    }

    private static class RuleSpec {

        private String name;
        private String key = "all";
        private String keyExpression = "";
        private boolean whiteListEnabled = false;
        private List<String> whiteListUsers = new ArrayList<>();
        private WhiteListMode whiteListMode = WhiteListMode.GATEKEEPER;
        private boolean rateLimiterEnabled = false;
        private long permits = 1L;
        private long interval = 1L;
        private TimeUnit intervalUnit = TimeUnit.SECONDS;
        private boolean blacklistEnabled = false;
        private long blacklistCount = 0L;
        private Duration blacklistTimeout = Duration.ofHours(24);
        private boolean enableBlacklistForAllKey = false;
        private boolean timeoutEnabled = false;
        private Duration timeout = Duration.ofMillis(350);
        private TimeoutExecutorType timeoutExecutor = TimeoutExecutorType.THREAD_POOL;
        private boolean fallbackOnException = false;
        private boolean cancelRunningTask = true;
        private String fallbackMethod = "";
        private String returnJson = "";
        private FailStrategy failStrategy = FailStrategy.FAIL_OPEN;

        static RuleSpec defaults(Method method) {
            RuleSpec spec = new RuleSpec();
            spec.name = SensitiveValueHasher.sha256Hex(MethodSignatureKey.from(method).asString()).substring(0, 16);
            return spec;
        }

        AccessGuardRule toRule() {
            return new AccessGuardRule(
                    name,
                    key,
                    keyExpression,
                    whiteListEnabled,
                    whiteListUsers,
                    whiteListMode,
                    rateLimiterEnabled,
                    permits,
                    interval,
                    intervalUnit,
                    blacklistEnabled,
                    blacklistCount,
                    blacklistTimeout,
                    enableBlacklistForAllKey,
                    timeoutEnabled,
                    timeout,
                    timeoutExecutor,
                    fallbackOnException,
                    cancelRunningTask,
                    fallbackMethod,
                    returnJson,
                    failStrategy
            );
        }
    }
}
