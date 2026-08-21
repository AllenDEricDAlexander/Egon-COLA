package top.egon.cola.component.rpc.consumer.invocation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Mutable state owned by one logical invocation. */
public final class RpcInvocationContext {

    private final String invocationId = UUID.randomUUID().toString();
    private final long deadlineNanos;
    private final int maxAttempts;
    private final Set<String> excluded = new HashSet<>();
    private final AtomicBoolean terminal = new AtomicBoolean();
    private int attempts;
    private boolean sawAvailability;
    private boolean allRateLimited = true;

    public RpcInvocationContext(long nowNanos, long timeoutMs, int retries) {
        if (timeoutMs <= 0 || retries < 0) {
            throw new IllegalArgumentException("invalid invocation deadline or retries");
        }
        long timeoutNanos;
        try {
            timeoutNanos = Math.multiplyExact(timeoutMs, 1_000_000L);
        } catch (ArithmeticException exception) {
            timeoutNanos = Long.MAX_VALUE;
        }
        deadlineNanos = nowNanos > Long.MAX_VALUE - timeoutNanos
                ? Long.MAX_VALUE : nowNanos + timeoutNanos;
        maxAttempts = retries == Integer.MAX_VALUE ? Integer.MAX_VALUE : retries + 1;
    }

    public String invocationId() {
        return invocationId;
    }

    public synchronized boolean beginAttempt(String endpointKey) {
        if (terminal.get() || attempts >= maxAttempts || !excluded.add(endpointKey)) {
            return false;
        }
        attempts++;
        return true;
    }

    public synchronized int attempts() {
        return attempts;
    }

    public synchronized Set<String> excluded() {
        return Set.copyOf(excluded);
    }

    public synchronized boolean hasAttemptBudget() {
        return attempts < maxAttempts;
    }

    public synchronized void recordAvailability(boolean rateLimited) {
        sawAvailability = true;
        allRateLimited &= rateLimited;
    }

    public synchronized boolean sawAvailability() {
        return sawAvailability;
    }

    public synchronized boolean allRateLimited() {
        return sawAvailability && allRateLimited;
    }

    public long remainingNanos(long nowNanos) {
        if (deadlineNanos == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        if (nowNanos >= deadlineNanos) {
            return 0L;
        }
        return deadlineNanos - nowNanos;
    }

    public boolean expired(long nowNanos) {
        return remainingNanos(nowNanos) == 0;
    }

    public boolean terminate() {
        return terminal.compareAndSet(false, true);
    }

    public boolean terminal() {
        return terminal.get();
    }
}
