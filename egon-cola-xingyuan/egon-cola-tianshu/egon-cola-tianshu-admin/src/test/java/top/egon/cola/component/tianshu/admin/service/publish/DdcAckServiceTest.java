package top.egon.cola.component.tianshu.admin.service.publish;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;
import top.egon.cola.component.tianshu.admin.config.DdcOutboxConfiguration.DdcConfigDispatchOutboxRouteDTO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishAckBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.enums.PublishMode;
import top.egon.cola.component.tianshu.admin.model.enums.PublishStatus;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcOperationLogRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcRedisRepository;
import top.egon.cola.component.tianshu.admin.service.lease.DdcConfigLeaseService;
import top.egon.cola.component.tianshu.admin.service.config.DdcYamlConfigValidator;
import top.egon.cola.component.tianshu.admin.service.publish.PublishCompletionWaiterRegistry;
import top.egon.cola.component.tianshu.admin.service.publish.PublishFailureRecorder;
import top.egon.cola.component.tianshu.admin.service.publish.PublishResourceLockRegistry;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextExecutor;
import top.egon.cola.component.tianshu.admin.model.bo.DdcOperationLogBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcConfigDispatchEventConverterImpl;
import top.egon.cola.component.tianshu.format.DdcChecksum;
import top.egon.cola.component.tianshu.model.config.DdcAckRequest;
import top.egon.cola.component.tianshu.model.config.DdcAckStatus;
import top.egon.cola.component.tianshu.model.event.DdcConfigDispatchPreparedEvent;

