package top.egon.cola.component.tianshu.admin.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;
import top.egon.cola.component.tianshu.admin.model.dto.DdcPublishRequest;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigVersionBO;
import top.egon.cola.component.tianshu.admin.model.enums.PublishStatus;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAtomicPublishCommand;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcRedisRepository;
import top.egon.cola.component.tianshu.admin.service.lease.DdcConfigLeaseService;
import top.egon.cola.component.tianshu.admin.service.publish.DdcPendingPublishDispatcher;
import top.egon.cola.component.tianshu.admin.service.publish.DdcPublishService;
import top.egon.cola.component.tianshu.admin.service.publish.DdcPublishStateTransitionService;
import top.egon.cola.component.tianshu.admin.service.publish.PublishCompletionWaiterRegistry;
import top.egon.cola.component.tianshu.admin.service.publish.PublishFailureRecorder;
import top.egon.cola.component.tianshu.admin.service.publish.PublishResourceLockRegistry;
import top.egon.cola.component.tianshu.admin.service.publish.PublishStartupRecovery;
import top.egon.cola.component.tianshu.format.DdcChecksum;
import top.egon.cola.component.tianshu.model.config.DdcAckRequest;
import top.egon.cola.component.tianshu.model.config.DdcPublishMessage;
import top.egon.cola.component.tianshu.model.config.DdcPublishTarget;
import top.egon.cola.component.tianshu.model.config.DdcAckStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@DataJpaTest
@EntityScan("top.egon.cola.component.tianshu.admin.model.entity")
@EnableJpaRepositories("top.egon.cola.component.tianshu.admin.repository")
@Import({
        DdcPublishService.class,
        DdcPendingPublishDispatcher.class,
        DdcPublishStateTransitionService.class,
        PublishFailureRecorder.class,
        PublishResourceLockRegistry.class,
        PublishCompletionWaiterRegistry.class,
        PublishStartupRecovery.class,
        DdcSyncPublishFlowTest.Dependencies.class
})
@EnableConfigurationProperties(DdcAdminProperties.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "egon.cola.component.id.machine-id=0",
        "spring.datasource.url=jdbc:sqlite:file:ddc_sync_flow_test?mode=memory&cache=shared",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.hikari.maximum-pool-size=1",
        "spring.flyway.enabled=false",
        "egon.cola.component.tianshu.admin.publish.default-timeout-ms=2000",
        "egon.cola.component.tianshu.admin.publish.max-timeout-ms=5000"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DdcSyncPublishFlowTest {

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    @Autowired
    private DdcPublishService publishService;

    @MockBean(name = "ddcConfigItemRepository")
    private DdcConfigItemRepository configItemRepository;

    @MockBean(name = "ddcConfigVersionRepository")
    private DdcConfigVersionRepository versionRepository;

    @Autowired
    private DdcPublishTaskRepository taskRepository;

    @Autowired
    private DdcPublishAckRepository ackRepository;

    @Autowired
    private DdcConfigLeaseService leaseService;

    @Autowired
    private DdcRedisRepository redisRepository;

    private final Map<String, DdcConfigItemBO> configItemsByScope = new HashMap<>();
    private final Map<String, DdcConfigItemBO> configItemsByPublicId = new HashMap<>();
    private final Map<String, DdcConfigVersionBO> configVersions = new HashMap<>();
    private long nextInternalConfigId;

    @Autowired
    private PublishStartupRecovery startupRecovery;

    @BeforeEach
    void setUp() {
        ackRepository.deleteAll();
        taskRepository.deleteAll();
        configItemsByScope.clear();
        configItemsByPublicId.clear();
        configVersions.clear();
        nextInternalConfigId = 1000L;
        reset(configItemRepository, versionRepository, leaseService, redisRepository);
        when(configItemRepository.findForPublishByBizCodeAndEnvAndAppCodeAndResourceName(
                any(), any(), any(), any())).thenAnswer(invocation -> Optional.ofNullable(configItemsByScope.get(
                        scopeKey(invocation.getArgument(0), invocation.getArgument(1),
                                invocation.getArgument(2), invocation.getArgument(3)))));
        when(configItemRepository.findByPublicId(any())).thenAnswer(invocation ->
                Optional.ofNullable(configItemsByPublicId.get(invocation.getArgument(0))));
        when(configItemRepository.update(any(DdcConfigItemBO.class))).thenAnswer(invocation -> {
            DdcConfigItemBO item = invocation.getArgument(0);
            item.setVersion(item.getVersion() + 1);
            configItemsByPublicId.put(item.getPublicId(), item);
            configItemsByScope.put(scopeKey(item), item);
            return item;
        });
        when(configItemRepository.advancePublishedVersion(any(), any(), any(), any())).thenAnswer(invocation -> {
            DdcConfigItemBO item = configItemsByPublicId.get(invocation.getArgument(0));
            if (item == null || !java.util.Objects.equals(item.getPublishedVersion(), invocation.getArgument(1))) return 0;
            item.setPublishedVersion(invocation.getArgument(2)).setVersion(item.getVersion() + 1);
            return 1;
        });
        when(versionRepository.insert(any(DdcConfigVersionBO.class))).thenAnswer(invocation -> {
            DdcConfigVersionBO version = invocation.getArgument(0);
            configVersions.put(versionKey(version.getConfigId(), version.getConfigRevision()), version);
            return version;
        });
        when(versionRepository.findByConfigIdAndVersion(any(), any())).thenAnswer(invocation ->
                Optional.ofNullable(configVersions.get(versionKey(invocation.getArgument(0), invocation.getArgument(1)))));
    }

    @Test
    void snapshotsExactLeasesAndReturnsOnlyAfterEverySuccessAck() throws Exception {
        saveConfig("sync-all");
        List<DdcPublishTarget> targets = List.of(
                new DdcPublishTarget("instance-2", "lease-2"),
                new DdcPublishTarget("instance-1", "lease-1")
        );
        when(leaseService.activeTargets("default", "dev", "demo"))
                .thenReturn(targets);
        LinkedBlockingQueue<DdcPublishMessage> published =
                capturePublishedMessages();
        DdcPublishRequest request = request("true", 2000);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<DdcPublishResultVO> result =
                    executor.submit(() -> publishService.publish(request, "tester"));
            DdcPublishMessage message = published.poll(2, TimeUnit.SECONDS);

            assertThat(message).isNotNull();
            assertThat(message.getTargets()).containsExactly(
                    new DdcPublishTarget("instance-1", "lease-1"),
                    new DdcPublishTarget("instance-2", "lease-2")
            );
            publishService.ack(ack(request, "instance-1", "lease-1"));
            assertThat(result.isDone()).isFalse();
            publishService.ack(ack(request, "instance-2", "lease-2"));
            assertThat(result.get(2, TimeUnit.SECONDS).getStatus())
                    .isEqualTo("SUCCESS");
        }
    }

    @Test
    void sameResourceRejectsConcurrentPublishWithoutBlockingAck() throws Exception {
        saveConfig("resource-lock");
        when(leaseService.activeTargets("default", "dev", "demo"))
                .thenReturn(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        LinkedBlockingQueue<DdcPublishMessage> published =
                capturePublishedMessages();
        DdcPublishRequest first = request("true", 2000);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<DdcPublishResultVO> result =
                    executor.submit(() -> publishService.publish(first, "tester"));
            assertThat(published.poll(2, TimeUnit.SECONDS)).isNotNull();

            DdcPublishRequest second = request("other", 2000);
            second.setExpectedVersion(2L);
            assertThatThrownBy(() -> publishService.publish(second, "tester"))
                    .isInstanceOf(DdcAdminException.class)
                    .hasMessageContaining("publish already in progress");

            publishService.ack(ack(first, "instance-1", "lease-1"));
            assertThat(result.get(2, TimeUnit.SECONDS).getStatus())
                    .isEqualTo("SUCCESS");
        }
    }

    @Test
    void exposesUncertainRedisDispatchWithoutAdvancingPublishedPointer() {
        saveConfig("dispatch-failure");
        when(leaseService.activeTargets("default", "dev", "demo"))
                .thenReturn(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisRepository)
                .dispatch(any());
        DdcPublishRequest request =
                request("true", 2000);

        assertThat(publishService.publish(request, "tester").getStatus())
                .isEqualTo(PublishStatus.UNKNOWN.name());

        assertThat(taskRepository.findByChangeId(request.getChangeId()))
                .get()
                .satisfies(task -> {
                    assertThat(task.getStatus()).isEqualTo("UNKNOWN");
                    assertThat(task.getFailureStage()).isEqualTo("REDIS_DISPATCH");
                });
        assertThat(config("demo").getPublishedVersion()).isNull();
    }

    @Test
    void restartRecoveryReplaysTheSameEvent() throws Exception {
        saveConfig("restart-unknown");
        when(leaseService.activeTargets("default", "dev", "demo"))
                .thenReturn(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        LinkedBlockingQueue<DdcPublishMessage> published =
                capturePublishedMessages();
        DdcPublishRequest request =
                request("true", 2000);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<DdcPublishResultVO> result =
                    executor.submit(() -> publishService.publish(request, "tester"));
            DdcPublishMessage first = published.poll(2, TimeUnit.SECONDS);
            assertThat(first).isNotNull();

            await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
                assertThat(config("demo").getPublishedVersion()).isEqualTo(2L);
                assertThat(taskRepository.findByChangeId(request.getChangeId()))
                        .get()
                        .satisfies(task -> assertThat(task.getStatus())
                                .isEqualTo(PublishStatus.PUBLISHING.name()));
            });
            var persistedTask = taskRepository.findByChangeId(
                    request.getChangeId()
            ).orElseThrow();
            persistedTask.setUpdatedAt(
                    LocalDateTime.now().minusMinutes(3)
            );
            taskRepository.saveAndFlush(persistedTask);
            startupRecovery.run(null);
            DdcPublishMessage replay = published.poll(2, TimeUnit.SECONDS);

            assertThat(replay).isNotNull();
            assertThat(replay.getChangeId()).isEqualTo(first.getChangeId());
            assertThat(replay.getTimestamp()).isEqualTo(first.getTimestamp());
            assertThat(replay.getChecksum()).isEqualTo(first.getChecksum());
            publishService.ack(ack(request, "instance-1", "lease-1"));
            assertThat(result.get(2, TimeUnit.SECONDS).getStatus())
                    .isEqualTo("SUCCESS");
        }
    }

    @Test
    void timeoutIsVisibleAndRetryRetainsOriginalTargets() throws Exception {
        saveConfig("timeout-retry");
        List<DdcPublishTarget> originalTargets = List.of(
                new DdcPublishTarget("instance-1", "lease-1")
        );
        when(leaseService.activeTargets("default", "dev", "demo"))
                .thenReturn(originalTargets);
        when(leaseService.areActiveTargets(
                eq("default"),
                eq("dev"),
                eq("demo"),
                anyList()
        )).thenReturn(true);
        LinkedBlockingQueue<DdcPublishMessage> published =
                capturePublishedMessages();
        DdcPublishRequest request =
                request("true", 100);

        DdcPublishResultVO timedOut =
                publishService.publish(request, "tester");
        DdcPublishMessage firstAttempt = published.poll(2, TimeUnit.SECONDS);

        assertThat(timedOut.getStatus()).isEqualTo("TIMEOUT");
        assertThat(firstAttempt).isNotNull();
        assertThat(firstAttempt.getTargets()).containsExactlyElementsOf(originalTargets);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<DdcPublishResultVO> retried =
                    executor.submit(() -> publishService.retry(request.getChangeId()));
            DdcPublishMessage retryMessage = published.poll(2, TimeUnit.SECONDS);

            assertThat(retryMessage).isNotNull();
            assertThat(retryMessage.getTargets())
                    .containsExactlyElementsOf(originalTargets);
            publishService.ack(ack(request, "instance-1", "lease-1"));
            assertThat(retried.get(2, TimeUnit.SECONDS))
                    .satisfies(result -> {
                        assertThat(result.getStatus()).isEqualTo("SUCCESS");
                        assertThat(result.getAttemptCount()).isEqualTo(1);
                    });
        }
    }

    private LinkedBlockingQueue<DdcPublishMessage> capturePublishedMessages() {
        LinkedBlockingQueue<DdcPublishMessage> published =
                new LinkedBlockingQueue<>();
        doAnswer(invocation -> {
            DdcAtomicPublishCommand command = invocation.getArgument(0);
            published.add(command.message());
            return null;
        }).when(redisRepository).dispatch(any());
        return published;
    }

    private void saveConfig(String ignoredLabel) {
        LocalDateTime now = LocalDateTime.now();
        long id = nextInternalConfigId++;
        String publicId = SnowflakeIdGenerator.nextId();
        DdcConfigItemBO config = DdcConfigItemBO.builder().id(id).tenantId(401L).publicId(publicId)
                .bizCode("default").appCode("demo").env("dev").resourceName("application.yml")
                .content("feature:\n  value: false\n").format("YAML").currentVersion(1L)
                .enabled(true).deleted(false).createdAt(now).updatedAt(now).version(0L).build();
        configItemsByScope.put(scopeKey(config), config);
        configItemsByPublicId.put(publicId, config);
    }

    private DdcConfigItemBO config(String appCode) {
        return configItemsByScope.get(scopeKey("default", "dev", appCode, "application.yml"));
    }
    private String scopeKey(DdcConfigItemBO item) {
        return scopeKey(item.getBizCode(), item.getEnv(), item.getAppCode(), item.getResourceName());
    }
    private String scopeKey(String bizCode, String env, String appCode, String resourceName) {
        return bizCode + "|" + env + "|" + appCode + "|" + resourceName;
    }
    private String versionKey(String configPublicId, Long revision) { return configPublicId + ":" + revision; }

    private DdcPublishRequest request(String value,
                                      long timeoutMs) {
        DdcPublishRequest request = new DdcPublishRequest();
        request.setChangeId(SnowflakeIdGenerator.nextId());
        request.setBizCode("default");
        request.setAppCode("demo");
        request.setEnv("dev");
        request.setResourceName("application.yml");
        request.setContent("feature:\n  value: " + value + "\n");
        request.setFormat("YAML");
        request.setExpectedVersion(1L);
        request.setTimeoutMs(timeoutMs);
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
        ack.setResourceChecksum(DdcChecksum.resource(
                request.getResourceName(),
                request.getFormat(),
                request.getContent()
        ));
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
