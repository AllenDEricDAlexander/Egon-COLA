package top.egon.cola.component.accessguard.ratelimiter;

import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;

public interface RateLimiterExecutor {

    RateLimiterDecision tryAcquire(AccessGuardRule rule, AccessGuardContext context);
}