import jakarta.validation.Validation;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DdcAckServiceTest {

    private static final DdcTenantContextDTO TEST_TENANT =
            new DdcTenantContextDTO(101L, "tenant-test", "test:publish");
    private static final DdcTenantContextExecutor TENANT_CONTEXT_EXECUTOR = new DdcTenantContextExecutor();

    @BeforeAll
    static void bindSnowflakeGenerator() {
        SnowflakeIdGenerator.initialize(0L, Duration.ofMillis(5));
    }

    private static final String CONFIG_VALUE =
            "feature:\n  enabled: true\n";

    private static final String CHECKSUM = DdcChecksum.resource(
            "application.yml",
            "YAML",
            CONFIG_VALUE
    );

    private DdcPublishService publishService;

    private DdcPublishTaskRepository taskRepository;

    private DdcConfigItemRepository configItemRepository;

    private DdcConfigVersionRepository versionRepository;

    private DdcPublishAckRepository ackRepository;

    private DdcPublishStateTransitionService stateTransitions;
    private DdcTenantContextExecutor.Scope tenantScope;

    private final Map<String, DdcConfigItemBO> configItemsByPublicId = new HashMap<>();
    private long nextInternalConfigId = 100L;

    @BeforeEach
    void setUp() {
        tenantScope = TENANT_CONTEXT_EXECUTOR.bind(TEST_TENANT);
        configItemRepository = Mockito.mock(DdcConfigItemRepository.class);
        versionRepository = Mockito.mock(DdcConfigVersionRepository.class);
        DdcOperationLogRepository operationLogRepository = Mockito.mock(DdcOperationLogRepository.class);
        DdcRedisRepository redisRepository = Mockito.mock(DdcRedisRepository.class);
        DdcConfigLeaseService leaseService = Mockito.mock(DdcConfigLeaseService.class);
        DdcAdminProperties properties = new DdcAdminProperties();
        properties.setMaxConfigBytes(64);
        properties.getPublish().setInlineContentMaxBytes(64);
        InMemoryPersistence persistence = new InMemoryPersistence();
        taskRepository = persistence.tasks;
        ackRepository = persistence.acks;
        configItemsByPublicId.clear();
        nextInternalConfigId = 100L;
        Mockito.reset(configItemRepository, versionRepository);
        Mockito.when(configItemRepository.findByPublicId(Mockito.any()))
                .thenAnswer(invocation -> Optional.ofNullable(configItemsByPublicId.get(invocation.getArgument(0))));
        publishService = persistence.service(configItemRepository, versionRepository,
                operationLogRepository, leaseService, redisRepository, properties);
        stateTransitions = persistence.stateTransitions();
    }

    @AfterEach
    void restoreTenantContext() {
        if (tenantScope != null) {
            tenantScope.close();
        }
    }

    public static <T> T withTenantContext(Supplier<T> action) {
        return TENANT_CONTEXT_EXECUTOR.call(TEST_TENANT, action);
    }

    public static void withTenantContext(Runnable action) {
        TENANT_CONTEXT_EXECUTOR.run(TEST_TENANT, action);
    }

    @Test
    void exactDuplicateAckIsIdempotentAndCompletesTask() {
        DdcPublishTaskBO task = savePublishingTask("ack-success");
        saveTarget(task, "instance-1", "lease-1");
        DdcAckRequest request =
                ackRequest(task.getChangeId(), "instance-1", "lease-1", CHECKSUM);

        publishService.ack(request);
        publishService.ack(request);

        assertThat(ackRepository.findByChangeId(task.getChangeId())).hasSize(1);
        assertThat(taskRepository.findByChangeId(task.getChangeId()))
                .get()
                .satisfies(updated -> {
                    assertThat(updated.getAckCount()).isEqualTo(1);
                    assertThat(updated.getStatus()).isEqualTo(PublishStatus.SUCCESS.name());
                });
    }

    @Test
    void duplicateAckCompletesAfterPublishedPointerCatchesUp() {
        DdcPublishTaskBO task = savePublishingTask("ack-pointer-catch-up");
        DdcConfigItemBO config = configItemRepository.findByPublicId(task.getConfigId())
                .orElseThrow();
        config.setPublishedVersion(1L);
        saveTarget(task, "instance-1", "lease-1");
        DdcAckRequest request =
                ackRequest(task.getChangeId(), "instance-1", "lease-1", CHECKSUM);

        assertThat(publishService.ack(request).getStatus())
                .isEqualTo(PublishStatus.PUBLISHING.name());
        config = configItemRepository.findByPublicId(task.getConfigId()).orElseThrow();
        config.setPublishedVersion(2L);

        assertThat(publishService.ack(request).getStatus())
                .isEqualTo(PublishStatus.SUCCESS.name());
        assertThat(ackRepository.findByChangeId(task.getChangeId())).hasSize(1);
    }

    @Test
    void rejectsUnknownIdentityVersionAndChecksumWithoutCreatingTarget() {
        DdcPublishTaskBO task = savePublishingTask("ack-reject");
        saveTarget(task, "instance-1", "lease-1");

        assertThatThrownBy(() -> publishService.ack(
                ackRequest(task.getChangeId(), "instance-1", "stale-lease", CHECKSUM)
        )).isInstanceOf(DdcAdminException.class);
        DdcAckRequest wrongVersion =
                ackRequest(task.getChangeId(), "instance-1", "lease-1", CHECKSUM);
        wrongVersion.setTargetVersion(3L);
        assertThatThrownBy(() -> publishService.ack(wrongVersion))
                .isInstanceOf(DdcAdminException.class);
        assertThatThrownBy(() -> publishService.ack(
                ackRequest(task.getChangeId(), "instance-1", "lease-1", "wrong")
        )).isInstanceOf(DdcAdminException.class);

        assertThat(ackRepository.findByChangeId(task.getChangeId()))
                .singleElement()
                .satisfies(target -> assertThat(target.getAckStatus()).isNull());
        assertThat(taskRepository.findByChangeId(task.getChangeId()))
                .get()
                .satisfies(updated -> {
                    assertThat(updated.getAckCount()).isZero();
                    assertThat(updated.getStatus()).isEqualTo(PublishStatus.PUBLISHING.name());
                });
    }

    @Test
    void failedAckWinsAndTerminalTaskIsReadOnly() {
        DdcPublishTaskBO task = savePublishingTask("ack-failed");
        saveTarget(task, "instance-1", "lease-1");
        DdcAckRequest failed =
                ackRequest(task.getChangeId(), "instance-1", "lease-1", CHECKSUM);
        failed.setStatus(DdcAckStatus.FAILED);

        publishService.ack(failed);
        publishService.ack(
                ackRequest(task.getChangeId(), "instance-1", "lease-1", CHECKSUM)
        );

        assertThat(taskRepository.findByChangeId(task.getChangeId()))
                .get()
                .extracting(DdcPublishTaskBO::getStatus)
                .isEqualTo(PublishStatus.FAILED.name());
        assertThat(ackRepository.findByChangeId(task.getChangeId()))
                .singleElement()
                .extracting(DdcPublishAckBO::getAckStatus)
                .isEqualTo(DdcAckStatus.FAILED.name());
    }

    @Test
    void ackAndTimeoutEachPreserveTheFirstTerminalWinner() {
        DdcPublishTaskBO ackWinner = savePublishingTask("ack-wins");
        saveTarget(ackWinner, "instance-1", "lease-1");
        publishService.ack(ackRequest(
                ackWinner.getChangeId(),
                "instance-1",
                "lease-1",
                CHECKSUM
        ));
        stateTransitions.timeout(ackWinner.getChangeId(), "late timeout");
        assertThat(taskRepository.findByChangeId(ackWinner.getChangeId()))
                .get()
                .extracting(DdcPublishTaskBO::getStatus)
                .isEqualTo(PublishStatus.SUCCESS.name());

        DdcPublishTaskBO timeoutWinner = savePublishingTask("timeout-wins");
        saveTarget(timeoutWinner, "instance-1", "lease-1");
        stateTransitions.timeout(timeoutWinner.getChangeId(), "timeout");
        publishService.ack(ackRequest(
                timeoutWinner.getChangeId(),
                "instance-1",
                "lease-1",
                CHECKSUM
        ));
        assertThat(taskRepository.findByChangeId(timeoutWinner.getChangeId()))
                .get()
                .extracting(DdcPublishTaskBO::getStatus)
                .isEqualTo(PublishStatus.TIMEOUT.name());
        assertThat(ackRepository.findByChangeId(timeoutWinner.getChangeId()))
                .singleElement()
                .extracting(DdcPublishAckBO::getAckStatus)
                .isEqualTo(DdcAckStatus.TIMEOUT.name());
    }

    private DdcPublishTaskBO savePublishingTask(String label) {
        LocalDateTime now = LocalDateTime.now();
        DdcPublishTaskBO task = new DdcPublishTaskBO();
        String configId = SnowflakeIdGenerator.nextId();
        long internalConfigId = nextInternalConfigId++;
        DdcConfigItemBO config = DdcConfigItemBO.builder().id(internalConfigId).tenantId(401L)
                .publicId(configId).bizCode("default").appCode(label).env("dev")
                .resourceName("application.yml").content(CONFIG_VALUE).format("YAML")
                .currentVersion(2L).publishedVersion(2L).enabled(true).deleted(false)
                .createdAt(now).updatedAt(now).version(0L).build();
        configItemsByPublicId.put(configId, config);

        task.setChangeId(SnowflakeIdGenerator.nextId());
        task.setConfigId(configId);
        task.setConfigItemId(internalConfigId);
        task.setBizCode("default");
        task.setAppCode(label);
        task.setEnv("dev");
        task.setNamespace("default");
        task.setResourceName("application.yml");
        task.setTargetVersion(2L);
        task.setResourceChecksum(CHECKSUM);
        task.setPublishMode(PublishMode.SYNC_ALL_ACK.name());
        task.setStatus(PublishStatus.PUBLISHING.name());
        task.setTargetCount(1);
        task.setAckCount(0);
        task.setFailedCount(0);
        task.setIgnoredCount(0);
        task.setTimeoutCount(0);
        task.setTimeoutMs(2000L);
        task.setCreatedAt(now);
        task.setDispatchedAt(now);
        task.setUpdatedAt(now);
        return taskRepository.insert(task);
    }

    private void saveTarget(DdcPublishTaskBO task,
                            String instanceId,
                            String leaseId) {
        DdcPublishAckBO target = new DdcPublishAckBO();
        target.setChangeId(task.getChangeId());
        target.setInstanceId(instanceId);
        target.setLeaseId(leaseId);
        target.setBizCode(task.getBizCode());
        target.setAppCode(task.getAppCode());
        target.setEnv(task.getEnv());
        target.setNamespace(task.getNamespace());
        target.setResourceName(task.getResourceName());
        target.setTargetVersion(task.getTargetVersion());
        target.setResourceChecksum(task.getResourceChecksum());
        ackRepository.insert(target);
    }

    private DdcAckRequest ackRequest(String changeId,
                                     String instanceId,
                                     String leaseId,
                                     String checksum) {
        DdcAckRequest request = new DdcAckRequest();
        request.setChangeId(changeId);
        request.setInstanceId(instanceId);
        request.setLeaseId(leaseId);
        request.setTargetVersion(2L);
        request.setCurrentVersion(2L);
        request.setResourceChecksum(checksum);
        request.setStatus(DdcAckStatus.SUCCESS);
        return request;
    }

    public static final class InMemoryPersistence {

        private final Map<String, DdcPublishTaskBO> taskRows = new ConcurrentHashMap<>();
        private final Map<String, CopyOnWriteArrayList<DdcPublishAckBO>> ackRows = new ConcurrentHashMap<>();
        private final AtomicLong ids = new AtomicLong(20000L);
        public final DdcPublishTaskRepository tasks = Mockito.mock(DdcPublishTaskRepository.class);
        public final DdcPublishAckRepository acks = Mockito.mock(DdcPublishAckRepository.class);
        public final TestTransactionManager transactionManager = new TestTransactionManager();
        public final TransactionalOutbox outbox = Mockito.mock(TransactionalOutbox.class);
        private final LinkedBlockingQueue<OutboxMessage> pendingOutbox = new LinkedBlockingQueue<>();
        public final PublishResourceLockRegistry resourceRegistry = new PublishResourceLockRegistry();
        public final PublishCompletionWaiterRegistry waiterRegistry = new PublishCompletionWaiterRegistry();
        private DdcPublishStateTransitionService transitions;
        private DdcPendingPublishDispatcher dispatcher;

        public InMemoryPersistence() {
            Mockito.when(outbox.enqueue(Mockito.any())).thenAnswer(invocation -> {
                OutboxMessage message = invocation.getArgument(0);
                pendingOutbox.add(message);
                return new OutboxReceipt(message.messageId(), message.idempotencyKey(), true);
            });
            Mockito.when(tasks.findByChangeId(Mockito.anyString()))
                    .thenAnswer(invocation -> Optional.ofNullable(taskRows.get(invocation.getArgument(0))));
            Mockito.when(tasks.findForUpdateByChangeId(Mockito.anyString()))
                    .thenAnswer(invocation -> Optional.ofNullable(taskRows.get(invocation.getArgument(0))));
            Mockito.when(tasks.findByStatusIn(Mockito.anyCollection())).thenAnswer(invocation -> {
                Collection<String> statuses = invocation.getArgument(0);
                return taskRows.values().stream().filter(task -> statuses.contains(task.getStatus())).toList();
            });
            Mockito.when(tasks.findByStatusInAndUpdatedAtBefore(Mockito.anyCollection(), Mockito.any()))
                    .thenAnswer(invocation -> {
                        Collection<String> statuses = invocation.getArgument(0);
                        LocalDateTime cutoff = invocation.getArgument(1);
                        return taskRows.values().stream().filter(task -> statuses.contains(task.getStatus())
                                && task.getUpdatedAt() != null && task.getUpdatedAt().isBefore(cutoff)).toList();
                    });
            Mockito.when(tasks.findFirstByBizCodeAndEnvAndAppCodeAndResourceNameAndStatusIn(
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                    Mockito.anyCollection())).thenAnswer(invocation -> {
                Collection<String> statuses = invocation.getArgument(4);
                return taskRows.values().stream().filter(task -> statuses.contains(task.getStatus())
                        && Objects.equals(task.getBizCode(), invocation.getArgument(0))
                        && Objects.equals(task.getEnv(), invocation.getArgument(1))
                        && Objects.equals(task.getAppCode(), invocation.getArgument(2))
                        && Objects.equals(task.getResourceName(), invocation.getArgument(3))).findFirst();
            });
            Mockito.when(tasks.insert(Mockito.any(DdcPublishTaskBO.class))).thenAnswer(invocation -> {
                DdcPublishTaskBO task = invocation.getArgument(0);
                long id = ids.getAndIncrement();
                task.setId(id).setTenantId(101L).setPublicId(task.getPublicId() == null
                        ? Long.toString(id) : task.getPublicId()).setVersion(0L);
                taskRows.put(task.getChangeId(), task);
                return task;
            });
            Mockito.when(tasks.updateCounters(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(),
                    Mockito.anyInt(), Mockito.anyInt(), Mockito.any())).thenAnswer(invocation -> {
                DdcPublishTaskBO task = taskRows.get(invocation.getArgument(0));
                if (task == null) return 0;
                task.setAckCount(invocation.getArgument(1)).setFailedCount(invocation.getArgument(2))
                        .setIgnoredCount(invocation.getArgument(3)).setTimeoutCount(invocation.getArgument(4))
                        .setUpdatedAt(invocation.getArgument(5));
                return 1;
            });
            Mockito.when(tasks.transitionToTerminal(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
                    Mockito.any(), Mockito.any(), Mockito.anyCollection())).thenAnswer(invocation -> {
                DdcPublishTaskBO task = taskRows.get(invocation.getArgument(0));
                Collection<String> statuses = invocation.getArgument(5);
                if (task == null || !statuses.contains(task.getStatus())) return 0;
                task.setStatus(invocation.getArgument(1)).setCompletedAt(invocation.getArgument(2))
                        .setFailureStage(invocation.getArgument(3)).setErrorMessage(invocation.getArgument(4))
                        .setUpdatedAt(invocation.getArgument(2));
                return 1;
            });
            Mockito.when(tasks.transitionToPublishing(Mockito.anyString(), Mockito.anyCollection(), Mockito.anyString(),
                    Mockito.anyString(), Mockito.any())).thenAnswer(invocation -> {
                DdcPublishTaskBO task = taskRows.get(invocation.getArgument(0));
                Collection<String> statuses = invocation.getArgument(1);
                if (task == null || !statuses.contains(task.getStatus())) return 0;
                String current = task.getStatus();
                task.setStatus(invocation.getArgument(3));
                if (!Objects.equals(invocation.getArgument(2), current)) {
                    task.setAttemptCount((task.getAttemptCount() == null ? 0 : task.getAttemptCount()) + 1);
                }
                task.setDispatchedAt(invocation.getArgument(4)).setCompletedAt(null)
                        .setFailureStage(null).setErrorMessage(null).setUpdatedAt(invocation.getArgument(4));
                return 1;
            });
            Mockito.when(tasks.resetForRetry(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
                    Mockito.any(), Mockito.anyCollection())).thenAnswer(invocation -> {
                DdcPublishTaskBO task = taskRows.get(invocation.getArgument(0));
                Collection<String> statuses = invocation.getArgument(4);
                if (task == null || !statuses.contains(task.getStatus())) return 0;
                task.setStatus(invocation.getArgument(1))
                        .setAttemptCount((task.getAttemptCount() == null ? 0 : task.getAttemptCount()) + 1)
                        .setDispatchedAt(null).setCompletedAt(null).setFailureStage(null).setErrorMessage(null)
                        .setAckCount(0).setFailedCount(0).setIgnoredCount(0).setTimeoutCount(0)
                        .setUpdatedAt(invocation.getArgument(3));
                if (invocation.getArgument(2) != null) task.setOperator(invocation.getArgument(2));
                return 1;
            });
            Mockito.when(tasks.markRetryLeaseExpired(Mockito.anyString(), Mockito.anyString(), Mockito.any(),
                    Mockito.any(), Mockito.any(), Mockito.anyCollection())).thenAnswer(invocation -> {
                DdcPublishTaskBO task = taskRows.get(invocation.getArgument(0));
                Collection<String> statuses = invocation.getArgument(5);
                if (task == null || !statuses.contains(task.getStatus())) return 0;
                task.setStatus(invocation.getArgument(1)).setCompletedAt(invocation.getArgument(2))
                        .setFailureStage(invocation.getArgument(3)).setErrorMessage(invocation.getArgument(4))
                        .setUpdatedAt(invocation.getArgument(2));
                return 1;
            });
            Mockito.when(tasks.claimStaleForRecovery(Mockito.anyString(), Mockito.anyCollection(), Mockito.any(),
                    Mockito.any())).thenAnswer(invocation -> {
                DdcPublishTaskBO task = taskRows.get(invocation.getArgument(0));
                Collection<String> statuses = invocation.getArgument(1);
                LocalDateTime cutoff = invocation.getArgument(2);
                if (task == null || !statuses.contains(task.getStatus()) || task.getUpdatedAt() == null
                        || !task.getUpdatedAt().isBefore(cutoff)) return 0;
                task.setUpdatedAt(invocation.getArgument(3));
                return 1;
            });
            Mockito.when(acks.findByChangeId(Mockito.anyString())).thenAnswer(invocation ->
                    List.copyOf(ackRows.getOrDefault(invocation.getArgument(0), new CopyOnWriteArrayList<>())));
            Mockito.when(acks.findByChangeIdAndInstanceIdAndLeaseId(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                    .thenAnswer(invocation -> findAck(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
            Mockito.when(acks.findForUpdateByChangeIdAndInstanceIdAndLeaseId(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                    .thenAnswer(invocation -> findAck(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2)));
            Mockito.when(acks.insert(Mockito.any(DdcPublishAckBO.class))).thenAnswer(invocation -> {
                DdcPublishAckBO ack = invocation.getArgument(0);
                long id = ids.getAndIncrement();
                ack.setId(id).setTenantId(101L).setPublicId(ack.getPublicId() == null
                        ? Long.toString(id) : ack.getPublicId()).setVersion(0L);
                ackRows.computeIfAbsent(ack.getChangeId(), ignored -> new CopyOnWriteArrayList<>()).add(ack);
                return ack;
            });
            Mockito.when(acks.insertAll(Mockito.anyList())).thenAnswer(invocation -> {
                List<DdcPublishAckBO> inserted = invocation.getArgument(0);
                inserted.forEach(acks::insert);
                return inserted;
            });
            Mockito.when(acks.update(Mockito.any(DdcPublishAckBO.class))).thenAnswer(invocation -> invocation.getArgument(0));
            Mockito.when(acks.markIncompleteTimeout(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                    .thenAnswer(invocation -> ackRows.getOrDefault(invocation.getArgument(0), new CopyOnWriteArrayList<>())
                            .stream().filter(ack -> ack.getAckStatus() == null)
                            .map(ack -> ack.setAckStatus(invocation.getArgument(1)).setAckAt(invocation.getArgument(2)))
                            .mapToInt(ignored -> 1).sum());
            Mockito.when(acks.resetTargets(Mockito.anyString())).thenAnswer(invocation ->
                    ackRows.getOrDefault(invocation.getArgument(0), new CopyOnWriteArrayList<>()).stream()
                            .map(ack -> ack.setCurrentVersion(null).setAckStatus(null).setErrorMessage(null).setAckAt(null))
                            .mapToInt(ignored -> 1).sum());
        }

        public DdcPublishStateTransitionService stateTransitions(DdcConfigItemRepository configItems) {
            transitions = new DdcPublishStateTransitionService(tasks, configItems, acks, resourceRegistry,
                    waiterRegistry, Clock.systemUTC());
            return transitions;
        }

        public DdcPublishStateTransitionService stateTransitions() {
            return transitions;
        }

        public DdcPublishService service(DdcConfigItemRepository configItems,
                DdcConfigVersionRepository versions, DdcOperationLogRepository operationLogs,
                DdcConfigLeaseService leases, DdcRedisRepository redisRepository, DdcAdminProperties properties) {
            DdcPublishStateTransitionService state = stateTransitions(configItems);
            dispatcher = new DdcPendingPublishDispatcher(
                    configItems, versions, tasks, acks, redisRepository, state,
                    new DdcConfigDispatchEventConverterImpl(), outbox,
                    new DdcConfigDispatchOutboxRouteDTO("tianshu.config-dispatch", "1"),
                    new org.springframework.transaction.support.TransactionTemplate(transactionManager),
                    Clock.systemUTC(), new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator()),
                    properties);
            return new DdcPublishService(configItems, versions, tasks, acks, operationLogs, dispatcher,
                    leases, resourceRegistry, waiterRegistry, state, new PublishFailureRecorder(tasks),
                    properties, new DdcYamlConfigValidator(properties.getMaxConfigBytes()),
                    new org.springframework.transaction.support.TransactionTemplate(transactionManager),
                    Clock.systemUTC());
        }

        public DdcPendingPublishDispatcher dispatcher() {
            return dispatcher;
        }

        public OutboxMessage takeOutboxMessage(long timeout, TimeUnit unit) throws InterruptedException {
            return pendingOutbox.poll(timeout, unit);
        }

        public OutboxMessage pollOutboxMessage() {
            return pendingOutbox.poll();
        }

        public DdcConfigDispatchPreparedEvent preparedEvent(OutboxMessage message) {
            return (DdcConfigDispatchPreparedEvent) message.payload();
        }

        public boolean deliver(DdcConfigDispatchPreparedEvent event) {
            return dispatcher.deliverPrepared(event);
        }

        private Optional<DdcPublishAckBO> findAck(String changeId, String instanceId, String leaseId) {
            return ackRows.getOrDefault(changeId, new CopyOnWriteArrayList<>()).stream()
                    .filter(ack -> Objects.equals(ack.getInstanceId(), instanceId)
                            && Objects.equals(ack.getLeaseId(), leaseId)).findFirst();
        }

        public static final class TestTransactionManager implements PlatformTransactionManager {

            private final ThreadLocal<Runnable> rollback = ThreadLocal.withInitial(() -> () -> { });

            public void onRollback(Runnable action) {
                rollback.set(action == null ? () -> { } : action);
            }

            @Override public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override public void commit(TransactionStatus status) {
                if (status.isRollbackOnly()) {
                    rollback(status);
                } else {
                    rollback.remove();
                }
            }

            @Override public void rollback(TransactionStatus status) {
                try {
                    rollback.get().run();
                } finally {
                    rollback.remove();
                }
            }
        }
    }
}
