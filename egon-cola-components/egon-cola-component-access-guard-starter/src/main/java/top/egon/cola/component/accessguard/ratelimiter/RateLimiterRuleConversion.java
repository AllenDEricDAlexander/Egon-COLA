package top.egon.cola.component.accessguard.ratelimiter;

import java.util.concurrent.TimeUnit;

public record RateLimiterRuleConversion(long permits, long interval, TimeUnit intervalUnit) {

    public static RateLimiterRuleConversion fromPermitsPerSecond(double permitsPerSecond) {
        if (permitsPerSecond <= 0) {
            return new RateLimiterRuleConversion(1L, 1L, TimeUnit.SECONDS);
        }
        if (permitsPerSecond < 1.0d) {
            return new RateLimiterRuleConversion(1L, Math.max(1L, Math.round(1.0d / permitsPerSecond)), TimeUnit.SECONDS);
        }
        return new RateLimiterRuleConversion(Math.max(1L, Math.round(permitsPerSecond)), 1L, TimeUnit.SECONDS);
    }
}
