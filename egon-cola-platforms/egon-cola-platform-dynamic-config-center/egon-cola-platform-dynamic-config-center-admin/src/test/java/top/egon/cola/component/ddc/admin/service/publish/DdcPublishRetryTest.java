package top.egon.cola.component.ddc.admin.service.publish;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigVersionEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishAckEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.enums.PublishMode;
import top.egon.cola.component.ddc.admin.model.enums.PublishStatus;
import top.egon.cola.component.ddc.admin.model.vo.DdcAtomicPublishCommand;
import top.egon.cola.component.ddc.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.ddc.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;
import top.egon.cola.component.ddc.admin.service.lease.DdcConfigLeaseService;
import top.egon.cola.component.ddc.format.DdcChecksum;
import top.egon.cola.component.ddc.model.config.DdcAckRequest;
import top.egon.cola.component.ddc.model.config.DdcPublishMessage;
import top.egon.cola.component.ddc.model.config.DdcPublishTarget;
import top.egon.cola.component.ddc.model.config.DdcAckStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({
        DdcPublishService.class,
        DdcPendingPublishDispatcher.class,
        DdcPublishStateTransitionService.class,
        PublishFailureRecorder.class,
        PublishResourceLockRegistry.class,
        PublishCompletionWaiterRegistry.class,
        DdcPublishRetryTest.Dependencies.class
})
@EnableConfigurationProperties(DdcAdminProperties.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:ddc_publish_retry_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.flyway.enabled=false",
        "egon.cola.component.ddc.admin.publish.default-timeout-ms=2000",
        "egon.cola.component.ddc.admin.publish.max-timeout-ms=5000"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DdcPublishRetryTest {

    private static final String CONFIG_VALUE =
            "feature:\n  enabled: true\n";

    private static final String CHECKSUM = DdcChecksum.resource(
            "application.yml",
            "YAML",
            CONFIG_VALUE
    );

    @Autowired
    private DdcPublishService publishService;

    @Autowired
    private DdcPublishTaskRepository taskRepository;

    @Autowired
    private DdcConfigItemRepository configItemRepository;

    @Autowired
    private DdcPublishAckRepository ackRepository;

    @Autowired
    private DdcConfigVersionRepository versionRepository;

    @Autowired
    private DdcConfigLeaseService leaseService;

    @Autowired
    private DdcRedisRepository redisRepository;

    @BeforeEach
    void setUp() {
        reset(leaseService, redisRepository);
    }

    @Test
    void retriesFailedTimeoutAndUnknownUsingOnlyOriginalTargets() throws Exception {
        LinkedBlockingQueue<DdcPublishMessage> published = new LinkedBlockingQueue<>();
        doAnswer(invocation -> {
            DdcAtomicPublishCommand command = invocation.getArgument(0);
            published.add(command.message());
            return null;
        }).when(redisRepository).dispatch(any());
        when(leaseService.areActiveTargets(
                eq("default"),
                eq("dev"),
                any(),
                anyList()
        )).thenReturn(true);

        for (PublishStatus status : List.of(
                PublishStatus.FAILED,
                PublishStatus.TIMEOUT,
                PublishStatus.UNKNOWN
        )) {
            DdcPublishTaskEntity task =
                    saveRetryable(status, "retry-" + status.name().toLowerCase());
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<DdcPublishResultVO> result =
                        executor.submit(() -> publishService.retry(task.getChangeId()));
                DdcPublishMessage message = published.poll(2, TimeUnit.SECONDS);
                assertThat(message).isNotNull();
                assertThat(message.getTargets()).containsExactly(
                        new DdcPublishTarget("instance-1", "lease-1")
                );

                publishService.ack(successAck(task));

                assertThat(result.get(2, TimeUnit.SECONDS))
                        .satisfies(retried -> {
                            assertThat(retried.getStatus()).isEqualTo("SUCCESS");
                            assertThat(retried.getAttemptCount()).isEqualTo(1);
                        });
            }
        }

        verify(leaseService, never()).activeTargets(any(), any(), any());
    }

    @Test
    void expiredFixedTargetPersistsFailedReasonAndDoesNotDispatch() {
        DdcPublishTaskEntity task =
                saveRetryable(PublishStatus.TIMEOUT, "retry-expired");
        when(leaseService.areActiveTargets(
                eq("default"),
                eq("dev"),
                any(),
                anyList()
        )).thenReturn(false);

        assertThatThrownBy(() -> publishService.retry(task.getChangeId()))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("publish target lease expired");

        assertThat(taskRepository.findByChangeId(task.getChangeId()))
                .get()
                .satisfies(updated -> {
                    assertThat(updated.getStatus()).isEqualTo(PublishStatus.FAILED.name());
                    assertThat(updated.getErrorMessage())
                            .isEqualTo("DDC_TARGET_LEASE_EXPIRED");
                    assertThat(updated.getAttemptCount()).isZero();
                });
        verify(redisRepository, never()).dispatch(any());
    }

    @Test
    void rejectsSuccessAndActiveTasks() {
        for (PublishStatus status : List.of(
                PublishStatus.SUCCESS,
                PublishStatus.PENDING,
                PublishStatus.PUBLISHING
        )) {
            DdcPublishTaskEntity task =
                    saveRetryable(status, "retry-reject-" + status.name().toLowerCase());

            assertThatThrownBy(() -> publishService.retry(task.getChangeId()))
                    .isInstanceOf(DdcAdminException.class);
        }
    }

    private DdcPublishTaskEntity saveRetryable(PublishStatus status, String label) {
        LocalDateTime now = LocalDateTime.now();
        String configId = UuidV7.simpleString();
        DdcConfigItemEntity config = new DdcConfigItemEntity();
        config.setId(configId);
        config.setBizCode("default");
        config.setAppCode(label);
        config.setEnv("dev");
        config.setResourceName("application.yml");
        config.setContent(CONFIG_VALUE);
        config.setDefaultValue(null);
        config.setFormat("YAML");
        config.setCurrentVersion(2L);
        config.setPublishedVersion(1L);
        config.setEnabled(true);
        config.setDeleted(false);
        config.setCreatedAt(now.minusSeconds(10));
        config.setUpdatedAt(now);
        configItemRepository.saveAndFlush(config);

        DdcPublishTaskEntity task = new DdcPublishTaskEntity();
        task.setId(UuidV7.simpleString());
        task.setChangeId(UuidV7.simpleString());
        task.setConfigId(configId);
        task.setBizCode(config.getBizCode());
        task.setAppCode(label);
        task.setEnv("dev");
        task.setNamespace("default");
        task.setResourceName("application.yml");
        task.setTargetVersion(2L);
        task.setResourceChecksum(CHECKSUM);
        task.setPublishMode(PublishMode.SYNC_ALL_ACK.name());
        task.setStatus(status.name());
        task.setTargetCount(1);
        task.setAckCount(status == PublishStatus.SUCCESS ? 1 : 0);
        task.setFailedCount(0);
        task.setIgnoredCount(0);
        task.setTimeoutCount(status == PublishStatus.TIMEOUT ? 1 : 0);
        task.setAttemptCount(0);
        task.setTimeoutMs(2000L);
        task.setCreatedAt(now.minusSeconds(10));
        task.setDispatchedAt(now.minusSeconds(5));
        task.setCompletedAt(status == PublishStatus.PENDING
                || status == PublishStatus.PUBLISHING ? null : now);
        task.setUpdatedAt(now);
        taskRepository.saveAndFlush(task);

        DdcPublishAckEntity target = new DdcPublishAckEntity();
        target.setId(UuidV7.simpleString());
        target.setChangeId(task.getChangeId());
        target.setInstanceId("instance-1");
        target.setLeaseId("lease-1");
        target.setBizCode(task.getBizCode());
        target.setAppCode(task.getAppCode());
        target.setEnv(task.getEnv());
        target.setNamespace(task.getNamespace());
        target.setResourceName(task.getResourceName());
        target.setTargetVersion(task.getTargetVersion());
        target.setResourceChecksum(task.getResourceChecksum());
        if (status == PublishStatus.SUCCESS) {
            target.setAckStatus(DdcAckStatus.SUCCESS.name());
            target.setAckAt(now);
        } else if (status == PublishStatus.TIMEOUT) {
            target.setAckStatus(DdcAckStatus.TIMEOUT.name());
            target.setAckAt(now);
        }
        ackRepository.saveAndFlush(target);

        DdcConfigVersionEntity version = new DdcConfigVersionEntity();
        version.setId(UuidV7.simpleString());
        version.setConfigId(configId);
        version.setBizCode(task.getBizCode());
        version.setAppCode(task.getAppCode());
        version.setEnv(task.getEnv());
        version.setNamespace(task.getNamespace());
        version.setResourceName(task.getResourceName());
        version.setVersion(task.getTargetVersion());
        version.setOldContent("feature:\n  enabled: false\n");
        version.setNewContent(CONFIG_VALUE);
        version.setFormat("YAML");
        version.setCreatedAt(now);
        versionRepository.saveAndFlush(version);
        return task;
    }

    private DdcAckRequest successAck(DdcPublishTaskEntity task) {
        DdcAckRequest request = new DdcAckRequest();
        request.setChangeId(task.getChangeId());
        request.setInstanceId("instance-1");
        request.setLeaseId("lease-1");
        request.setTargetVersion(task.getTargetVersion());
        request.setCurrentVersion(task.getTargetVersion());
        request.setResourceChecksum(task.getResourceChecksum());
        request.setStatus(DdcAckStatus.SUCCESS);
        return request;
    }

    @TestConfiguration
    static class Dependencies {

        @Bean
        DdcRedisRepository ddcRedisRepository() {
            return org.mockito.Mockito.mock(DdcRedisRepository.class);
        }

        @Bean
        DdcConfigLeaseService ddcConfigLeaseService() {
            return org.mockito.Mockito.mock(DdcConfigLeaseService.class);
        }
    }
}
