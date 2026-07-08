package top.egon.cola.component.accessguard.ratelimiter;

public record RateLimiterDecision(boolean allowed, long remainingPermits, String reason) {

    public static RateLimiterDecision allow(long remainingPermits) {
        return new RateLimiterDecision(true, remainingPermits, "allowed");
    }

    public static RateLimiterDecision reject(String reason) {
        return new RateLimiterDecision(false, 0L, reason);
    }
}
