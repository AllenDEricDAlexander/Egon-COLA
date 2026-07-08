package top.egon.cola.component.accessguard.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
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

@Aspect
public class AccessGuardAop implements Ordered {

    private final AccessGuardProperties properties;

    private final AccessGuardRuleResolver ruleResolver;

    private final AccessKeyResolver keyResolver;

    private final WhiteListService whiteListService;

    private final BlacklistService blacklistService;

    private final RateLimiterExecutor rateLimiterExecutor;

    private final TimeoutCircuitBreakerExecutor timeoutCircuitBreakerExecutor;

    private final RejectResponseInvoker rejectResponseInvoker;

    private final AccessGuardEventPublisher eventPublisher;

    private final AopMethodResolver methodResolver = new AopMethodResolver();

    public AccessGuardAop(
            AccessGuardProperties properties,
            AccessGuardRuleResolver ruleResolver,
            AccessKeyResolver keyResolver,
            WhiteListService whiteListService,
            BlacklistService blacklistService,
            RateLimiterExecutor rateLimiterExecutor,
            TimeoutCircuitBreakerExecutor timeoutCircuitBreakerExecutor,
            RejectResponseInvoker rejectResponseInvoker,
            AccessGuardEventPublisher eventPublisher
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
    }

    @Around("@annotation(top.egon.cola.component.accessguard.annotation.AccessGuard)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.WhiteListAccessInterceptor)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.RateLimiterAccessInterceptor)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.TimeoutCircuitBreaker)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.DoWhiteList)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.DoRateLimiter)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.DoHystrix)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        Method method = methodResolver.resolve(joinPoint);
        AccessGuardRule rule = ruleResolver.resolve(method);
        AccessGuardContext context = new AccessGuardContext();
        context.setRuleName(rule.name());
        context.setMethodSignature(MethodSignatureKey.from(method).asString());

        AccessKeyResolution key = keyResolver.resolve(joinPoint, rule);
        context.setAccessKey(key.normalizedKey());
        context.setAccessKeyHash(key.keyHash());

        if (rule.whiteListEnabled()) {
            WhiteListDecision whiteListDecision = whiteListService.check(rule, key.keyHash());
            if (!whiteListDecision.passed()) {
                return reject(joinPoint, rule, context, AccessGuardDecision.WHITELIST_REJECTED, whiteListDecision.reason());
            }
            if (whiteListDecision.bypassGuard()) {
                Object result = joinPoint.proceed();
                publish(context, AccessGuardResult.pass(rule.name(), key.keyHash()));
                return result;
            }
        }

        if (rule.blacklistEnabled()) {
            BlacklistStatus blacklistStatus = blacklistService.status(rule, context);
            if (blacklistStatus.blacklisted()) {
                return reject(joinPoint, rule, context, AccessGuardDecision.BLACKLIST_HIT, blacklistStatus.reason());
            }
        }

        if (rule.rateLimiterEnabled()) {
            RateLimiterDecision rateLimiterDecision = rateLimiterExecutor.tryAcquire(rule, context);
            if (!rateLimiterDecision.allowed()) {
                if (rule.blacklistEnabled()) {
                    blacklistService.incrementRejectAndMaybeBlacklist(rule, context);
                }
                return reject(joinPoint, rule, context, AccessGuardDecision.RATE_LIMITED, rateLimiterDecision.reason());
            }
        }

        Object result = rule.timeoutEnabled()
                ? timeoutCircuitBreakerExecutor.execute(joinPoint, rule, context)
                : joinPoint.proceed();
        publish(context, AccessGuardResult.pass(rule.name(), key.keyHash()));
        return result;
    }

    @Override
    public int getOrder() {
        return properties.getAop().getOrder();
    }

    private Object reject(
            ProceedingJoinPoint joinPoint,
            AccessGuardRule rule,
            AccessGuardContext context,
            AccessGuardDecision decision,
            String message
    ) {
        AccessGuardResult result = AccessGuardResult.reject(decision, rule.name(), context.accessKeyHash(), message);
        publish(context, result);
        return rejectResponseInvoker.reject(joinPoint, rule, context, joinPoint.getArgs());
    }

    private void publish(AccessGuardContext context, AccessGuardResult result) {
        context.setResult(result);
        eventPublisher.publish(AccessGuardEvent.from(context));
    }
}
