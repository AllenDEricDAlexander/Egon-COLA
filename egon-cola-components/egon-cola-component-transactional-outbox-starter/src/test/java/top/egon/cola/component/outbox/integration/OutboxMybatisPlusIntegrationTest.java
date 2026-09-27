package top.egon.cola.component.outbox.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.aop.support.AopUtils;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.common.exception.OutboxIdempotencyConflictException;
import top.egon.cola.component.outbox.store.MybatisPlusOutboxStore;
import top.egon.cola.component.outbox.store.NewOutboxRecord;
import top.egon.cola.component.outbox.store.OutboxRecord;
import top.egon.cola.component.outbox.store.OutboxStatus;
import top.egon.cola.component.outbox.store.OutboxStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxMybatisPlusIntegrationTest extends PostgresqlOutboxTestSupport {

    private OutboxStore store;

    @BeforeEach
    void setUpStore() {
        store = outboxStore();
    }

    @Test
    void shouldPersistAndTransitionThroughManagedMpStoreWhileRestoringTenantContext() {
        assertThat(context).isNotNull();
        assertThat(context.containsBean("outboxSchemaValidator")).isTrue();
        assertThat(AopUtils.getTargetClass(store)).isEqualTo(MybatisPlusOutboxStore.class);
        MDC.put("tenantId", "42");
        MDC.put("userId", "business-user");
        try {
            OutboxReceipt receipt = store.enqueue(
                    newMpRecord("message-mp-1", "key-message-mp-1", "a".repeat(64), 2));

            assertThat(receipt).isEqualTo(new OutboxReceipt("message-mp-1", "key-message-mp-1", true));
            assertThat(store.enqueue(newMpRecord("message-mp-duplicate", "key-message-mp-1", "a".repeat(64), 3)))
                    .isEqualTo(new OutboxReceipt("message-mp-1", "key-message-mp-1", false));
            assertThatThrownBy(() -> store.enqueue(
                    newMpRecord("message-mp-conflict", "key-message-mp-1", "b".repeat(64), 3)))
                    .isInstanceOf(OutboxIdempotencyConflictException.class);
            assertThat(MDC.get("tenantId")).isEqualTo("42");
            assertThat(MDC.get("userId")).isEqualTo("business-user");
            Map<String, Object> inserted = jdbcTemplate.queryForMap("""
                    select tenant_id, version, deleted_at, status, headers_json
                    from egon_outbox.egon_cola_outbox_message
                    where message_id = ?
                    """, "message-mp-1");
            assertThat(((Number) inserted.get("tenant_id")).longValue()).isZero();
            assertThat(((Number) inserted.get("version")).longValue()).isZero();
            assertThat(inserted.get("deleted_at")).isNull();
            assertThat(inserted.get("status")).isEqualTo(OutboxStatus.PENDING.getMessage());
            assertThat(inserted.get("headers_json")).isEqualTo("{\"x-origin\":\"integration\"}");

            OutboxRecord claimed = store.claimDue(1, "worker-mp:lease-1", Duration.ofSeconds(60)).getFirst();
            assertThat(claimed.status()).isEqualTo(OutboxStatus.PROCESSING);
            assertThat(store.markRetry(claimed.id(), "worker-mp:lease-1", Duration.ZERO,
                    "TEMPORARY", "first attempt")).isTrue();
            jdbcTemplate.update("""
                    update egon_outbox.egon_cola_outbox_message
                    set next_attempt_at = clock_timestamp() - interval '1 second'
                    where message_id = 'message-mp-1'
                    """);
            OutboxRecord retried = store.claimDue(1, "worker-mp:lease-2", Duration.ofSeconds(60)).getFirst();
            assertThat(retried.attemptCount()).isEqualTo(2);
            assertThat(store.markDead(retried.id(), "worker-mp:lease-2",
                    "OUTBOX_RETRY_EXHAUSTED", "retry budget exhausted")).isTrue();
            assertThat(jdbcTemplate.queryForObject("""
                    select status from egon_outbox.egon_cola_outbox_message where message_id = 'message-mp-1'
                    """, String.class)).isEqualTo(OutboxStatus.DEAD.getMessage());

            store.enqueue(newMpRecord("message-mp-success"));
            OutboxRecord successful = store.claimDue(1, "worker-mp:lease-3", Duration.ofSeconds(60)).getFirst();
            assertThat(successful.messageId()).isEqualTo("message-mp-success");
            assertThat(store.markSucceeded(successful.id(), "worker-mp:lease-3")).isTrue();
            assertThat(MDC.get("tenantId")).isEqualTo("42");
            assertThat(MDC.get("userId")).isEqualTo("business-user");
        } finally {
            MDC.remove("tenantId");
            MDC.remove("userId");
        }
    }

    private NewOutboxRecord newMpRecord(String messageId) {
        return newMpRecord(messageId, "key-" + messageId, "a".repeat(64), 3);
    }

    private NewOutboxRecord newMpRecord(String messageId, String idempotencyKey, String fingerprint, int maxAttempts) {
        return new NewOutboxRecord(
                messageId,
                idempotencyKey,
                fingerprint,
                "test",
                "orders",
                "{}",
                "application/json",
                "1",
                "{\"x-origin\":\"integration\"}",
                "trace-mp-1",
                Instant.parse("2026-09-24T14:00:00Z"),
                maxAttempts
        );
    }
}
