package top.egon.cola.component.ddc.admin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.admin.model.dto.DdcPublishRequest;
import top.egon.cola.component.ddc.admin.model.entity.DdcConfigItemEntity;
import top.egon.cola.component.ddc.admin.model.entity.DdcPublishTaskEntity;
import top.egon.cola.component.ddc.admin.model.vo.DdcConfigResourceKey;
import top.egon.cola.component.ddc.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.ddc.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.ddc.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.ddc.admin.repository.DdcRedisRepository;
import top.egon.cola.component.ddc.common.DdcChecksum;
import top.egon.cola.component.ddc.model.dto.DdcAckRequest;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.model.dto.DdcPublishTarget;
import top.egon.cola.component.ddc.model.enums.DdcAckStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({
        DdcPublishService.class,
        DdcPublishStateTransitionService.class,
        PublishFailureRecorder.class,
        PublishResourceLockRegistry.class,
        PublishCompletionWaiterRegistry.class,
        DdcPublishPreparationTest.Dependencies.class
})
@EnableConfigurationProperties(DdcAdminProperties.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:ddc_publish_prepare_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.flyway.enabled=false",
        "egon.cola.component.ddc.admin.max-value-bytes=5",
        "egon.cola.component.ddc.admin.publish.default-timeout-ms=2000",
        "egon.cola.component.ddc.admin.publish.max-timeout-ms=5000"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DdcPublishPreparationTest {

    @Autowired
    private DdcPublishService publishService;

    @Autowired
    private DdcConfigItemRepository configItemRepository;

    @Autowired
    private DdcPublishTaskRepository taskRepository;

    @Autowired
    private DdcPublishAckRepository ackRepository;

    @Autowired
    private DdcConfigLeaseService leaseService;

    @Autowired
    private DdcRedisRepository redisRepository;

    @Autowired
    private PublishResourceLockRegistry resourceRegistry;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        reset(leaseService, redisRepository);
    }

    @Test
    void fixesRedisLeaseTargetsAndCompletesOnlyAfterEverySuccessAck() throws Exception {
        saveConfig("switch-all");
        List<DdcPublishTarget> targets = List.of(
                new DdcPublishTarget("instance-2", "lease-2"),
                new DdcPublishTarget("instance-1", "lease-1")
        );
        when(leaseService.activeTargets("demo", "dev", "default")).thenReturn(targets);
        CountDownLatch published = new CountDownLatch(1);
        doAnswer(invocation -> {
            published.countDown();
            return null;
        }).when(redisRepository).publish(any());
        DdcPublishRequest request = request("switch-all", "true");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<DdcPublishResultVO> result =
                    executor.submit(() -> publishService.publish(request, "tester"));
            assertThat(published.await(2, TimeUnit.SECONDS)).isTrue();

            ArgumentCaptor<DdcPublishMessage> message =
                    ArgumentCaptor.forClass(DdcPublishMessage.class);
            verify(redisRepository).publish(message.capture());
            assertThat(message.getValue().getTargets())
                    .containsExactly(
                            new DdcPublishTarget("instance-1", "lease-1"),
                            new DdcPublishTarget("instance-2", "lease-2")
                    );
            assertThat(message.getValue().getContentChecksum())
                    .isEqualTo(DdcChecksum.content("true"));
            assertThat(ackRepository.findByChangeId(request.getChangeId()))
                    .extracting(target -> target.getInstanceId() + ":" + target.getLeaseId())
                    .containsExactlyInAnyOrder(
                            "instance-1:lease-1",
                            "instance-2:lease-2"
                    );

            publishService.ack(ack(request, "instance-1", "lease-1"));
            assertThat(result.isDone()).isFalse();
            publishService.ack(ack(request, "instance-2", "lease-2"));

            assertThat(result.get(2, TimeUnit.SECONDS).getStatus()).isEqualTo("SUCCESS");
        }
        assertThat(configItemRepository.findByAppCodeAndEnvAndNamespaceAndConfigKey(
                "demo", "dev", "default", "switch-all"
        )).get().extracting(DdcConfigItemEntity::getCurrentVersion).isEqualTo(2L);
    }

    @Test
    void noLiveRedisLeaseRollsBackConfigAndNeverReadsDatabaseOnlineState() {
        saveConfig("switch-no-live");
        when(leaseService.activeTargets("demo", "dev", "default")).thenReturn(List.of());
        DdcPublishRequest request = request("switch-no-live", "true");

        assertThatThrownBy(() -> publishService.publish(request, "tester"))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("no live config instance");

        assertThat(configItemRepository.findByAppCodeAndEnvAndNamespaceAndConfigKey(
                "demo", "dev", "default", "switch-no-live"
        )).get().satisfies(config -> {
            assertThat(config.getCurrentVersion()).isEqualTo(1L);
            assertThat(config.getConfigValue()).isEqualTo("false");
        });
    }

    @Test
    void rejectsAnotherChangeForTheSameResourceWithoutBlockingAck() throws Exception {
        saveConfig("switch-lock");
        when(leaseService.activeTargets("demo", "dev", "default"))
                .thenReturn(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        CountDownLatch published = new CountDownLatch(1);
        doAnswer(invocation -> {
            published.countDown();
            return null;
        }).when(redisRepository).publish(any());
        DdcPublishRequest first = request("switch-lock", "true");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<DdcPublishResultVO> result =
                    executor.submit(() -> publishService.publish(first, "tester"));
            assertThat(published.await(2, TimeUnit.SECONDS)).isTrue();

            DdcPublishRequest second = request("switch-lock", "other");
            second.setExpectedVersion(2L);
            assertThatThrownBy(() -> publishService.publish(second, "tester"))
                    .isInstanceOf(DdcAdminException.class)
                    .hasMessageContaining("publish already in progress");

            publishService.ack(ack(first, "instance-1", "lease-1"));
            assertThat(result.get(2, TimeUnit.SECONDS).getStatus()).isEqualTo("SUCCESS");
        }
    }

    @Test
    void rejectsPersistedActiveChangeOwnedByAnotherAdmin() {
        saveConfig("shared-admin-lock");
        LocalDateTime now = LocalDateTime.now();
        var active = new DdcPublishTaskEntity();
        active.setId(UuidV7.simpleString());
        active.setChangeId(UuidV7.simpleString());
        active.setAppCode("demo");
        active.setEnv("dev");
        active.setNamespace("default");
        active.setConfigKey("shared-admin-lock");
        active.setStatus("PUBLISHING");
        active.setTargetCount(1);
        active.setAckCount(0);
        active.setFailedCount(0);
        active.setIgnoredCount(0);
        active.setTimeoutCount(0);
        active.setAttemptCount(0);
        active.setCreatedAt(now);
        active.setUpdatedAt(now);
        taskRepository.saveAndFlush(active);

        assertThatThrownBy(() -> publishService.publish(
                request("shared-admin-lock", "true"),
                "second-admin"
        )).isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("publish already in progress");
    }

    @Test
    void observesTerminalStateWrittenByAnotherAdminWithoutLocalSignal()
            throws Exception {
        saveConfig("shared-admin-ack");
        when(leaseService.activeTargets("demo", "dev", "default"))
                .thenReturn(List.of(
                        new DdcPublishTarget("instance-1", "lease-1")
                ));
        CountDownLatch published = new CountDownLatch(1);
        doAnswer(invocation -> {
            published.countDown();
            return null;
        }).when(redisRepository).publish(any());
        DdcPublishRequest request = request("shared-admin-ack", "true");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<DdcPublishResultVO> result = executor.submit(
                    () -> publishService.publish(request, "first-admin")
            );
            assertThat(published.await(2, TimeUnit.SECONDS)).isTrue();
            new TransactionTemplate(transactionManager).executeWithoutResult(
                    status -> taskRepository.transitionToTerminal(
                            request.getChangeId(),
                            "SUCCESS",
                            LocalDateTime.now(),
                            null,
                            null,
                            List.of("PENDING", "PUBLISHING")
                    )
            );

            assertThat(result.get(1, TimeUnit.SECONDS).getStatus())
                    .isEqualTo("SUCCESS");
        }
    }

    @Test
    void preparesDifferentResourcesInParallel() throws Exception {
        saveConfig("parallel-a");
        saveConfig("parallel-b");
        when(leaseService.activeTargets("demo", "dev", "default"))
                .thenReturn(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        CountDownLatch published = new CountDownLatch(2);
        doAnswer(invocation -> {
            published.countDown();
            return null;
        }).when(redisRepository).publish(any());
        DdcPublishRequest first = request("parallel-a", "true");
        DdcPublishRequest second = request("parallel-b", "true");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<DdcPublishResultVO> firstResult =
                    executor.submit(() -> publishService.publish(first, "tester"));
            Future<DdcPublishResultVO> secondResult =
                    executor.submit(() -> publishService.publish(second, "tester"));
            assertThat(published.await(2, TimeUnit.SECONDS)).isTrue();

            publishService.ack(ack(first, "instance-1", "lease-1"));
            publishService.ack(ack(second, "instance-1", "lease-1"));

            assertThat(firstResult.get(2, TimeUnit.SECONDS).getStatus())
                    .isEqualTo("SUCCESS");
            assertThat(secondResult.get(2, TimeUnit.SECONDS).getStatus())
                    .isEqualTo("SUCCESS");
        }
    }

    @Test
    void dispatchFailurePersistsFailedTaskAndReleasesResource() {
        saveConfig("dispatch-failure");
        when(leaseService.activeTargets("demo", "dev", "default"))
                .thenReturn(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisRepository)
                .publish(any());
        DdcPublishRequest request = request("dispatch-failure", "true");

        assertThatThrownBy(() -> publishService.publish(request, "tester"))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("publish dispatch failed");

        assertThat(taskRepository.findByChangeId(request.getChangeId()))
                .get()
                .extracting(task -> task.getStatus() + ":" + task.getFailureStage())
                .isEqualTo("FAILED:REDIS_DISPATCH");
        assertThat(resourceRegistry.owner(new DdcConfigResourceKey(
                "demo", "dev", "default", "dispatch-failure"
        ))).isEmpty();
    }

    @Test
    void oversizedUtf8ValueIsRejectedBeforeDatabaseOrRedisMutation() {
        saveConfig("oversized");
        DdcPublishRequest request = request("oversized", "你好");

        assertThatThrownBy(() -> publishService.publish(request, "tester"))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("5")
                .hasMessageNotContaining("你好");

        assertThat(taskRepository.findByChangeId(request.getChangeId())).isEmpty();
        assertThat(configItemRepository
                .findByAppCodeAndEnvAndNamespaceAndConfigKey(
                        "demo",
                        "dev",
                        "default",
                        "oversized"
                ))
                .get()
                .extracting(DdcConfigItemEntity::getConfigValue)
                .isEqualTo("false");
        verify(redisRepository, never()).publish(any());
    }

    private void saveConfig(String configKey) {
        LocalDateTime now = LocalDateTime.now();
        DdcConfigItemEntity config = new DdcConfigItemEntity();
        config.setId(UuidV7.simpleString());
        config.setAppCode("demo");
        config.setEnv("dev");
        config.setNamespace("default");
        config.setConfigKey(configKey);
        config.setConfigValue("false");
        config.setDefaultValue("false");
        config.setValueType("BOOLEAN");
        config.setCurrentVersion(1L);
        config.setEnabled(true);
        config.setDeleted(false);
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        configItemRepository.saveAndFlush(config);
    }

    private DdcPublishRequest request(String configKey, String value) {
        DdcPublishRequest request = new DdcPublishRequest();
        request.setChangeId(UuidV7.simpleString());
        request.setAppCode("demo");
        request.setEnv("dev");
        request.setNamespace("default");
        request.setConfigKey(configKey);
        request.setConfigValue(value);
        request.setExpectedVersion(1L);
        request.setTimeoutMs(2000L);
        return request;
    }

    private DdcAckRequest ack(DdcPublishRequest request,
                              String instanceId,
                              String leaseId) {
        DdcAckRequest ack = new DdcAckRequest();
        ack.setChangeId(request.getChangeId());
        ack.setInstanceId(instanceId);
        ack.setLeaseId(leaseId);
        ack.setTargetVersion(2L);
        ack.setCurrentVersion(2L);
        ack.setContentChecksum(DdcChecksum.content(request.getConfigValue()));
        ack.setStatus(DdcAckStatus.SUCCESS);
        return ack;
    }

    @TestConfiguration
    static class Dependencies {

        @Bean
        DdcConfigLeaseService ddcConfigLeaseService() {
            return org.mockito.Mockito.mock(DdcConfigLeaseService.class);
        }

        @Bean
        DdcRedisRepository ddcRedisRepository() {
            return org.mockito.Mockito.mock(DdcRedisRepository.class);
        }
    }
}
