package top.egon.cola.component.outbox.test;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.outbox.store.OutboxRecord;
import top.egon.cola.component.outbox.store.PostgresqlJdbcOutboxStore;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresqlOutboxConcurrencyIntegrationTest extends PostgresqlOutboxTestSupport {

    @Test
    void shouldClaimDisjointBatchesAndReleaseDatabaseLocksBeforeDelivery() throws Exception {
        PostgresqlJdbcOutboxStore store = outboxStore();
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
                                from egon_cola_outbox_message
                                where message_id = ?
                                for update nowait
                            ) locked_row
                            """, Integer.class, messageId));
            assertThat(locked).isEqualTo(1);
        }
    }
}
