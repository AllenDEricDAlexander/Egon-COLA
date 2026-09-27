package top.egon.cola.component.outbox.integration;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.outbox.persistence.repository.OutboxMessageRepository;
import top.egon.cola.component.outbox.store.OutboxRecord;
import top.egon.cola.component.outbox.store.OutboxStore;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresqlOutboxConcurrencyIntegrationTest extends PostgresqlOutboxTestSupport {

    @Test
    void shouldHoldTransactionAdvisoryLockUntilTheWorkerCommits() throws Exception {
        OutboxStore store = outboxStore();
        store.enqueue(newRecord("lock-holder"));
        long id = jdbcTemplate.queryForObject(
                "select id from egon_outbox.egon_cola_outbox_message where message_id = 'lock-holder'",
                Long.class);
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
                assertThat(store.claimDue(1, "worker-contender", Duration.ofSeconds(60))).isEmpty();
            } finally {
                releaseTransaction.countDown();
            }
            assertThat(lockTransaction.get(5, TimeUnit.SECONDS)).isTrue();

            List<OutboxRecord> claimed = store.claimDue(1, "worker-contender", Duration.ofSeconds(60));
            assertThat(claimed).singleElement()
                    .extracting(OutboxRecord::messageId)
                    .isEqualTo("lock-holder");
            assertThat(jdbcTemplate.queryForMap("""
                    select attempt_count, version, locked_by
                    from egon_outbox.egon_cola_outbox_message
                    where id = ?
                    """, id))
                    .containsEntry("attempt_count", 1)
                    .containsEntry("version", 1L)
                    .containsEntry("locked_by", "worker-contender");
        }
    }

    @Test
    void shouldClaimDisjointBatchesAndReleaseDatabaseLocksBeforeDelivery() throws Exception {
        OutboxStore store = outboxStore();
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                IntStream.range(0, 200).forEach(index ->
                        store.enqueue(newRecord("message-" + index))));
        CyclicBarrier barrier = new CyclicBarrier(2);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<List<OutboxRecord>> first = executor.submit(() -> {
                barrier.await();
                return store.claimDue(100, "node-a:claim-1", Duration.ofSeconds(60));
            });
            Future<List<OutboxRecord>> second = executor.submit(() -> {
                barrier.await();
                return store.claimDue(100, "node-b:claim-1", Duration.ofSeconds(60));
            });

            List<Long> firstIds = first.get(5, TimeUnit.SECONDS).stream()
                    .map(OutboxRecord::id)
                    .toList();
            List<Long> secondIds = second.get(5, TimeUnit.SECONDS).stream()
                    .map(OutboxRecord::id)
                    .toList();
            assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
            assertThat(Stream.concat(firstIds.stream(), secondIds.stream()).distinct())
                    .hasSize(200);

            String messageId = store.claimByMessageIds(
                            List.of("message-0"),
                            1,
                            "node-c:claim-1",
                            Duration.ofSeconds(60)
                    ).stream()
                    .findFirst()
                    .map(OutboxRecord::messageId)
                    .orElse("message-0");
            Integer locked = new TransactionTemplate(transactionManager).execute(status ->
                    jdbcTemplate.queryForObject("""
                            select count(*)
                            from (
                                select id
                                from egon_outbox.egon_cola_outbox_message
                                where message_id = ?
                                for update nowait
                            ) locked_row
                            """, Integer.class, messageId));
            assertThat(locked).isEqualTo(1);
        }
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
