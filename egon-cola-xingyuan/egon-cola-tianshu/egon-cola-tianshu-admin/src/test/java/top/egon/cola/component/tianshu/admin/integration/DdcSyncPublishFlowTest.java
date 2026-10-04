package top.egon.cola.component.tianshu.admin.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;
import top.egon.cola.component.tianshu.admin.model.dto.DdcPublishRequest;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigVersionBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.enums.PublishStatus;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAtomicPublishCommand;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcRedisRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcOperationLogRepository;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantBindingDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextExecutor;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantRegistry;
import top.egon.cola.component.tianshu.admin.service.lease.DdcConfigLeaseService;
import top.egon.cola.component.tianshu.admin.service.publish.DdcPendingPublishDispatcher;
import top.egon.cola.component.tianshu.admin.service.publish.DdcPublishService;
import top.egon.cola.component.tianshu.admin.service.publish.DdcPublishStateTransitionService;
import top.egon.cola.component.tianshu.admin.service.publish.DdcAckServiceTest;
import top.egon.cola.component.tianshu.admin.service.publish.PublishCompletionWaiterRegistry;
import top.egon.cola.component.tianshu.admin.service.publish.PublishFailureRecorder;
import top.egon.cola.component.tianshu.admin.service.publish.PublishResourceLockRegistry;
import top.egon.cola.component.tianshu.admin.service.publish.PublishStartupRecovery;
import top.egon.cola.component.tianshu.format.DdcChecksum;
import top.egon.cola.component.tianshu.model.config.DdcAckRequest;
import top.egon.cola.component.tianshu.model.config.DdcPublishMessage;
import top.egon.cola.component.tianshu.model.event.DdcConfigDispatchPreparedEvent;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.tianshu.model.config.DdcPublishTarget;
import top.egon.cola.component.tianshu.model.config.DdcAckStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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

class DdcSyncPublishFlowTest {

    @BeforeAll
    static void bindTheProcessWideEngine() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    private DdcPublishService publishService;

    private DdcConfigItemRepository configItemRepository;

    private DdcConfigVersionRepository versionRepository;

    private DdcPublishTaskRepository taskRepository;

    private DdcPublishAckRepository ackRepository;

    private DdcConfigLeaseService leaseService;

    private DdcRedisRepository redisRepository;

    private final Map<String, DdcConfigItemBO> configItemsByScope = new ConcurrentHashMap<>();
    private final Map<String, DdcConfigItemBO> configItemsByPublicId = new ConcurrentHashMap<>();
    private final Map<String, DdcConfigVersionBO> configVersions = new ConcurrentHashMap<>();
    private long nextInternalConfigId;

    private PublishStartupRecovery startupRecovery;
    private DdcAckServiceTest.InMemoryPersistence persistence;
    private DdcTenantContextExecutor.Scope tenantScope;

