package top.egon.cola.component.outbox.observability;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerOutboxMetricsTest {

    @Test
    void shouldRecordOnlyLowCardinalityTagsAndCachedBacklog() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerOutboxMetrics metrics = new MicrometerOutboxMetrics(registry);

        metrics.enqueue(true);
        metrics.claimed(2);
        metrics.delivery("http", "success", Duration.ofMillis(12));
        metrics.updateBacklog(7);

        assertThat(registry.get("egon.cola.outbox.enqueue")
                .tag("result", "created").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("egon.cola.outbox.claim").counter().count()).isEqualTo(2.0);
        assertThat(registry.get("egon.cola.outbox.delivery")
                .tag("channel", "http").tag("result", "success")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.get("egon.cola.outbox.backlog").gauge().value())
                .isEqualTo(7.0);
        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags())
                .extracting(Tag::getKey)
                .doesNotContain("messageId", "idempotencyKey", "destination", "url");
    }
}
