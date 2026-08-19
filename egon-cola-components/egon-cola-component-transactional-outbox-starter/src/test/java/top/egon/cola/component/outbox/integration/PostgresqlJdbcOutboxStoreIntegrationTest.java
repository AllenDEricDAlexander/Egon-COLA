package top.egon.cola.component.outbox.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.exception.OutboxIdempotencyConflictException;
import top.egon.cola.component.outbox.store.NewOutboxRecord;
import top.egon.cola.component.outbox.store.OutboxRecord;
import top.egon.cola.component.outbox.store.PostgresqlJdbcOutboxStore;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresqlJdbcOutboxStoreIntegrationTest extends PostgresqlOutboxTestSupport {

    private PostgresqlJdbcOutboxStore store;

    @BeforeEach
    void setUpStore() {
        store = new PostgresqlJdbcOutboxStore(
                jdbcTemplate,
                new NamedParameterJdbcTemplate(dataSource),
                objectMapper,
                transactionManager
        );
    }

    @Test
    void shouldInsertAndReturnExistingRecordForSameFingerprint() {
        OutboxReceipt created = store.enqueue(newRecord("message-1", "order:created:1", "fingerprint-a"));
        OutboxReceipt duplicate = store.enqueue(newRecord("message-2", "order:created:1", "fingerprint-a"));

        assertThat(created).isEqualTo(new OutboxReceipt("message-1", "order:created:1", true));
        assertThat(duplicate).isEqualTo(new OutboxReceipt("message-1", "order:created:1", false));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from egon_cola_outbox_message", Long.class)).isEqualTo(1L);
    }

    @Test
    void shouldRejectIdentifierReuseWithDifferentFingerprint() {
        store.enqueue(newRecord("message-1", "order:created:1", "fingerprint-a"));

        assertThatThrownBy(() ->
                store.enqueue(newRecord("message-1", "order:created:1", "fingerprint-b")))
                .isInstanceOf(OutboxIdempotencyConflictException.class);
    }

    @Test
    void shouldClaimWithUniqueOwnerAndRejectStaleCompletion() {
        store.enqueue(newRecord("message-1", null, "fingerprint-a"));

        OutboxRecord first = store.claimDue(1, "node-a:claim-1", Duration.ofSeconds(60)).getFirst();
        jdbcTemplate.update("""
                update egon_cola_outbox_message
                set locked_until = clock_timestamp() - interval '1 second'
                where id = ?
                """, first.id());
        OutboxRecord reclaimed =
                store.claimDue(1, "node-a:claim-2", Duration.ofSeconds(60)).getFirst();

        assertThat(store.markSucceeded(first.id(), "node-a:claim-1")).isFalse();
        assertThat(store.markSucceeded(reclaimed.id(), "node-a:claim-2")).isTrue();
    }

    private NewOutboxRecord newRecord(String messageId, String idempotencyKey, String fingerprint) {
        return new NewOutboxRecord(
                messageId,
                idempotencyKey,
                fingerprint,
                "http",
                "order-callback",
                "{}",
                "application/json",
                null,
                "{}",
                null,
                null,
                10
        );
    }
}
