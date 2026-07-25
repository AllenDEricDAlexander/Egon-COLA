package top.egon.cola.component.gateway.admin.infrastructure.messaging;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;

public final class GatewayKafkaConsumerMetrics {

    private final Counter retries;

    private final Counter deadLetters;

    private final Counter workerRestarts;

    private final AtomicLong eventLagMs = new AtomicLong();

    private final Clock clock;

    public GatewayKafkaConsumerMetrics(
            MeterRegistry registry,
            Clock clock) {
        this.clock = clock;
        retries = registry.counter(
                "gateway.admin.kafka.consumer.retries"
        );
        deadLetters = registry.counter(
                "gateway.admin.kafka.consumer.dead.letters"
        );
        workerRestarts = registry.counter(
                "gateway.admin.kafka.consumer.worker.restarts"
        );
        Gauge.builder(
                        "gateway.admin.kafka.consumer.event.lag",
                        eventLagMs,
                        AtomicLong::get
                )
                .baseUnit("milliseconds")
                .register(registry);
    }

    void retry() {
        retries.increment();
    }

    void deadLetter() {
        deadLetters.increment();
    }

    void workerRestart() {
        workerRestarts.increment();
    }

    void observed(long recordTimestamp) {
        if (recordTimestamp < 0) {
            return;
        }
        eventLagMs.set(Math.max(
                0,
                clock.millis() - recordTimestamp
        ));
    }
}
