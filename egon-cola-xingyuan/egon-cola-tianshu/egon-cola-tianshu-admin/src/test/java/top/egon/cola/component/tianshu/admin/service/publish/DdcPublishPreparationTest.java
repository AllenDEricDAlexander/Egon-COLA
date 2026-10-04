package top.egon.cola.component.tianshu.admin.service.publish;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;
import top.egon.cola.component.tianshu.admin.model.dto.DdcPublishRequest;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigVersionBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.enums.PublishStatus;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAtomicPublishCommand;
import top.egon.cola.component.tianshu.admin.model.vo.DdcConfigResourceKey;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPublishResultVO;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcOperationLogRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcRedisRepository;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextExecutor;
import top.egon.cola.component.tianshu.admin.service.lease.DdcConfigLeaseService;
import top.egon.cola.component.tianshu.format.DdcChecksum;
import top.egon.cola.component.tianshu.model.config.DdcAckRequest;
import top.egon.cola.component.tianshu.model.config.DdcPublishMessage;
import top.egon.cola.component.tianshu.model.config.DdcPublishTarget;
import top.egon.cola.component.tianshu.model.config.DdcAckStatus;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.tianshu.model.event.DdcConfigDispatchPreparedEvent;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcPublishPreparationTest {

    private DdcPublishService publishService;

    private DdcConfigItemRepository configItemRepository;

    private DdcConfigVersionRepository versionRepository;

    private DdcPublishTaskRepository taskRepository;

    private DdcPublishAckRepository ackRepository;

    private DdcConfigLeaseService leaseService;

    private DdcRedisRepository redisRepository;

    private PublishResourceLockRegistry resourceRegistry;

    private PlatformTransactionManager transactionManager;
    private DdcAckServiceTest.InMemoryPersistence persistence;
    private DdcTenantContextExecutor.Scope tenantScope;

    private final Map<String, DdcConfigItemBO> configItemsByScope = new HashMap<>();
    private final Map<String, DdcConfigItemBO> configItemsByPublicId = new HashMap<>();
    private final Map<String, DdcConfigVersionBO> configVersions = new HashMap<>();
    private long nextInternalConfigId;

    @BeforeEach
    void setUp() {
        tenantScope = new DdcTenantContextExecutor().bind(
                new DdcTenantContextDTO(101L, "tenant-test", "test:publish-preparation"));
        configItemRepository = mock(DdcConfigItemRepository.class);
        versionRepository = mock(DdcConfigVersionRepository.class);
        DdcOperationLogRepository operationLogRepository = mock(DdcOperationLogRepository.class);
        leaseService = mock(DdcConfigLeaseService.class);
        redisRepository = mock(DdcRedisRepository.class);
        DdcAdminProperties properties = new DdcAdminProperties();
        properties.setMaxConfigBytes(64);
        properties.getPublish().setDefaultTimeoutMs(2000L);
        properties.getPublish().setMaxTimeoutMs(5000L);
        properties.getPublish().setInlineContentMaxBytes(64);
        persistence = new DdcAckServiceTest.InMemoryPersistence();
        taskRepository = persistence.tasks;
        ackRepository = persistence.acks;
        resourceRegistry = persistence.resourceRegistry;
        transactionManager = persistence.transactionManager;
        publishService = persistence.service(configItemRepository, versionRepository,
                operationLogRepository, leaseService, redisRepository, properties);
        configItemsByScope.clear();
        configItemsByPublicId.clear();
        configVersions.clear();
        nextInternalConfigId = 1000L;
        when(configItemRepository.findForPublishByBizCodeAndEnvAndAppCodeAndResourceName(
                any(), any(), any(), any())).thenAnswer(invocation -> Optional.ofNullable(copyItem(configItemsByScope.get(
                        scopeKey(invocation.getArgument(0), invocation.getArgument(1),
                                invocation.getArgument(2), invocation.getArgument(3))))));
        when(configItemRepository.findByPublicId(any())).thenAnswer(invocation ->
                Optional.ofNullable(configItemsByPublicId.get(invocation.getArgument(0))));
        when(configItemRepository.update(any(DdcConfigItemBO.class))).thenAnswer(invocation -> {
            DdcConfigItemBO item = invocation.getArgument(0);
            DdcConfigItemBO previous = configItemsByPublicId.get(item.getPublicId());
            if (previous != null) {
                DdcConfigItemBO snapshot = copyItem(previous);
                persistence.transactionManager.onRollback(() -> {
                    configItemsByPublicId.put(snapshot.getPublicId(), snapshot);
                    configItemsByScope.put(scopeKey(snapshot), snapshot);
                });
            }
            item.setVersion(item.getVersion() + 1);
            configItemsByPublicId.put(item.getPublicId(), item);
            configItemsByScope.put(scopeKey(item), item);
            return item;
        });
        when(configItemRepository.advancePublishedVersion(any(), any(), any(), any())).thenAnswer(invocation -> {
            DdcConfigItemBO item = configItemsByPublicId.get(invocation.getArgument(0));
            if (item == null || !java.util.Objects.equals(item.getPublishedVersion(), invocation.getArgument(1))) return 0;
            item.setPublishedVersion(invocation.getArgument(2)).setUpdatedAt(invocation.getArgument(3));
            item.setVersion(item.getVersion() + 1);
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

    private DdcConfigItemBO copyItem(DdcConfigItemBO source) {
        if (source == null) return null;
        DdcConfigItemBO copy = new DdcConfigItemBO();
        BeanUtils.copyProperties(source, copy);
        return copy;
    }

    @Test
    void fixesRedisLeaseTargetsAndCompletesOnlyAfterEverySuccessAck() throws Exception {
        saveConfig("switch-all");
        List<DdcPublishTarget> targets = List.of(
                new DdcPublishTarget("instance-2", "lease-2"),
                new DdcPublishTarget("instance-1", "lease-1")
        );
        when(leaseService.activeTargets("default", "dev", "switch-all"))
                .thenReturn(targets);
        CountDownLatch published = new CountDownLatch(1);
        doAnswer(invocation -> {
            published.countDown();
            return null;
        }).when(redisRepository).dispatch(any(), any());
        DdcPublishRequest request = request("switch-all", "true");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<DdcPublishResultVO> result =
                    executor.submit(() -> DdcAckServiceTest.withTenantContext(
                            () -> publishService.publish(request, "tester")));
            OutboxMessage outboxMessage = persistence.takeOutboxMessage(2, TimeUnit.SECONDS);
            assertThat(outboxMessage).isNotNull();
            DdcConfigDispatchPreparedEvent event = persistence.preparedEvent(outboxMessage);
            assertThat(event.getEventId()).hasSize(64);
            assertThat(event.getAttempt()).isZero();
            assertThat(persistence.deliver(event)).isTrue();
            assertThat(published.await(2, TimeUnit.SECONDS)).isTrue();

            ArgumentCaptor<DdcAtomicPublishCommand> command =
                    ArgumentCaptor.forClass(DdcAtomicPublishCommand.class);
            verify(redisRepository).dispatch(command.capture(), any());
            DdcPublishMessage message = command.getValue().message();
            assertThat(message.getTargets())
                    .containsExactly(
                            new DdcPublishTarget("instance-1", "lease-1"),
                            new DdcPublishTarget("instance-2", "lease-2")
                    );
            assertThat(message.getResourceChecksum())
                    .isEqualTo(DdcChecksum.resource(
                            request.getResourceName(),
                            request.getFormat(),
                            request.getContent()
                    ));
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
        assertThat(config("switch-all").getCurrentVersion()).isEqualTo(2L);
    }

    @Test
    void noLiveRedisLeaseRollsBackConfigAndNeverReadsDatabaseOnlineState() {
        saveConfig("switch-no-live");
        when(leaseService.activeTargets("default", "dev", "switch-no-live"))
                .thenReturn(List.of());
        DdcPublishRequest request = request("switch-no-live", "true");

        assertThatThrownBy(() -> publishService.publish(request, "tester"))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("no live config instance");

        assertThat(config("switch-no-live").getCurrentVersion()).isEqualTo(1L);
        assertThat(config("switch-no-live").getContent()).isEqualTo("feature:\n  value: false\n");
    }

    @Test
    void rejectsAnotherChangeForTheSameResourceWithoutBlockingAck() throws Exception {
        saveConfig("switch-lock");
        when(leaseService.activeTargets("default", "dev", "switch-lock"))
                .thenReturn(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        CountDownLatch published = new CountDownLatch(1);
        doAnswer(invocation -> {
            published.countDown();
            return null;
        }).when(redisRepository).dispatch(any(), any());
        DdcPublishRequest first = request("switch-lock", "true");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<DdcPublishResultVO> result =
                    executor.submit(() -> DdcAckServiceTest.withTenantContext(
                            () -> publishService.publish(first, "tester")));
            OutboxMessage outboxMessage = persistence.takeOutboxMessage(2, TimeUnit.SECONDS);
            assertThat(outboxMessage).isNotNull();
            assertThat(persistence.deliver(persistence.preparedEvent(outboxMessage))).isTrue();
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
        var active = new DdcPublishTaskBO();
        active.setChangeId(SnowflakeIdGenerator.nextId());
        active.setBizCode("default");
        active.setAppCode("shared-admin-lock");
        active.setEnv("dev");
        active.setNamespace("default");
        active.setResourceName("application.yml");
        active.setStatus("PUBLISHING");
        active.setTargetCount(1);
        active.setAckCount(0);
        active.setFailedCount(0);
        active.setIgnoredCount(0);
        active.setTimeoutCount(0);
        active.setAttemptCount(0);
        active.setCreatedAt(now);
        active.setUpdatedAt(now);
        taskRepository.insert(active);

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
        when(leaseService.activeTargets("default", "dev", "shared-admin-ack"))
                .thenReturn(List.of(
                        new DdcPublishTarget("instance-1", "lease-1")
                ));
        CountDownLatch published = new CountDownLatch(1);
        doAnswer(invocation -> {
            published.countDown();
            return null;
        }).when(redisRepository).dispatch(any(), any());
        DdcPublishRequest request = request("shared-admin-ack", "true");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<DdcPublishResultVO> result = executor.submit(
                    () -> DdcAckServiceTest.withTenantContext(
                            () -> publishService.publish(request, "first-admin"))
            );
            OutboxMessage outboxMessage = persistence.takeOutboxMessage(2, TimeUnit.SECONDS);
            assertThat(outboxMessage).isNotNull();
            assertThat(persistence.deliver(persistence.preparedEvent(outboxMessage))).isTrue();
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
            assertThat(resourceRegistry.owner(new DdcConfigResourceKey(
                    "shared-admin-ack", "dev", "default", "application.yml"
            ))).isEmpty();
        }
    }

    @Test
    void preparesDifferentScopesInParallel() throws Exception {
        saveConfig("parallel-a");
        saveConfig("parallel-b");
        when(leaseService.activeTargets("default", "dev", "parallel-a"))
                .thenReturn(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        when(leaseService.activeTargets("default", "dev", "parallel-b"))
                .thenReturn(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        CountDownLatch published = new CountDownLatch(2);
        doAnswer(invocation -> {
            published.countDown();
            return null;
        }).when(redisRepository).dispatch(any(), any());
        DdcPublishRequest first = request("parallel-a", "true");
        DdcPublishRequest second = request("parallel-b", "true");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<DdcPublishResultVO> firstResult =
                    executor.submit(() -> DdcAckServiceTest.withTenantContext(
                            () -> publishService.publish(first, "tester")));
            Future<DdcPublishResultVO> secondResult =
                    executor.submit(() -> DdcAckServiceTest.withTenantContext(
                            () -> publishService.publish(second, "tester")));
            OutboxMessage firstOutbox = persistence.takeOutboxMessage(2, TimeUnit.SECONDS);
            OutboxMessage secondOutbox = persistence.takeOutboxMessage(2, TimeUnit.SECONDS);
            assertThat(firstOutbox).isNotNull();
            assertThat(secondOutbox).isNotNull();
            assertThat(persistence.deliver(persistence.preparedEvent(firstOutbox))).isTrue();
            assertThat(persistence.deliver(persistence.preparedEvent(secondOutbox))).isTrue();
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
    void uncertainDispatchFailureKeepsRecoverableTaskAndReleasesResource() {
        saveConfig("dispatch-failure");
        when(leaseService.activeTargets("default", "dev", "dispatch-failure"))
                .thenReturn(List.of(new DdcPublishTarget("instance-1", "lease-1")));
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(persistence.outbox)
                .enqueue(any());
        DdcPublishRequest request = request("dispatch-failure", "true");

        assertThat(publishService.publish(request, "tester").getStatus())
                .isEqualTo(PublishStatus.UNKNOWN.name());

        assertThat(taskRepository.findByChangeId(request.getChangeId()))
                .get()
                .extracting(task -> task.getStatus() + ":" + task.getFailureStage())
                .isEqualTo("UNKNOWN:OUTBOX_ENQUEUE");
        assertThat(resourceRegistry.owner(new DdcConfigResourceKey(
                "dispatch-failure", "dev", "default", "application.yml"
        ))).isEmpty();
    }

    @Test
    void oversizedUtf8ValueIsRejectedBeforeDatabaseOrRedisMutation() {
        saveConfig("oversized");
        DdcPublishRequest request = request("oversized", "你".repeat(64));

        assertThatThrownBy(() -> publishService.publish(request, "tester"))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("64")
                .hasMessageNotContaining("你你");

        assertThat(taskRepository.findByChangeId(request.getChangeId())).isEmpty();
        assertThat(config("oversized").getContent()).isEqualTo("feature:\n  value: false\n");
        verify(redisRepository, never()).dispatch(any());
    }

    @Test
    void malformedAndReservedYamlAreRejectedBeforePublishPreparation() {
        saveConfig("invalid-yaml");
        DdcPublishRequest malformed = request("invalid-yaml", "true");
        malformed.setContent("feature: [");
        DdcPublishRequest reserved = request("invalid-yaml", "true");
        reserved.setContent(
                "spring:\n  config:\n    import: optional:file:local.yml\n"
        );

        assertThatThrownBy(() -> publishService.publish(malformed, "tester"))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("invalid application.yml");
        assertThatThrownBy(() -> publishService.publish(reserved, "tester"))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("reserved");
        assertThat(taskRepository.findByChangeId(malformed.getChangeId()))
                .isEmpty();
        assertThat(taskRepository.findByChangeId(reserved.getChangeId()))
                .isEmpty();
        verify(redisRepository, never()).dispatch(any());
    }

    private void saveConfig(String label) {
        LocalDateTime now = LocalDateTime.now();
        long id = nextInternalConfigId++;
        String publicId = SnowflakeIdGenerator.nextId();
        DdcConfigItemBO config = DdcConfigItemBO.builder().id(id).tenantId(401L).publicId(publicId)
                .bizCode("default").appCode(label).env("dev").resourceName("application.yml")
                .content("feature:\n  value: false\n").format("YAML").currentVersion(1L)
                .publishedVersion(null).enabled(true).deleted(false).createdAt(now).updatedAt(now).version(0L).build();
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

    private DdcPublishRequest request(String label, String value) {
        DdcPublishRequest request = new DdcPublishRequest();
        request.setChangeId(SnowflakeIdGenerator.nextId());
        request.setBizCode("default");
        request.setAppCode(label);
        request.setEnv("dev");
        request.setResourceName("application.yml");
        request.setContent("feature:\n  value: " + value + "\n");
        request.setFormat("YAML");
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
        ack.setResourceChecksum(DdcChecksum.resource(
                request.getResourceName(),
                request.getFormat(),
                request.getContent()
        ));
        ack.setStatus(DdcAckStatus.SUCCESS);
        return ack;
    }
}
