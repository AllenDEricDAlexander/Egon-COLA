package top.egon.cola.component.outbox.observability;

import java.time.Duration;

public class NoopOutboxMetrics implements OutboxMetrics {

    @Override
    public void enqueue(boolean created) {
    }

    @Override
    public void claimed(int count) {
    }

    @Override
    public void delivery(String channel, String result, Duration duration) {
    }

    @Override
    public void retry(String channel) {
    }

    @Override
    public void dead(String channel) {
    }

    @Override
    public void leaseLost() {
    }

    @Override
    public void wakeupRejected() {
    }

    @Override
    public void updateBacklog(long value) {
    }
}
