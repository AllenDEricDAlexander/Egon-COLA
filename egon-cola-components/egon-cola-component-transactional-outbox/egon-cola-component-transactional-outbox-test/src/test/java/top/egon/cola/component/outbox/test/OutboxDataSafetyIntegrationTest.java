package top.egon.cola.component.outbox.test;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.delivery.DefaultDeliveryFailureClassifier;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.outbox.event.OutboxDeadLetterEvent;
import top.egon.cola.component.outbox.exception.OutboxValidationException;
import top.egon.cola.component.outbox.observability.MicrometerOutboxMetrics;
import top.egon.cola.component.outbox.serialization.SerializedOutboxPayload;
import top.egon.cola.component.outbox.store.OutboxRecord;
import top.egon.cola.component.outbox.store.PostgresqlJdbcOutboxStore;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxDataSafetyIntegrationTest extends PostgresqlOutboxTestSupport {

    @Test
    void shouldPersistOnlySanitizedBoundedFailureSummaries() {
        PostgresqlJdbcOutboxStore store = outboxStore();
        store.enqueue(newRecord("message-1"));
        OutboxRecord claimed =
                store.claimDue(1, "node-a:claim-1", Duration.ofSeconds(60)).getFirst();
        DeliveryResult classified = new DefaultDeliveryFailureClassifier().classify(
                new IllegalStateException("Cookie=should-not-persist")
        );

        store.markRetry(
                claimed.id(),
                claimed.lockedBy(),
                Duration.ofSeconds(1),
                classified.code(),
                classified.message()
        );

        String retryMessage = jdbcTemplate.queryForObject("""
                select last_error_message
                from egon_cola_outbox_message
                where message_id = 'message-1'
                """, String.class);
        assertThat(retryMessage)
                .doesNotContain("Cookie", "should-not-persist")
                .isEqualTo("IllegalStateException");

        jdbcTemplate.update("""
                update egon_cola_outbox_message
                set next_attempt_at = clock_timestamp() - interval '1 second'
                where message_id = 'message-1'
                """);
        OutboxRecord reclaimed =
                store.claimDue(1, "node-a:claim-2", Duration.ofSeconds(60)).getFirst();
        String unsafe = ("control\n\r\t" + "x".repeat(2_100));
        store.markDead(reclaimed.id(), reclaimed.lockedBy(), "PERMANENT", unsafe);

        String deadMessage = jdbcTemplate.queryForObject("""
                select last_error_message
                from egon_cola_outbox_message
                where message_id = 'message-1'
                """, String.class);
        assertThat(deadMessage)
                .doesNotContain("Authorization", "Cookie", "should-not-persist")
                .doesNotContain("\n", "\r")
                .hasSizeLessThanOrEqualTo(2_000);
    }

    @Test
    void shouldRejectSensitiveHeadersAndKeepEventsAndMetricsLowCardinality() {
        OutboxMessageValidator validator =
                new OutboxMessageValidator(objectMapper, 1_024, 10, 1_024);
        OutboxMessage message = OutboxMessage.builder()
                .channel("test")
                .destination("orders")
                .payload(Map.of("id", 1))
                .header("Authorization", "secret")
                .build();

        assertThatThrownBy(() -> validator.validateEnvelope(message))
                .isInstanceOf(OutboxValidationException.class);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from egon_cola_outbox_message",
                Integer.class
        )).isZero();

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerOutboxMetrics metrics = new MicrometerOutboxMetrics(registry);
        metrics.delivery("http", "success", Duration.ofMillis(1));
        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags())
                .extracting(Tag::getKey)
                .doesNotContain("messageId", "idempotencyKey", "destination", "url");
        assertThat(OutboxDeadLetterEvent.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("payload", "headers");

        validator.validateSerialized(new SerializedOutboxPayload("{}", 2), Map.of());
    }
}
