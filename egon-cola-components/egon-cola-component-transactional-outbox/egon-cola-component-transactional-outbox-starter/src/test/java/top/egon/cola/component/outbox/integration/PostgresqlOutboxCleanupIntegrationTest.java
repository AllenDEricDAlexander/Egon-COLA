package top.egon.cola.component.outbox.integration;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.exception.OutboxIdempotencyConflictException;
import top.egon.cola.component.outbox.store.PostgresqlJdbcOutboxStore;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresqlOutboxCleanupIntegrationTest extends PostgresqlOutboxTestSupport {

    @Test
    void shouldDeleteOnlyOldSuccessAndEndOnlyItsDeduplicationWindow() {
        PostgresqlJdbcOutboxStore store = outboxStore();
        for (String messageId : new String[]{
                "old-success",
                "recent-success",
                "pending",
                "processing",
                "retry",
                "dead"
        }) {
            store.enqueue(newRecord(messageId));
        }
        jdbcTemplate.update("""
                update egon_cola_outbox_message
                set status = 'SUCCEEDED', completed_at = clock_timestamp() - interval '10 days'
                where message_id = 'old-success'
                """);
        jdbcTemplate.update("""
                update egon_cola_outbox_message
                set status = 'SUCCEEDED', completed_at = clock_timestamp()
                where message_id = 'recent-success'
                """);
        jdbcTemplate.update("""
                update egon_cola_outbox_message
                set status = 'PROCESSING', locked_by = 'owner',
                    locked_until = clock_timestamp() + interval '1 minute'
                where message_id = 'processing'
                """);
        jdbcTemplate.update("""
                update egon_cola_outbox_message
                set status = 'RETRY_WAIT'
                where message_id = 'retry'
                """);
        jdbcTemplate.update("""
                update egon_cola_outbox_message
                set status = 'DEAD', completed_at = clock_timestamp() - interval '10 days'
                where message_id = 'dead'
                """);

        int deleted = store.deleteSucceeded(Duration.ofDays(7), 500);

        assertThat(deleted).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("""
                select message_id
                from egon_cola_outbox_message
                order by message_id
                """, String.class))
                .containsExactly("dead", "pending", "processing", "recent-success", "retry");

        OutboxReceipt recreated = store.enqueue(newRecord(
                "old-success-recreated",
                "key-old-success",
                "b".repeat(64),
                10
        ));
        assertThat(recreated.created()).isTrue();
        assertThatThrownBy(() -> store.enqueue(newRecord(
                "recent-conflict",
                "key-recent-success",
                "b".repeat(64),
                10
        ))).isInstanceOf(OutboxIdempotencyConflictException.class);
    }
}
