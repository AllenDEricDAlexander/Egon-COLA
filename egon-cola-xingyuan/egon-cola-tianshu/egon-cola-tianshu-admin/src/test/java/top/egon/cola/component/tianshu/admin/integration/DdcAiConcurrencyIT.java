package top.egon.cola.component.tianshu.admin.integration;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DdcAiConcurrencyIT {

    @Test
    void separateConnectionsCannotCreateTheSameLiveResourceKeyTwice() throws Exception {
        long tenantId = DdcAiPersistenceIT.nextId();
        long namespaceId = DdcAiPersistenceIT.nextId();
        long firstResourceId = DdcAiPersistenceIT.nextId();
        long secondResourceId = DdcAiPersistenceIT.nextId();
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String bizCode = "it-biz-" + suffix;
        String namespaceCode = "it-space-" + suffix;
        String resourceKey = "it-" + suffix;
        try (Connection setup = DdcAiPersistenceIT.openPostgres()) {
            DdcAiPersistenceIT.insertNamespace(setup, tenantId, namespaceId, bizCode, namespaceCode);
            setup.commit();
        }

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<String> first = executor.submit(() -> insertAtBarrier(
                    ready, start, firstResourceId, tenantId, namespaceId, bizCode, resourceKey));
            Future<String> second = executor.submit(() -> insertAtBarrier(
                    ready, start, secondResourceId, tenantId, namespaceId, bizCode, resourceKey));

            boolean bothConnectionsReady = ready.await(30, TimeUnit.SECONDS);
            start.countDown();
            assertThat(bothConnectionsReady).isTrue();
            List<String> outcomes = List.of(first.get(), second.get());
            assertThat(outcomes).containsExactlyInAnyOrder("INSERTED", "UNIQUE_CONFLICT");
        } finally {
            try (Connection cleanup = DdcAiPersistenceIT.openPostgres()) {
                DdcAiPersistenceIT.softDeleteResource(cleanup, tenantId, firstResourceId);
                DdcAiPersistenceIT.softDeleteResource(cleanup, tenantId, secondResourceId);
                DdcAiPersistenceIT.softDeleteNamespace(cleanup, tenantId, namespaceId);
            }
        }
    }

    private static String insertAtBarrier(CountDownLatch ready, CountDownLatch start, long resourceId,
                                          long tenantId, long namespaceId, String bizCode, String resourceKey)
            throws Exception {
        try (Connection connection = DdcAiPersistenceIT.openPostgres()) {
            ready.countDown();
            start.await();
            DdcAiPersistenceIT.insertResource(connection, resourceId, tenantId, namespaceId,
                    bizCode, "prod", "it-app", "PROMPT", resourceKey, "draft");
            return "INSERTED";
        } catch (SQLException failure) {
            if ("23505".equals(failure.getSQLState())) return "UNIQUE_CONFLICT";
            throw failure;
        }
    }
}
