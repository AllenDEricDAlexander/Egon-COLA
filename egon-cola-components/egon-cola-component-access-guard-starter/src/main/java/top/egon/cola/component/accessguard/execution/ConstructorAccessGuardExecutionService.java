package top.egon.cola.component.accessguard.execution;

import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.blacklist.BlacklistService;
import top.egon.cola.component.accessguard.blacklist.BlacklistStatus;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.config.AccessGuardRuleResolver;
import top.egon.cola.component.accessguard.context.AccessGuardContext;
import top.egon.cola.component.accessguard.context.AccessGuardDecision;
import top.egon.cola.component.accessguard.context.AccessGuardResult;
import top.egon.cola.component.accessguard.event.AccessGuardEvent;
import top.egon.cola.component.accessguard.event.AccessGuardEventPublisher;
import top.egon.cola.component.accessguard.exception.AccessGuardRejectedException;
import top.egon.cola.component.accessguard.key.AccessKeyResolution;
import top.egon.cola.component.accessguard.key.ExecutableAccessKeyResolver;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterDecision;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterExecutor;
import top.egon.cola.component.accessguard.support.ExecutableSignatureKey;
import top.egon.cola.component.accessguard.whitelist.WhiteListDecision;
import top.egon.cola.component.accessguard.whitelist.WhiteListService;

import java.lang.reflect.Constructor;

public class ConstructorAccessGuardExecutionService {

    private final AccessGuardProperties properties;
    private final AccessGuardRuleResolver ruleResolver;
    private final ConstructorAccessGuardValidator validator;
    private final ExecutableAccessKeyResolver keyResolver;
    private final WhiteListService whiteListService;
    private final BlacklistService blacklistService;
    private final RateLimiterExecutor rateLimiterExecutor;
    private final AccessGuardEventPublisher eventPublisher;
    private final AccessGuardFailureHandler failureHandler;

    public ConstructorAccessGuardExecutionService(
            AccessGuardProperties properties,
            AccessGuardRuleResolver ruleResolver,
            ConstructorAccessGuardValidator validator,
            ExecutableAccessKeyResolver keyResolver,
            WhiteListService whiteListService,
            BlacklistService blacklistService,
            RateLimiterExecutor rateLimiterExecutor,
            AccessGuardEventPublisher eventPublisher,
            AccessGuardFailureHandler failureHandler
    ) {
        this.properties = properties;
        this.ruleResolver = ruleResolver;
        this.validator = validator;
        this.keyResolver = keyResolver;
        this.whiteListService = whiteListService;
        this.blacklistService = blacklistService;
        this.rateLimiterExecutor = rateLimiterExecutor;
        this.eventPublisher = eventPublisher;
        this.failureHandler = failureHandler;
    }

    public ConstructorGuardResult evaluate(Constructor<?> constructor, Object[] arguments) {
        if (!properties.isEnabled()) {
            return ConstructorGuardResult.allow();
        }
        AccessGuardRule rule;
        try {
            rule = ruleResolver.resolve(constructor);
            validator.validate(constructor, rule);
        } catch (RuntimeException failure) {
            return infrastructure(null, "constructor rule validation", failure);
        }

        AccessGuardContext context = new AccessGuardContext();
        context.setRuleName(rule.name());
        context.setMethodSignature(ExecutableSignatureKey.from(constructor).asString());
        AccessKeyResolution key;
        try {
            key = keyResolver.resolve(constructor, arguments, rule);
        } catch (RuntimeException failure) {
            return infrastructure(rule, "constructor key resolution", failure);
        }
        context.setAccessKey(key.normalizedKey());
        context.setAccessKeyHash(key.keyHash());

        if (rule.whiteListEnabled()) {
            WhiteListDecision decision;
            try {
                decision = whiteListService.check(rule, key.keyHash());
            } catch (RuntimeException failure) {
                ConstructorGuardResult result = infrastructure(
                        rule, "constructor white list", failure);
                if (!result.allowed()) {
                    return result;
                }
                decision = WhiteListDecision.pass(rule.whiteListMode());
            }
            if (!decision.passed()) {
                return reject(rule, context, AccessGuardDecision.WHITELIST_REJECTED,
                        decision.reason());
            }
            if (decision.bypassGuard()) {
                publish(rule, context, AccessGuardResult.pass(rule.name(), key.keyHash()));
                return ConstructorGuardResult.allow();
            }
        }

        if (rule.blacklistEnabled()) {
            BlacklistStatus status;
            try {
                status = blacklistService.status(rule, context);
            } catch (RuntimeException failure) {
                ConstructorGuardResult result = infrastructure(
                        rule, "constructor blacklist", failure);
                if (!result.allowed()) {
                    return result;
                }
                status = BlacklistStatus.none();
            }
            if (status.blacklisted()) {
                return reject(rule, context, AccessGuardDecision.BLACKLIST_HIT,
                        status.reason());
            }
        }

        if (rule.rateLimiterEnabled()) {
            RateLimiterDecision decision;
            try {
                decision = rateLimiterExecutor.tryAcquire(rule, context);
            } catch (RuntimeException failure) {
                ConstructorGuardResult result = infrastructure(
                        rule, "constructor rate limiter", failure);
                if (!result.allowed()) {
                    return result;
                }
                decision = RateLimiterDecision.allow(0L);
            }
            if (!decision.allowed()) {
                if (rule.blacklistEnabled()) {
                    incrementBlacklist(rule, context);
                }
                return reject(rule, context, AccessGuardDecision.RATE_LIMITED,
                        decision.reason());
            }
        }

        publish(rule, context, AccessGuardResult.pass(rule.name(), key.keyHash()));
        return ConstructorGuardResult.allow();
    }

    private ConstructorGuardResult reject(
            AccessGuardRule rule,
            AccessGuardContext context,
            AccessGuardDecision decision,
            String message
    ) {
        publish(rule, context, AccessGuardResult.reject(
                decision, rule.name(), context.accessKeyHash(), message));
        return ConstructorGuardResult.reject(new AccessGuardRejectedException(
                "Access Guard rejected constructor " + context.methodSignature()
                        + ": " + message));
    }

    private void incrementBlacklist(AccessGuardRule rule, AccessGuardContext context) {
        try {
            blacklistService.incrementRejectAndMaybeBlacklist(rule, context);
        } catch (RuntimeException failure) {
            failureHandler.failOpen(rule, "constructor blacklist increment", failure);
        }
    }

    private void publish(
            AccessGuardRule rule,
            AccessGuardContext context,
            AccessGuardResult result
    ) {
        context.setResult(result);
        try {
            eventPublisher.publish(AccessGuardEvent.from(context));
        } catch (RuntimeException failure) {
            failureHandler.failOpen(rule, "constructor event publication", failure);
        }
    }

    private ConstructorGuardResult infrastructure(
            AccessGuardRule rule,
            String stage,
            RuntimeException failure
    ) {
        if (failureHandler.failOpen(rule, stage, failure)) {
            return ConstructorGuardResult.allow();
        }
        return ConstructorGuardResult.reject(failure);
    }
}
