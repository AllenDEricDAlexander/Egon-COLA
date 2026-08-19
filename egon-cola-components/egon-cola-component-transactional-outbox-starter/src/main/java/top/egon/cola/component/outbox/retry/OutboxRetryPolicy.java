package top.egon.cola.component.outbox.retry;

import java.time.Duration;

public interface OutboxRetryPolicy {

    Duration nextDelay(int attempt);
}
