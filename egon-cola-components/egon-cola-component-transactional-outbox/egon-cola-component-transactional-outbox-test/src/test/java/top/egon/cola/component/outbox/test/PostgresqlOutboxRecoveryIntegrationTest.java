package top.egon.cola.component.outbox.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.api.UuidOutboxIdGenerator;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.deadletter.OutboxDeadLetterNotifier;
import top.egon.cola.component.outbox.delivery.DefaultDeliveryFailureClassifier;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryHandlerRegistry;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.outbox.dispatch.OutboxDispatcher;
import top.egon.cola.component.outbox.dispatch.OutboxWorkerIdentity;
import top.egon.cola.component.outbox.observability.NoopOutboxMetrics;
import top.egon.cola.component.outbox.serialization.JacksonOutboxMessageSerializer;
import top.egon.cola.component.outbox.store.OutboxRecord;
import top.egon.cola.component.outbox.store.OutboxStatus;
import top.egon.cola.component.outbox.store.PostgresqlJdbcOutboxStore;
import top.egon.cola.component.outbox.transaction.DefaultTransactionalOutbox;
import top.egon.cola.component.outbox.transaction.OutboxAfterCommitBuffer;
import top.egon.cola.component.outbox.transaction.OutboxTransactionGuard;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresqlOutboxRecoveryIntegrationTest extends PostgresqlOutboxTestSupport {

    private PostgresqlJdbcOutboxStore store;
    private final Map<String, DeliveryResult> results = new ConcurrentHashMap<>();

    @BeforeEach
    void setUpStore() {
        store = outboxStore();
    }

    @Test
    void shouldRespectDueTimeAndRecoverOnlyExpiredLeases() {
        store.enqueue(newRecord("future"));
        store.enqueue(newRecord("retry"));
        store.enqueue(newRecord("active"));
        store.enqueue(newRecord("expired"));
        jdbcTemplate.update("""
                update egon_cola_outbox_message
                set next_attempt_at = clock_timestamp() + interval '1 hour'
                where message_id = 'future'
                """);
        jdbcTemplate.update("""
                update egon_cola_outbox_message
                set status = 'RETRY_WAIT',
                    next_attempt_at = clock_timestamp() - interval '1 second'
                where message_id = 'retry'
                """);
        jdbcTemplate.update("""
                update egon_cola_outbox_message
                set status = 'PROCESSING', locked_by = 'old',
                    locked_until = clock_timestamp() + interval '1 hour',
                    attempt_count = 1
                where message_id = 'active'
                """);
        jdbcTemplate.update("""
                update egon_cola_outbox_message
                set status = 'PROCESSING', locked_by = 'old',
                    locked_until = clock_timestamp() - interval '1 second',
                    attempt_count = 1
                where message_id = 'expired'
                """);

        List<OutboxRecord> claimed =
                store.claimDue(10, "node-a:new-token", Duration.ofSeconds(60));

        assertThat(claimed).extracting(OutboxRecord::messageId)
                .containsExactlyInAnyOrder("retry", "expired");
        assertThat(claimed.stream()
                .filter(record -> record.messageId().equals("expired"))
                .findFirst().orElseThrow().attemptCount()).isEqualTo(2);
    }

    @Test
    void shouldRejectCompletionFromAnOldClaimToken() {
        store.enqueue(newRecord("message-1"));
        OutboxRecord oldClaim =
                store.claimDue(1, "node-a:old-token", Duration.ofMillis(1)).getFirst();
        jdbcTemplate.update("""
                update egon_cola_outbox_message
                set locked_until = clock_timestamp() - interval '1 second'
                where message_id = 'message-1'
                """);
        OutboxRecord newClaim =
                store.claimDue(1, "node-a:new-token", Duration.ofSeconds(60)).getFirst();

        assertThat(store.markSucceeded(oldClaim.id(), oldClaim.lockedBy())).isFalse();
        assertThat(store.markSucceeded(newClaim.id(), newClaim.lockedBy())).isTrue();
    }

    @Test
    void shouldRecoverFromPostCommitWakeFailureThroughPolling() {
        DeliveryHandlerRegistry registry = registry();
        TransactionalOutboxProperties properties = properties();
        ApplicationEventPublisher failingPublisher = event -> {
            throw new IllegalStateException("wake failed");
        };
        TransactionalOutbox outbox = new DefaultTransactionalOutbox(
                new OutboxMessageValidator(objectMapper, 1_048_576, 64, 16_384),
                new JacksonOutboxMessageSerializer(objectMapper),
                new UuidOutboxIdGenerator(),
                objectMapper,
                new OutboxTransactionGuard(dataSource),
                store,
                registry,
                new OutboxAfterCommitBuffer(failingPublisher, new NoopOutboxMetrics()),
                new NoopOutboxMetrics(),
                properties
        );

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                outbox.enqueue(message("message-1", "key-1")));

        assertThat(status("message-1")).isEqualTo(OutboxStatus.PENDING);
        results.put("message-1", DeliveryResult.success());
        dispatcher(registry, properties).submitDue();
        assertThat(status("message-1")).isEqualTo(OutboxStatus.SUCCEEDED);
    }

    @Test
    void shouldIsolatePermanentFailureAndCompleteHealthyRecord() {
        store.enqueue(newRecord("poison"));
        store.enqueue(newRecord("healthy"));
        results.put("poison", DeliveryResult.permanentFailure("INVALID", "invalid"));
        results.put("healthy", DeliveryResult.success());

        dispatcher(registry(), properties()).submitDue();

        assertThat(status("poison")).isEqualTo(OutboxStatus.DEAD);
        assertThat(status("healthy")).isEqualTo(OutboxStatus.SUCCEEDED);
    }

    @Test
    void shouldRetryUsingDatabaseTimeThenDeadLetterAtMaximumAttempt() {
        store.enqueue(newRecord("message-1", "key-1", "a".repeat(64), 2));
        results.put(
                "message-1",
                DeliveryResult.retryableFailure("TEMPORARY", "temporary")
        );
        OutboxDispatcher dispatcher = dispatcher(registry(), properties());

        dispatcher.submitDue();

        assertThat(status("message-1")).isEqualTo(OutboxStatus.RETRY_WAIT);
        assertThat(jdbcTemplate.queryForObject("""
                select next_attempt_at > clock_timestamp()
                from egon_cola_outbox_message
                where message_id = 'message-1'
                """, Boolean.class)).isTrue();
        jdbcTemplate.update("""
                update egon_cola_outbox_message
                set next_attempt_at = clock_timestamp() - interval '1 second'
                where message_id = 'message-1'
                """);

        dispatcher.submitDue();

        assertThat(status("message-1")).isEqualTo(OutboxStatus.DEAD);
        assertThat(jdbcTemplate.queryForObject("""
                select last_error_code
                from egon_cola_outbox_message
                where message_id = 'message-1'
                """, String.class)).isEqualTo("OUTBOX_RETRY_EXHAUSTED");
    }

    private DeliveryHandlerRegistry registry() {
        return new DeliveryHandlerRegistry(List.of(new DeliveryHandler() {
            @Override
            public String channel() {
                return "test";
            }

            @Override
            public void validateDestination(String destination) {
            }

            @Override
            public DeliveryResult deliver(DeliveryContext context) {
                return results.getOrDefault(context.messageId(), DeliveryResult.success());
            }
        }));
    }

    private OutboxDispatcher dispatcher(
            DeliveryHandlerRegistry registry,
            TransactionalOutboxProperties properties
    ) {
        return new OutboxDispatcher(
                store,
                registry,
                new DefaultDeliveryFailureClassifier(),
                attempt -> Duration.ofSeconds(1),
                new OutboxDeadLetterNotifier(List.of()),
                new NoopOutboxMetrics(),
                new OutboxWorkerIdentity("node-a"),
                Runnable::run,
                properties,
                Clock.systemUTC()
        );
    }

    private TransactionalOutboxProperties properties() {
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        properties.getPolling().setBatchSize(10);
        properties.getPolling().setConcurrency(1);
        properties.getDelivery().setQueueCapacity(10);
        return properties;
    }

    private OutboxMessage message(String messageId, String idempotencyKey) {
        return OutboxMessage.builder()
                .messageId(messageId)
                .idempotencyKey(idempotencyKey)
                .channel("test")
                .destination("orders")
                .payload(Map.of("id", messageId))
                .build();
    }

    private OutboxStatus status(String messageId) {
        return OutboxStatus.valueOf(jdbcTemplate.queryForObject("""
                select status
                from egon_cola_outbox_message
                where message_id = ?
                """, String.class, messageId));
    }
}
