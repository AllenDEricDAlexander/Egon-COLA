package top.egon.cola.component.tianshu.ai.feedback;

import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.tianshu.ai.observability.DdcAiStarterMetrics;
import top.egon.cola.component.tianshu.ai.properties.DdcAiFeedbackProperties;
import top.egon.cola.component.tianshu.api.client.DdcAiFeedbackClient;
import top.egon.cola.component.tianshu.error.DdcClientTransportException;
import top.egon.cola.component.tianshu.model.ai.DdcAiOutcomeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportBatchDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportReceiptDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportStatusEnum;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultDdcAiUsageReporterTest {

    private final RecordingMetrics metrics = new RecordingMetrics();
    private final List<Duration> sleeps = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @AfterEach
    void stop() {
        scheduler.shutdownNow();
    }

    @Test
    void reportEnqueuesAndFlushesInBatchesPreservingOrder() {
        FakeFeedbackClient client = new FakeFeedbackClient();
        DefaultDdcAiUsageReporter reporter = reporter(client, properties(10, 2, 3));

        for (int index = 1; index <= 5; index++) assertThat(reporter.report(sample("usage-" + index))).isTrue();

        assertThat(reporter.flush()).isEqualTo(3);
        assertThat(client.batches).extracting(batch -> batch.getReports().size()).containsExactly(2, 2, 1);
        assertThat(client.batches.stream().flatMap(batch -> batch.getReports().stream())
                .map(DdcAiUsageReportDTO::getUsageId).toList())
                .containsExactly(id("usage-1"), id("usage-2"), id("usage-3"), id("usage-4"), id("usage-5"));
        assertThat(metrics.count("tianshu.ai.feedback.reported", "status", "ACCEPTED")).isEqualTo(5);
    }

    @Test
    void fullQueueClosedReporterAndInvalidReportsAreDroppedAndCounted() {
        DefaultDdcAiUsageReporter reporter = reporter(new FakeFeedbackClient(), properties(1, 10, 3));

        assertThat(reporter.report(sample("usage-1"))).isTrue();
        assertThat(reporter.report(sample("usage-2"))).isFalse();
        assertThat(reporter.report(sample("usage-3").toBuilder().usageId("bad").build())).isFalse();
        assertThat(reporter.report(null)).isFalse();
        reporter.close();
        assertThat(reporter.report(sample("usage-4"))).isFalse();

        assertThat(metrics.count("tianshu.ai.feedback.dropped", "reason", "QUEUE_FULL")).isEqualTo(1);
        assertThat(metrics.count("tianshu.ai.feedback.dropped", "reason", "INVALID")).isEqualTo(2);
        assertThat(metrics.count("tianshu.ai.feedback.dropped", "reason", "CLOSED")).isEqualTo(1);
    }

    @Test
    void transportFailureRetriesWithBackoffUpToMaxAttempts() {
        FakeFeedbackClient client = new FakeFeedbackClient();
        client.failures.set(5);
        DefaultDdcAiUsageReporter reporter = reporter(client, properties(10, 10, 3));
        reporter.report(sample("usage-1"));
        reporter.report(sample("usage-2"));

        reporter.flush();

        assertThat(client.attempts.get()).isEqualTo(3);
        assertThat(sleeps).containsExactly(Duration.ofSeconds(1), Duration.ofSeconds(2));
        assertThat(metrics.count("tianshu.ai.feedback.dropped", "reason", "TRANSPORT")).isEqualTo(2);
    }

    @Test
    void transportRecoveryAfterOneFailureDeliversTheBatch() {
        FakeFeedbackClient client = new FakeFeedbackClient();
        client.failures.set(1);
        DefaultDdcAiUsageReporter reporter = reporter(client, properties(10, 10, 3));
        reporter.report(sample("usage-1"));

        reporter.flush();

        assertThat(client.batches).hasSize(1);
        assertThat(sleeps).containsExactly(Duration.ofSeconds(1));
        assertThat(metrics.count("tianshu.ai.feedback.dropped", "reason", "TRANSPORT")).isZero();
    }

    @Test
    void rejectedItemsAreCountedAndNotRetried() {
        FakeFeedbackClient client = new FakeFeedbackClient();
        client.status = DdcAiUsageReportStatusEnum.REJECTED;
        DefaultDdcAiUsageReporter reporter = reporter(client, properties(10, 10, 3));
        reporter.report(sample("usage-1"));

        reporter.flush();
        reporter.flush();

        assertThat(client.attempts.get()).isEqualTo(1);
        assertThat(metrics.count("tianshu.ai.feedback.reported", "status", "REJECTED")).isEqualTo(1);
    }

    @Test
    void closeFlushesRemaining() {
        FakeFeedbackClient client = new FakeFeedbackClient();
        DefaultDdcAiUsageReporter reporter = reporter(client, properties(10, 10, 3));
        reporter.start();
        reporter.report(sample("usage-1"));

        reporter.close();

        assertThat(client.batches).hasSize(1);
    }

    private DefaultDdcAiUsageReporter reporter(FakeFeedbackClient client, DdcAiFeedbackProperties properties) {
        return new DefaultDdcAiUsageReporter(client, properties, metrics,
                Validation.buildDefaultValidatorFactory().getValidator(), scheduler, sleeps::add);
    }

    private static DdcAiFeedbackProperties properties(int capacity, int batchSize, int maxAttempts) {
        return DdcAiFeedbackProperties.builder().enabled(true).queueCapacity(capacity).batchSize(batchSize)
                .flushInterval(Duration.ofHours(1)).maxAttempts(maxAttempts).build();
    }

    private static String id(String suffix) {
        return ("01JAX0M7Q9K2R5T8" + suffix).replace("-", "_");
    }

    private static DdcAiUsageReportDTO sample(String suffix) {
        return DdcAiUsageReportDTO.builder().usageId(id(suffix)).namespaceCode("default").resourceKey("refund")
                .resourceId(1L).versionId(2L).versionNo(3L).outcome(DdcAiOutcomeEnum.SUCCESS)
                .occurredAt(Instant.parse("2026-10-17T05:00:00Z")).build();
    }

    private static final class FakeFeedbackClient implements DdcAiFeedbackClient {
        private final List<DdcAiUsageReportBatchDTO> batches = new ArrayList<>();
        private final AtomicInteger attempts = new AtomicInteger();
        private final AtomicInteger failures = new AtomicInteger();
        private DdcAiUsageReportStatusEnum status = DdcAiUsageReportStatusEnum.ACCEPTED;

        @Override
        public DdcAiUsageReportReceiptDTO report(DdcAiUsageReportBatchDTO batch) {
            attempts.incrementAndGet();
            if (failures.getAndDecrement() > 0) throw new DdcClientTransportException("UNAVAILABLE", true);
            batches.add(batch);
            return DdcAiUsageReportReceiptDTO.builder().items(batch.getReports().stream()
                    .map(report -> DdcAiUsageReportReceiptDTO.ItemDTO.builder().usageId(report.getUsageId())
                            .status(status).build()).toList()).build();
        }
    }

    static final class RecordingMetrics implements DdcAiStarterMetrics {
        private final Map<String, Long> counts = new ConcurrentHashMap<>();

        @Override
        public void increment(String name, long amount, String... tags) {
            counts.merge(name + String.join("|", tags), amount, Long::sum);
        }

        long count(String name, String... tags) {
            return counts.getOrDefault(name + String.join("|", tags), 0L);
        }
    }
}
