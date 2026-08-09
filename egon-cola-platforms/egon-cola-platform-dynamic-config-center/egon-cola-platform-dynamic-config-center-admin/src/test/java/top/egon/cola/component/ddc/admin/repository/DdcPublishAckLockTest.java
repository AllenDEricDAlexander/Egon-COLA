package top.egon.cola.component.ddc.admin.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishAckEntity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class DdcPublishAckLockTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private DdcPublishAckRepository repository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Test
    void conflictingAckTransactionsSerializeOnTheTargetRow() throws Exception {
        String changeId = "change-lock";
        transaction().executeWithoutResult(status ->
                repository.saveAndFlush(target(changeId)));

        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> transaction().execute(status -> {
                DdcPublishAckEntity target = repository
                        .findForUpdateByChangeIdAndInstanceIdAndLeaseId(
                                changeId, "instance-1", "lease-1")
                        .orElseThrow();
                target.setAckStatus("FAILED");
                repository.saveAndFlush(target);
                firstLocked.countDown();
                await(releaseFirst);
                return target.getAckStatus();
            }));

            assertThat(firstLocked.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> transaction().execute(status ->
                    repository.findForUpdateByChangeIdAndInstanceIdAndLeaseId(
                                    changeId, "instance-1", "lease-1")
                            .orElseThrow()
                            .getAckStatus()));

            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            releaseFirst.countDown();

            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo("FAILED");
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo("FAILED");
        } finally {
            releaseFirst.countDown();
        }
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactionManager);
    }

    private DdcPublishAckEntity target(String changeId) {
        DdcPublishAckEntity target = new DdcPublishAckEntity();
        target.setId("ack-lock");
        target.setChangeId(changeId);
        target.setInstanceId("instance-1");
        target.setLeaseId("lease-1");
        return target;
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for ACK lock test");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ACK lock test interrupted", exception);
        }
    }
}
