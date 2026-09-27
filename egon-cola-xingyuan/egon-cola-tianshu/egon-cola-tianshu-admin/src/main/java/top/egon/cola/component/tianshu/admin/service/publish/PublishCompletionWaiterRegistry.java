package top.egon.cola.component.tianshu.admin.service.publish;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component("publishCompletionWaiterRegistry")
public class PublishCompletionWaiterRegistry {

    private final ConcurrentMap<TenantChangeKey, PublishWaiter> waiters = new ConcurrentHashMap<>();

    public PublishWaiter register(String changeId) {
        if (changeId == null || changeId.isBlank()) {
            throw new IllegalArgumentException("changeId is required");
        }
        return waiters.computeIfAbsent(new TenantChangeKey(tenantId(), changeId),
                ignored -> new PublishWaiter());
    }

    public void signal(String changeId) {
        long tenantId = tenantId();
        PublishWaiter waiter = waiters.get(new TenantChangeKey(tenantId, changeId));
        if (waiter != null) {
            waiter.signal();
        } else {
            log.debug("No Tianshu publish completion waiter for tenantId={}", tenantId);
        }
    }

    public void remove(String changeId, PublishWaiter waiter) {
        waiters.remove(new TenantChangeKey(tenantId(), changeId), waiter);
    }

    private long tenantId() {
        String value = MDC.get("tenantId");
        try {
            long tenantId = Long.parseLong(value);
            if (tenantId > 0 && Long.toString(tenantId).equals(value)) {
                return tenantId;
            }
        } catch (NumberFormatException ignored) {
            // A request without trusted tenant MDC is rejected below.
        }
        throw new IllegalStateException("A positive trusted tenantId is required for publish completion");
    }

    private record TenantChangeKey(long tenantId, String changeId) {
    }

    public static final class PublishWaiter {

        private long signalVersion;

        public synchronized long signalVersion() {
            return signalVersion;
        }

        public synchronized void awaitAfter(long observedVersion,
                                            Duration timeout) throws InterruptedException {
            if (signalVersion == observedVersion && !timeout.isNegative() && !timeout.isZero()) {
                wait(Math.max(1, timeout.toMillis()));
            }
        }

        private synchronized void signal() {
            signalVersion++;
            notifyAll();
        }
    }
}
