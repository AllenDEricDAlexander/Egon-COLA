package top.egon.cola.component.accessguard.execution;

import org.aspectj.lang.ProceedingJoinPoint;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.blacklist.BlacklistService;
import top.egon.cola.component.accessguard.blacklist.BlacklistStatus;
import top.egon.cola.component.accessguard.circuitbreaker.TimeoutCircuitBreakerExecutor;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.config.AccessGuardRuleResolver;
import top.egon.cola.component.accessguard.context.AccessGuardContext;
import top.egon.cola.component.accessguard.context.AccessGuardDecision;
import top.egon.cola.component.accessguard.context.AccessGuardResult;
import top.egon.cola.component.accessguard.event.AccessGuardEvent;
import top.egon.cola.component.accessguard.event.AccessGuardEventPublisher;
import top.egon.cola.component.accessguard.key.AccessKeyResolution;
import top.egon.cola.component.accessguard.key.AccessKeyResolver;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterDecision;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterExecutor;
import top.egon.cola.component.accessguard.reject.RejectResponseInvoker;
import top.egon.cola.component.accessguard.support.AopMethodResolver;
import top.egon.cola.component.accessguard.support.MethodSignatureKey;
import top.egon.cola.component.accessguard.whitelist.WhiteListDecision;
import top.egon.cola.component.accessguard.whitelist.WhiteListService;

import java.lang.reflect.Method;

public class AccessGuardExecutionService {

    private final AccessGuardProperties properties;
    private final AccessGuardRuleResolver ruleResolver;
    private final AccessKeyResolver keyResolver;
    private final WhiteListService whiteListService;
    private final BlacklistService blacklistService;
    private final RateLimiterExecutor rateLimiterExecutor;
    private final TimeoutCircuitBreakerExecutor timeoutCircuitBreakerExecutor;
    private final RejectResponseInvoker rejectResponseInvoker;
    private final AccessGuardEventPublisher eventPublisher;
    private final AccessGuardFailureHandler failureHandler;
    private final AopMethodResolver methodResolver = new AopMethodResolver();

    public AccessGuardExecutionService(
            AccessGuardProperties properties,
            AccessGuardRuleResolver ruleResolver,
            AccessKeyResolver keyResolver,
            WhiteListService whiteListService,
            BlacklistService blacklistService,
            RateLimiterExecutor rateLimiterExecutor,
            TimeoutCircuitBreakerExecutor timeoutCircuitBreakerExecutor,
            RejectResponseInvoker rejectResponseInvoker,
            AccessGuardEventPublisher eventPublisher,
            AccessGuardFailureHandler failureHandler
    ) {
        this.properties = properties;
        this.ruleResolver = ruleResolver;
        this.keyResolver = keyResolver;
        this.whiteListService = whiteListService;
        this.blacklistService = blacklistService;
        this.rateLimiterExecutor = rateLimiterExecutor;
        this.timeoutCircuitBreakerExecutor = timeoutCircuitBreakerExecutor;
        this.rejectResponseInvoker = rejectResponseInvoker;
        this.eventPublisher = eventPublisher;
        this.failureHandler = failureHandler;
    }

    public Object execute(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        Method method = methodResolver.resolve(joinPoint);
        AccessGuardRule rule;
        try {
            rule = ruleResolver.resolve(method);
        } catch (RuntimeException failure) {
            if (failureHandler.failOpen(null, "rule resolution", failure)) {
                return joinPoint.proceed();
            }
            throw failure;
        }

        AccessGuardContext context = new AccessGuardContext();
        context.setRuleName(rule.name());
        context.setMethodSignature(MethodSignatureKey.from(method).asString());

        AccessKeyResolution key;
        try {
            key = keyResolver.resolve(joinPoint, rule);
        } catch (RuntimeException failure) {
            if (failureHandler.failOpen(rule, "key resolution", failure)) {
                return joinPoint.proceed();
            }
            throw failure;
        }
        context.setAccessKey(key.normalizedKey());
        context.setAccessKeyHash(key.keyHash());

        if (rule.whiteListEnabled()) {
            WhiteListDecision decision;
            try {
                decision = whiteListService.check(rule, key.keyHash());
            } catch (RuntimeException failure) {
                if (!failureHandler.failOpen(rule, "white list", failure)) {
                    throw failure;
                }
                decision = WhiteListDecision.pass(rule.whiteListMode());
            }
            if (!decision.passed()) {
                return reject(joinPoint, rule, context,
                        AccessGuardDecision.WHITELIST_REJECTED, decision.reason());
            }
            if (decision.bypassGuard()) {
                Object result = joinPoint.proceed();
                publish(context, AccessGuardResult.pass(rule.name(), key.keyHash()));
                return result;
            }
        }

        if (rule.blacklistEnabled()) {
            BlacklistStatus status;
            try {
                status = blacklistService.status(rule, context);
            } catch (RuntimeException failure) {
                if (!failureHandler.failOpen(rule, "blacklist", failure)) {
                    throw failure;
                }
                status = BlacklistStatus.none();
            }
            if (status.blacklisted()) {
                return reject(joinPoint, rule, context,
                        AccessGuardDecision.BLACKLIST_HIT, status.reason());
            }
        }

        if (rule.rateLimiterEnabled()) {
            RateLimiterDecision decision;
            try {
                decision = rateLimiterExecutor.tryAcquire(rule, context);
            } catch (RuntimeException failure) {
                if (!failureHandler.failOpen(rule, "rate limiter", failure)) {
                    throw failure;
                }
                decision = RateLimiterDecision.allow(0L);
            }
            if (!decision.allowed()) {
                if (rule.blacklistEnabled()) {
                    incrementBlacklist(rule, context);
                }
                return reject(joinPoint, rule, context,
                        AccessGuardDecision.RATE_LIMITED, decision.reason());
            }
        }

        Object result = rule.timeoutEnabled()
                ? timeoutCircuitBreakerExecutor.execute(joinPoint, rule, context)
                : joinPoint.proceed();
        publish(context, AccessGuardResult.pass(rule.name(), key.keyHash()));
        return result;
    }

    private void incrementBlacklist(AccessGuardRule rule, AccessGuardContext context) {
        try {
            blacklistService.incrementRejectAndMaybeBlacklist(rule, context);
        } catch (RuntimeException failure) {
            failureHandler.failOpen(rule, "blacklist increment", failure);
        }
    }

    private Object reject(
            ProceedingJoinPoint joinPoint,
            AccessGuardRule rule,
            AccessGuardContext context,
            AccessGuardDecision decision,
            String message
    ) throws Throwable {
        AccessGuardResult result = AccessGuardResult.reject(
                decision, rule.name(), context.accessKeyHash(), message);
        publish(context, result);
        try {
            return rejectResponseInvoker.reject(joinPoint, rule, context, joinPoint.getArgs());
        } catch (RuntimeException failure) {
            if (failureHandler.failOpen(rule, "reject response", failure)) {
                return joinPoint.proceed();
            }
            throw failure;
        }
    }

    private void publish(AccessGuardContext context, AccessGuardResult result) {
        context.setResult(result);
        try {
            eventPublisher.publish(AccessGuardEvent.from(context));
        } catch (RuntimeException failure) {
            failureHandler.failOpen(null, "event publication", failure);
        }
    }
}
