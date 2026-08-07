package top.egon.cola.component.ddc.admin.service.publish;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PublishCompletionWaiterRegistry {

    private final ConcurrentMap<String, PublishWaiter> waiters = new ConcurrentHashMap<>();

    public PublishWaiter register(String changeId) {
        if (changeId == null || changeId.isBlank()) {
            throw new IllegalArgumentException("changeId is required");
        }
        return waiters.computeIfAbsent(changeId, ignored -> new PublishWaiter());
    }

    public void signal(String changeId) {
        PublishWaiter waiter = waiters.get(changeId);
        if (waiter != null) {
            waiter.signal();
        }
    }

    public void remove(String changeId, PublishWaiter waiter) {
        waiters.remove(changeId, waiter);
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
