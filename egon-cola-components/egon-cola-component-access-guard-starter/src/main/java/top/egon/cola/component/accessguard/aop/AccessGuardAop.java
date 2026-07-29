package top.egon.cola.component.accessguard.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import top.egon.cola.component.accessguard.autoconfigure.AccessGuardProperties;
import top.egon.cola.component.accessguard.blacklist.BlacklistService;
import top.egon.cola.component.accessguard.circuitbreaker.TimeoutCircuitBreakerExecutor;
import top.egon.cola.component.accessguard.config.AccessGuardRuleResolver;
import top.egon.cola.component.accessguard.event.AccessGuardEventPublisher;
import top.egon.cola.component.accessguard.execution.AccessGuardExecutionService;
import top.egon.cola.component.accessguard.execution.AccessGuardFailureHandler;
import top.egon.cola.component.accessguard.key.AccessKeyResolver;
import top.egon.cola.component.accessguard.ratelimiter.RateLimiterExecutor;
import top.egon.cola.component.accessguard.reject.RejectResponseInvoker;
import top.egon.cola.component.accessguard.whitelist.WhiteListService;

@Aspect
public class AccessGuardAop implements Ordered {

    private final AccessGuardProperties properties;
    private final AccessGuardExecutionService executionService;

    public AccessGuardAop(
            AccessGuardProperties properties,
            AccessGuardExecutionService executionService
    ) {
        this.properties = properties;
        this.executionService = executionService;
    }

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
        this(properties, new AccessGuardExecutionService(
                properties,
                ruleResolver,
                keyResolver,
                whiteListService,
                blacklistService,
                rateLimiterExecutor,
                timeoutCircuitBreakerExecutor,
                rejectResponseInvoker,
                eventPublisher,
                new AccessGuardFailureHandler(properties)
        ));
    }

    @Around("@annotation(top.egon.cola.component.accessguard.annotation.AccessGuard)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.WhiteListAccessInterceptor)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.RateLimiterAccessInterceptor)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.TimeoutCircuitBreaker)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.DoWhiteList)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.DoRateLimiter)"
            + " || @annotation(top.egon.cola.component.accessguard.annotation.DoHystrix)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        return executionService.execute(joinPoint);
    }

    @Override
    public int getOrder() {
        return properties.getAop().getOrder();
    }
}
