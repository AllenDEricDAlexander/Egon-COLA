package top.egon.cola.component.outbox.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public class MicrometerOutboxMetrics implements OutboxMetrics {

    private final MeterRegistry registry;
    private final AtomicLong backlog = new AtomicLong();

    public MicrometerOutboxMetrics(MeterRegistry registry) {
        this.registry = registry;
        registry.gauge("egon.cola.outbox.backlog", backlog);
    }

    @Override
    public void enqueue(boolean created) {
        registry.counter(
                "egon.cola.outbox.enqueue",
                "result",
                created ? "created" : "existing"
        ).increment();
    }

    @Override
    public void claimed(int count) {
        registry.counter("egon.cola.outbox.claim").increment(count);
    }

    @Override
    public void delivery(String channel, String result, Duration duration) {
        registry.counter(
                "egon.cola.outbox.delivery",
                "channel",
                channel,
                "result",
                result
        ).increment();
        Timer.builder("egon.cola.outbox.delivery.duration")
                .tag("channel", channel)
                .tag("result", result)
                .register(registry)
                .record(duration);
    }

    @Override
    public void retry(String channel) {
        registry.counter("egon.cola.outbox.retry", "channel", channel).increment();
    }

    @Override
    public void dead(String channel) {
        registry.counter("egon.cola.outbox.dead", "channel", channel).increment();
    }

    @Override
    public void leaseLost() {
        registry.counter("egon.cola.outbox.lease_lost").increment();
    }

    @Override
    public void wakeupRejected() {
        registry.counter(
                "egon.cola.outbox.delivery",
                "channel",
                "internal",
                "result",
                "wakeup_rejected"
        ).increment();
    }

    @Override
    public void updateBacklog(long value) {
        backlog.set(value);
    }
}
