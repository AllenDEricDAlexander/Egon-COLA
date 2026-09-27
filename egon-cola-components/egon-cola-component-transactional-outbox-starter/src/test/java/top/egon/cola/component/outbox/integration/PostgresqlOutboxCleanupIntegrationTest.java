package top.egon.cola.component.outbox.integration;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.common.exception.OutboxIdempotencyConflictException;
import top.egon.cola.component.outbox.persistence.repository.OutboxMessageRepository;
import top.egon.cola.component.outbox.store.OutboxStore;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostgresqlOutboxCleanupIntegrationTest extends PostgresqlOutboxTestSupport {

    @Test
    void shouldSkipAConcurrentCleanupWhileTheTransactionLockIsHeld() throws Exception {
        OutboxStore store = outboxStore();
        store.enqueue(newRecord("cleanup-lock-holder"));
        jdbcTemplate.update("""
                update egon_outbox.egon_cola_outbox_message
                set status = 'SUCCEEDED', completed_at = clock_timestamp() - interval '10 days'
                where message_id = 'cleanup-lock-holder'
                """);
        long id = jdbcTemplate.queryForObject("""
                select id from egon_outbox.egon_cola_outbox_message
                where message_id = 'cleanup-lock-holder'
                """, Long.class);
        OutboxMessageRepository repository = context.getBean(OutboxMessageRepository.class);
        CountDownLatch lockAttempted = new CountDownLatch(1);
        CountDownLatch releaseTransaction = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<Boolean> lockTransaction = executor.submit(() ->
                    new TransactionTemplate(transactionManager).execute(status -> {
                        try {
                            boolean acquired = repository.tryAcquireTransactionLock(id);
                            if (!acquired) {
                                throw new IllegalStateException("OUTBOX_TEST_ADVISORY_LOCK_NOT_ACQUIRED");
                            }
                            lockAttempted.countDown();
                            await(releaseTransaction);
                            return true;
                        } catch (RuntimeException | Error failure) {
                            lockAttempted.countDown();
                            throw failure;
                        }
                    }));

            assertThat(lockAttempted.await(5, TimeUnit.SECONDS)).isTrue();
            if (lockTransaction.isDone()) {
                lockTransaction.get(5, TimeUnit.SECONDS);
            }
            try {
                assertThat(store.deleteSucceeded(Duration.ofDays(7), 10)).isZero();
            } finally {
                releaseTransaction.countDown();
            }
            assertThat(lockTransaction.get(5, TimeUnit.SECONDS)).isTrue();
            assertThat(store.deleteSucceeded(Duration.ofDays(7), 10)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("""
                    select count(*) from egon_outbox.egon_cola_outbox_message where id = ?
                    """, Integer.class, id)).isZero();
        }
    }

    @Test
    void shouldDeleteOnlyOldSuccessAndEndOnlyItsDeduplicationWindow() {
        OutboxStore store = outboxStore();
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
                update egon_outbox.egon_cola_outbox_message
                set status = 'SUCCEEDED', completed_at = clock_timestamp() - interval '10 days'
                where message_id = 'old-success'
                """);
        jdbcTemplate.update("""
                update egon_outbox.egon_cola_outbox_message
                set status = 'SUCCEEDED', completed_at = clock_timestamp()
                where message_id = 'recent-success'
                """);
        jdbcTemplate.update("""
                update egon_outbox.egon_cola_outbox_message
                set status = 'PROCESSING', locked_by = 'owner',
                    locked_until = clock_timestamp() + interval '1 minute'
                where message_id = 'processing'
                """);
        jdbcTemplate.update("""
                update egon_outbox.egon_cola_outbox_message
                set status = 'RETRY_WAIT'
                where message_id = 'retry'
                """);
        jdbcTemplate.update("""
                update egon_outbox.egon_cola_outbox_message
                set status = 'DEAD', completed_at = clock_timestamp() - interval '10 days'
                where message_id = 'dead'
                """);

        int deleted = store.deleteSucceeded(Duration.ofDays(7), 500);

        assertThat(deleted).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("""
                select message_id
                from egon_outbox.egon_cola_outbox_message
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

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("OUTBOX_TEST_ADVISORY_LOCK_RELEASE_TIMEOUT");
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OUTBOX_TEST_ADVISORY_LOCK_INTERRUPTED", failure);
        }
    }
}
