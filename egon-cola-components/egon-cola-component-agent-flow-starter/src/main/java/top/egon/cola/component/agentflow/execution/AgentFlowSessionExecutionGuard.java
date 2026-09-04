package top.egon.cola.component.agentflow.execution;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.agentflow.exception.AgentFlowException;
import top.egon.cola.component.agentflow.exception.AgentFlowSessionBusyException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Coordinates atomic per-session execution leases and component shutdown quiescence. */
@Slf4j
public class AgentFlowSessionExecutionGuard {

    private enum State {
        OPEN,
        CLOSING,
        CLOSED
    }

    private final Object monitor = new Object();
    private final Map<SessionKey, Lease> leases = new HashMap<>();
    private State state = State.OPEN;

    public Lease acquire(String flowId, String userId, String sessionId) {
        String normalizedFlowId = required(flowId, "flowId");
        String normalizedUserId = required(userId, "userId");
        String normalizedSessionId = optional(sessionId);
        synchronized (monitor) {
            ensureOpenLocked();
            SessionKey key = new SessionKey(normalizedFlowId, normalizedUserId, normalizedSessionId);
            if (leases.containsKey(key)) {
                throw new AgentFlowSessionBusyException(normalizedFlowId);
            }
            Lease lease = new Lease(key);
            leases.put(key, lease);
            return lease;
        }
    }

    public boolean beginClosing() {
        synchronized (monitor) {
            if (state != State.OPEN) {
                return false;
            }
            state = State.CLOSING;
            monitor.notifyAll();
            return true;
        }
    }

    public boolean awaitQuiescence(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        long deadline = System.nanoTime() + timeout.toNanos();
        synchronized (monitor) {
            while (!leases.isEmpty()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return false;
                }
                try {
                    long millis = remaining / 1_000_000L;
                    int nanos = (int) (remaining % 1_000_000L);
                    monitor.wait(Math.max(1L, millis), nanos);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return true;
        }
    }

    public void markClosed() {
        synchronized (monitor) {
            state = State.CLOSED;
            monitor.notifyAll();
        }
    }

    public void ensureOpen() {
        synchronized (monitor) {
            ensureOpenLocked();
        }
    }

    public int activeCount() {
        synchronized (monitor) {
            return leases.size();
        }
    }

    public boolean isClosed() {
        synchronized (monitor) {
            return state == State.CLOSED;
        }
    }

    private void ensureOpenLocked() {
        if (state != State.OPEN) {
            throw new AgentFlowException("Agent Flow execution guard is not open: " + state);
        }
    }

    private void release(Lease lease) {
        if (!lease.released.compareAndSet(false, true)) {
            return;
        }
        synchronized (monitor) {
            leases.remove(lease.key, lease);
            monitor.notifyAll();
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String optional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private record SessionKey(String flowId, String userId, String sessionId) {
    }

    public final class Lease implements AutoCloseable {
        private final SessionKey key;
        private final AtomicBoolean released = new AtomicBoolean();

        private Lease(SessionKey key) {
            this.key = key;
        }

        @Override
        public void close() {
            release(this);
        }
    }
}
