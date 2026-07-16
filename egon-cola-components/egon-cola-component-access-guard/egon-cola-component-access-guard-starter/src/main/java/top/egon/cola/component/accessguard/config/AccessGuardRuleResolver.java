package top.egon.cola.component.accessguard.config;

import top.egon.cola.component.accessguard.annotation.FailStrategy;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.support.ExecutableSignatureKey;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

public class AccessGuardRuleResolver {

    private final AccessGuardProperties properties;

    private final AccessGuardConfigProvider configProvider;

    private final AccessGuardAnnotationResolver annotationResolver;

    public AccessGuardRuleResolver() {
        this(new AccessGuardProperties(), new DefaultAccessGuardConfigProvider(), new AccessGuardAnnotationResolver());
    }

    public AccessGuardRuleResolver(
            AccessGuardProperties properties,
            AccessGuardConfigProvider configProvider,
            AccessGuardAnnotationResolver annotationResolver
    ) {
        this.properties = properties;
        this.configProvider = configProvider;
        this.annotationResolver = annotationResolver;
    }

    public AccessGuardRule resolve(Method method) {
        return resolve((Executable) method);
    }

    public AccessGuardRule resolve(Executable executable) {
        AccessGuardRule rule = applyProperties(annotationResolver.resolve(executable));
        Optional<AccessGuardRuleOverride> globalOverride = configProvider.findGlobalOverride();
        if (globalOverride.isPresent()) {
            rule = applyOverride(rule, globalOverride.get());
        }
        String methodSignature = ExecutableSignatureKey.from(executable).asString();
        Optional<AccessGuardRuleOverride> methodOverride = configProvider.findMethodOverride(rule.name(), methodSignature);
        if (methodOverride.isPresent()) {
            rule = applyOverride(rule, methodOverride.get());
        }
        return normalizeFailStrategy(rule);
    }

    private AccessGuardRule normalizeFailStrategy(AccessGuardRule rule) {
        FailStrategy strategy = rule.failStrategy();
        if (strategy == null || strategy == FailStrategy.GLOBAL_DEFAULT) {
            strategy = properties.getFailStrategy();
        }
        if (strategy == null || strategy == FailStrategy.GLOBAL_DEFAULT) {
            strategy = FailStrategy.FAIL_OPEN;
        }
        return new AccessGuardRule(
                rule.name(), rule.key(), rule.keyExpression(), rule.whiteListEnabled(),
                rule.whiteListUsers(), rule.whiteListMode(), rule.rateLimiterEnabled(),
                rule.permits(), rule.interval(), rule.intervalUnit(), rule.blacklistEnabled(),
                rule.blacklistCount(), rule.blacklistTimeout(), rule.enableBlacklistForAllKey(),
                rule.timeoutEnabled(), rule.timeout(), rule.timeoutExecutor(),
                rule.fallbackOnException(), rule.cancelRunningTask(), rule.fallbackMethod(),
                rule.returnJson(), strategy
        );
    }

    private AccessGuardRule applyProperties(AccessGuardRule rule) {
        Optional<AccessGuardProperties.Rule> matchedRule = properties.getRules().stream()
                .filter(propertyRule -> propertyRule.getName().equals(rule.name()))
                .findFirst();
        if (matchedRule.isEmpty()) {
            return rule;
        }
        AccessGuardProperties.Rule propertyRule = matchedRule.get();
        return new AccessGuardRule(
                propertyRule.getName(),
                propertyRule.getKey(),
                propertyRule.getKeyExpression(),
                propertyRule.getWhiteList().isEnabled(),
                propertyRule.getWhiteList().getUsers(),
                propertyRule.getWhiteList().getMode(),
                propertyRule.getRateLimiter().isEnabled(),
                propertyRule.getRateLimiter().getPermits(),
                propertyRule.getRateLimiter().getInterval(),
                propertyRule.getRateLimiter().getIntervalUnit(),
                propertyRule.getRateLimiter().isBlacklistEnabled(),
                propertyRule.getRateLimiter().getBlacklistCount(),
                propertyRule.getRateLimiter().getBlacklistTimeout(),
                propertyRule.getRateLimiter().isEnableBlacklistForAllKey(),
                propertyRule.getCircuitBreaker().isEnabled(),
                propertyRule.getCircuitBreaker().getTimeout(),
                propertyRule.getCircuitBreaker().getExecutor(),
                propertyRule.getCircuitBreaker().isFallbackOnException(),
                propertyRule.getCircuitBreaker().isCancelRunningTask(),
                propertyRule.getFallbackMethod(),
                propertyRule.getReturnJson(),
                propertyRule.getFailStrategy()
        );
    }

    private AccessGuardRule applyOverride(AccessGuardRule rule, AccessGuardRuleOverride override) {
        return new AccessGuardRule(
                value(override.name(), rule.name()),
                value(override.key(), rule.key()),
                value(override.keyExpression(), rule.keyExpression()),
                value(override.whiteListEnabled(), rule.whiteListEnabled()),
                value(override.whiteListUsers(), rule.whiteListUsers()),
                value(override.whiteListMode(), rule.whiteListMode()),
                value(override.rateLimiterEnabled(), rule.rateLimiterEnabled()),
                value(override.permits(), rule.permits()),
                value(override.interval(), rule.interval()),
                value(override.intervalUnit(), rule.intervalUnit()),
                value(override.blacklistEnabled(), rule.blacklistEnabled()),
                value(override.blacklistCount(), rule.blacklistCount()),
                value(override.blacklistTimeout(), rule.blacklistTimeout()),
                value(override.enableBlacklistForAllKey(), rule.enableBlacklistForAllKey()),
                value(override.timeoutEnabled(), rule.timeoutEnabled()),
                value(override.timeout(), rule.timeout()),
                value(override.timeoutExecutor(), rule.timeoutExecutor()),
                value(override.fallbackOnException(), rule.fallbackOnException()),
                value(override.cancelRunningTask(), rule.cancelRunningTask()),
                value(override.fallbackMethod(), rule.fallbackMethod()),
                value(override.returnJson(), rule.returnJson()),
                value(override.failStrategy(), rule.failStrategy())
        );
    }

    private <T> T value(T override, T current) {
        return override == null ? current : override;
    }

    private boolean value(Boolean override, boolean current) {
        return override == null ? current : override;
    }

    private long value(Long override, long current) {
        return override == null ? current : override;
    }

    private Duration value(Duration override, Duration current) {
        return override == null ? current : override;
    }
}
