package top.egon.cola.component.outbox.observability;

import java.time.Duration;

public interface OutboxMetrics {

    void enqueue(boolean created);

    void claimed(int count);

    void delivery(String channel, String result, Duration duration);

    void retry(String channel);

    void dead(String channel);

    void leaseLost();

    void wakeupRejected();

    void updateBacklog(long value);
}
