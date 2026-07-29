package top.egon.cola.component.accessguard.ratelimiter;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import top.egon.cola.component.accessguard.annotation.FailStrategy;
import top.egon.cola.component.accessguard.config.AccessGuardRule;
import top.egon.cola.component.accessguard.context.AccessGuardContext;
import top.egon.cola.component.accessguard.support.AccessGuardRedisKeys;

import java.util.concurrent.TimeUnit;

public class RedissonRateLimiterExecutor implements RateLimiterExecutor {

    private final RedissonClient redissonClient;

    private final AccessGuardRedisKeys redisKeys;

    private final RateLimiterExecutor localFallback;

    public RedissonRateLimiterExecutor(RedissonClient redissonClient, AccessGuardRedisKeys redisKeys, RateLimiterExecutor localFallback) {
        this.redissonClient = redissonClient;
        this.redisKeys = redisKeys;
        this.localFallback = localFallback;
    }

    @Override
    public RateLimiterDecision tryAcquire(AccessGuardRule rule, AccessGuardContext context) {
        if (!rule.rateLimiterEnabled()) {
            return RateLimiterDecision.allow(Long.MAX_VALUE);
        }
        try {
            RRateLimiter rateLimiter = redissonClient.getRateLimiter(redisKeys.limiter(rule.name(), context.accessKeyHash()));
            rateLimiter.trySetRate(RateType.OVERALL, rule.permits(), rule.interval(), toRateIntervalUnit(rule.intervalUnit()));
            if (rateLimiter.tryAcquire()) {
                return RateLimiterDecision.allow(rateLimiter.availablePermits());
            }
            return RateLimiterDecision.reject("redisson rate limited");
        } catch (RuntimeException e) {
            return onRedissonError(rule, context);
        }
    }

    private RateLimiterDecision onRedissonError(AccessGuardRule rule, AccessGuardContext context) {
        if (rule.failStrategy() == FailStrategy.FAIL_CLOSED) {
            return RateLimiterDecision.reject("redisson error");
        }
        if (rule.failStrategy() == FailStrategy.LOCAL_FALLBACK) {
            return localFallback.tryAcquire(rule, context);
        }
        return RateLimiterDecision.allow(Long.MAX_VALUE);
    }

    private RateIntervalUnit toRateIntervalUnit(TimeUnit timeUnit) {
        return switch (timeUnit) {
            case NANOSECONDS, MICROSECONDS, MILLISECONDS -> RateIntervalUnit.MILLISECONDS;
            case SECONDS -> RateIntervalUnit.SECONDS;
            case MINUTES -> RateIntervalUnit.MINUTES;
            case HOURS -> RateIntervalUnit.HOURS;
            case DAYS -> RateIntervalUnit.DAYS;
        };
    }
}