    @BeforeEach
    void setUp() {
        tenantScope = new DdcTenantContextExecutor().bind(
                new DdcTenantContextDTO(101L, "tenant-test", "test:sync-publish"));
        configItemRepository = Mockito.mock(DdcConfigItemRepository.class);
        versionRepository = Mockito.mock(DdcConfigVersionRepository.class);
        leaseService = Mockito.mock(DdcConfigLeaseService.class);
        redisRepository = Mockito.mock(DdcRedisRepository.class);
        DdcOperationLogRepository operationLogRepository = Mockito.mock(DdcOperationLogRepository.class);
        DdcAdminProperties adminProperties = new DdcAdminProperties();
        adminProperties.setMaxConfigBytes(64);
        adminProperties.getPublish().setDefaultTimeoutMs(2000L);
        adminProperties.getPublish().setMaxTimeoutMs(5000L);
        adminProperties.getPublish().setRecoveryStaleMs(120000L);
        adminProperties.getPublish().setInlineContentMaxBytes(64);
        persistence = new DdcAckServiceTest.InMemoryPersistence();
        taskRepository = persistence.tasks;
        ackRepository = persistence.acks;
        publishService = persistence.service(configItemRepository, versionRepository, operationLogRepository,
                leaseService, redisRepository, adminProperties);
        DdcTenantBindingDTO binding = DdcTenantBindingDTO.builder().tenantId(101L)
                .identityTenantId("tenant-test").enabled(true).build();
        DdcTenantAdminProperties tenantProperties = new DdcTenantAdminProperties();
        tenantProperties.setBindings(List.of(binding));
        DdcTenantRegistry registry = Mockito.mock(DdcTenantRegistry.class);
        Mockito.when(registry.requireEnabledTenant(101L)).thenReturn(binding);
        startupRecovery = new PublishStartupRecovery(taskRepository, persistence.dispatcher(), adminProperties,
                tenantProperties, registry, new DdcTenantContextExecutor());
        configItemsByScope.clear();
        configItemsByPublicId.clear();
        configVersions.clear();
        nextInternalConfigId = 1000L;
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

    @AfterEach
    void restoreTenantContext() {
        tenantScope.close();
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
                    executor.submit(() -> DdcAckServiceTest.withTenantContext(
                            () -> publishService.publish(request, "tester")));
            DdcConfigDispatchPreparedEvent event = deliverNextPreparedEvent();
            DdcPublishMessage message = published.poll(2, TimeUnit.SECONDS);

            assertThat(message).isNotNull();
            assertThat(event.getMessage()).isEqualTo(message);
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
                    executor.submit(() -> DdcAckServiceTest.withTenantContext(
                            () -> publishService.publish(first, "tester")));
            deliverNextPreparedEvent();
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
    void transientDeliveryFailureDoesNotAdvancePointerOrCompleteBusinessPublish() throws Exception {
        saveConfig("dispatch-failure");
        when(leaseService.activeTargets("default", "dev", "demo"))
                .thenReturn(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisRepository)
                .dispatch(any(), any());
        DdcPublishRequest request =
                request("true", 2000);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<DdcPublishResultVO> result = executor.submit(() -> DdcAckServiceTest.withTenantContext(
                    () -> publishService.publish(request, "tester")));
            DdcConfigDispatchPreparedEvent event = persistence.preparedEvent(
                    persistence.takeOutboxMessage(2, TimeUnit.SECONDS));

            assertThatThrownBy(() -> persistence.deliver(event))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("redis unavailable");
            assertThat(result.isDone()).isFalse();
            assertThat(taskRepository.findByChangeId(request.getChangeId()))
                    .get().extracting(DdcPublishTaskBO::getStatus)
                    .isEqualTo(PublishStatus.PUBLISHING.name());
            assertThat(config("demo").getPublishedVersion()).isNull();

            persistence.stateTransitions().timeout(request.getChangeId(), "test timeout");
            assertThat(result.get(2, TimeUnit.SECONDS).getStatus()).isEqualTo(PublishStatus.TIMEOUT.name());
        }
    }

    @Test
    void restartRecoveryRequeuesTheSameFrozenOutboxIdentity() throws Exception {
        saveConfig("restart-unknown");
        when(leaseService.activeTargets("default", "dev", "demo"))
                .thenReturn(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        LinkedBlockingQueue<DdcPublishMessage> published =
                capturePublishedMessages();
        DdcPublishRequest request =
                request("true", 2000);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<DdcPublishResultVO> result =
                    executor.submit(() -> DdcAckServiceTest.withTenantContext(
                            () -> publishService.publish(request, "tester")));
            DdcConfigDispatchPreparedEvent first = persistence.preparedEvent(
                    persistence.takeOutboxMessage(2, TimeUnit.SECONDS));
            assertThat(first.getAttempt()).isZero();
            var persistedTask = taskRepository.findByChangeId(
                    request.getChangeId()
            ).orElseThrow();
            persistedTask.setUpdatedAt(
                    LocalDateTime.now().minusMinutes(3)
            );
            taskRepository.update(persistedTask);
            startupRecovery.run(null);
            DdcConfigDispatchPreparedEvent retry = persistence.preparedEvent(
                    persistence.takeOutboxMessage(2, TimeUnit.SECONDS));

            assertThat(retry.getAttempt()).isEqualTo(0);
            assertThat(retry.getEventId()).isEqualTo(first.getEventId());
            assertThat(retry.getMessage().getTimestamp()).isEqualTo(first.getMessage().getTimestamp());
            assertThat(retry.getEventChecksum()).isEqualTo(first.getEventChecksum());
            assertThat(persistence.deliver(first)).isTrue();
            assertThat(persistence.deliver(retry)).isTrue();
            DdcPublishMessage delivered = published.poll(2, TimeUnit.SECONDS);
            assertThat(delivered).isNotNull();
            assertThat(delivered.getChangeId()).isEqualTo(retry.getMessage().getChangeId());
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

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<DdcPublishResultVO> timedOutFuture = executor.submit(() -> DdcAckServiceTest.withTenantContext(
                    () -> publishService.publish(request, "tester")));
            DdcConfigDispatchPreparedEvent firstAttempt = persistence.preparedEvent(
                    persistence.takeOutboxMessage(2, TimeUnit.SECONDS));
            assertThat(persistence.deliver(firstAttempt)).isTrue();
            DdcPublishMessage firstMessage = published.poll(2, TimeUnit.SECONDS);
            assertThat(firstMessage).isNotNull();
            assertThat(firstMessage.getTargets()).containsExactlyElementsOf(originalTargets);
            assertThat(timedOutFuture.get(2, TimeUnit.SECONDS).getStatus()).isEqualTo("TIMEOUT");

            Future<DdcPublishResultVO> retried =
                    executor.submit(() -> DdcAckServiceTest.withTenantContext(
                            () -> publishService.retry(request.getChangeId())));
            DdcConfigDispatchPreparedEvent retryEvent = persistence.preparedEvent(
                    persistence.takeOutboxMessage(2, TimeUnit.SECONDS));
            assertThat(retryEvent.getAttempt()).isEqualTo(1);
            assertThat(retryEvent.getEventId()).isNotEqualTo(firstAttempt.getEventId());
            assertThat(persistence.deliver(retryEvent)).isTrue();
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
        }).when(redisRepository).dispatch(any(), any());
        return published;
    }

    private DdcConfigDispatchPreparedEvent deliverNextPreparedEvent() throws InterruptedException {
        OutboxMessage outboxMessage = persistence.takeOutboxMessage(2, TimeUnit.SECONDS);
        assertThat(outboxMessage).isNotNull();
        DdcConfigDispatchPreparedEvent event = persistence.preparedEvent(outboxMessage);
        assertThat(persistence.deliver(event)).isTrue();
        return event;
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
}
