package top.egon.cola.component.tianshu.admin.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.OutboxReceipt;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandlerRegistry;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.config.DdcAdminProperties;
import top.egon.cola.component.tianshu.admin.config.DdcOutboxConfiguration.DdcConfigDispatchOutboxRouteDTO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigItemBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcConfigVersionBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishAckBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcPublishTaskBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcConfigDispatchEventConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcConfigDispatchEventConverterImpl;
import top.egon.cola.component.tianshu.admin.model.enums.PublishMode;
import top.egon.cola.component.tianshu.admin.model.enums.PublishStatus;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigItemRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcConfigVersionRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishAckRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcPublishTaskRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcRedisRepository;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantAdminProperties;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantBindingDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextExecutor;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantRegistry;
import top.egon.cola.component.tianshu.admin.service.publish.DdcPendingPublishDispatcher;
import top.egon.cola.component.tianshu.admin.service.publish.DdcPublishStateTransitionService;
import top.egon.cola.component.tianshu.format.DdcChecksum;
import top.egon.cola.component.tianshu.model.config.DdcPublishTarget;
import top.egon.cola.component.tianshu.model.event.DdcConfigDispatchPreparedEvent;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcConfigOutboxContractTest {

    private static final String CHANGE_ID = "change-outbox-1";
    private static final String CONFIG_ID = "810001";
    private static final String CONTENT = "feature:\n  enabled: true\n";
    private static final Instant NOW = Instant.parse("2026-10-02T00:00:00Z");

    private final DataSource outboxDataSource = org.mockito.Mockito.mock(DataSource.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final DdcConfigDispatchOutboxRouteDTO route =
            new DdcConfigDispatchOutboxRouteDTO("tianshu.config-dispatch", "1");
    private final TransactionalOutbox outbox = org.mockito.Mockito.mock(TransactionalOutbox.class);
    private final DdcConfigItemRepository configItems = org.mockito.Mockito.mock(DdcConfigItemRepository.class);
    private final DdcConfigVersionRepository versions = org.mockito.Mockito.mock(DdcConfigVersionRepository.class);
    private final DdcPublishTaskRepository tasks = org.mockito.Mockito.mock(DdcPublishTaskRepository.class);
    private final DdcPublishAckRepository acks = org.mockito.Mockito.mock(DdcPublishAckRepository.class);
    private final DdcRedisRepository redis = org.mockito.Mockito.mock(DdcRedisRepository.class);
    private final DdcPublishStateTransitionService transitions =
            org.mockito.Mockito.mock(DdcPublishStateTransitionService.class);
    private final DdcConfigDispatchEventConverter converter = new DdcConfigDispatchEventConverterImpl();
    private final ValidationUtils validationUtils = new ValidationUtils(
            Validation.buildDefaultValidatorFactory().getValidator());
    private final SelectedDataSourceTransactionManager transactionManager =
            new SelectedDataSourceTransactionManager(outboxDataSource);
    private final DdcAdminProperties properties = new DdcAdminProperties();

    private DdcPublishTaskBO task;
    private DdcConfigItemBO config;
    private DdcConfigDispatchPreparedEvent capturedEvent;
    private DdcPendingPublishDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());
        String resourceChecksum = DdcChecksum.resource("application.yml", "YAML", CONTENT);
        task = DdcPublishTaskBO.builder()
                .id(901L).tenantId(101L).publicId("810002").version(0L)
                .changeId(CHANGE_ID).configItemId(900L).configId(CONFIG_ID)
                .bizCode("retail").appCode("orders").env("dev").resourceName("application.yml")
                .targetVersion(2L).publishMode(PublishMode.SYNC_ALL_ACK.name())
                .resourceChecksum(resourceChecksum).attemptCount(0).status(PublishStatus.PENDING.name())
                .targetCount(1).ackCount(0).failedCount(0).ignoredCount(0).timeoutCount(0)
                .timeoutMs(60_000L).operator("user:test").createdAt(now.minusSeconds(10)).updatedAt(now)
                .build();
        config = DdcConfigItemBO.builder()
                .id(900L).tenantId(101L).publicId(CONFIG_ID).version(0L)
                .bizCode("retail").appCode("orders").env("dev").resourceName("application.yml")
                .content(CONTENT).format("YAML").currentVersion(2L).publishedVersion(1L)
                .enabled(true).deleted(false).createdAt(now.minusSeconds(20)).updatedAt(now)
                .build();
        DdcConfigVersionBO version = DdcConfigVersionBO.builder()
                .id(902L).tenantId(101L).publicId("810003").version(0L)
                .configItemId(900L).configId(CONFIG_ID).bizCode("retail").appCode("orders").env("dev")
                .resourceName("application.yml").configRevision(2L).oldContent("feature: false\n")
                .newContent(CONTENT).format("YAML").changeType("UPDATE").operator("user:test")
                .createdAt(now).build();
        DdcPublishAckBO target = DdcPublishAckBO.builder()
                .id(903L).tenantId(101L).publicId("810004").version(0L)
                .changeId(CHANGE_ID).instanceId("instance-1").leaseId("lease-1")
                .resourceChecksum(resourceChecksum).bizCode("retail").appCode("orders").env("dev")
                .resourceName("application.yml").targetVersion(2L)
                .build();
        when(tasks.findByChangeId(CHANGE_ID)).thenAnswer(ignored -> Optional.of(task));
        when(tasks.findForUpdateByChangeId(CHANGE_ID)).thenAnswer(ignored -> Optional.of(task));
        when(configItems.findByPublicId(CONFIG_ID)).thenAnswer(ignored -> Optional.of(config));
        when(versions.findByConfigIdAndVersion(CONFIG_ID, 2L)).thenAnswer(ignored -> Optional.of(version));
        when(acks.findByChangeId(CHANGE_ID)).thenAnswer(ignored -> new CopyOnWriteArrayList<>(List.of(target)));
        when(configItems.advancePublishedVersion(anyString(), any(), any(), any())).thenAnswer(invocation -> {
            config.setPublishedVersion(invocation.getArgument(2));
            return 1;
        });
        when(transitions.markPublishing(CHANGE_ID)).thenAnswer(ignored -> {
            task.setStatus(PublishStatus.PUBLISHING.name())
                    .setDispatchedAt(now)
                    .setUpdatedAt(now);
            return task;
        });
        when(transitions.unknown(anyString(), anyString(), anyString())).thenAnswer(invocation ->
                task.setStatus(PublishStatus.UNKNOWN.name())
                        .setFailureStage(invocation.getArgument(1))
                        .setErrorMessage(invocation.getArgument(2)));
        when(transitions.fail(anyString(), anyString(), anyString())).thenAnswer(invocation ->
                task.setStatus(PublishStatus.FAILED.name())
                        .setFailureStage(invocation.getArgument(1))
                        .setErrorMessage(invocation.getArgument(2)));
        when(transitions.refreshAfterAck(CHANGE_ID)).thenAnswer(ignored -> task);
        when(outbox.enqueue(any())).thenAnswer(invocation -> {
            OutboxMessage message = invocation.getArgument(0);
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            assertThat(TransactionSynchronizationManager.hasResource(outboxDataSource)).isTrue();
            capturedEvent = (DdcConfigDispatchPreparedEvent) message.payload();
            return new OutboxReceipt(message.messageId(), message.idempotencyKey(), true);
        });
        dispatcher = new DdcPendingPublishDispatcher(
                configItems, versions, tasks, acks, redis, transitions, converter, outbox, route,
                new org.springframework.transaction.support.TransactionTemplate(transactionManager),
                clock, validationUtils, properties);
    }

    @Test
    void freezesAndEnqueuesSameAttemptInsideTheSelectedDatasourceTransaction() {
        assertThat(dispatcher.dispatch(CHANGE_ID).getStatus()).isEqualTo(PublishStatus.PUBLISHING.name());
        DdcConfigDispatchPreparedEvent first = capturedEvent;
        assertThat(first).isNotNull();
        assertThat(first.getTenantId()).isEqualTo(101L);
        assertThat(first.getMessage().getContent()).isEqualTo(CONTENT);
        assertThat(first.getAttempt()).isZero();
        assertThat(route.destination(first)).contains("tianshu:v4:tenant:101:");

        assertThat(dispatcher.dispatch(CHANGE_ID).getStatus()).isEqualTo(PublishStatus.PUBLISHING.name());
        DdcConfigDispatchPreparedEvent replay = capturedEvent;

        assertThat(replay.getEventId()).isEqualTo(first.getEventId());
        assertThat(replay.getEventChecksum()).isEqualTo(first.getEventChecksum());
        assertThat(replay.getMessage().getTimestamp()).isEqualTo(first.getMessage().getTimestamp());
        verify(transitions, times(1)).markPublishing(CHANGE_ID);
        verify(outbox, times(2)).enqueue(any());
    }

    @Test
    void enqueueFailureDoesNotReturnAQueuedOrSuccessfulTask() {
        doThrow(new IllegalStateException("outbox store unavailable")).when(outbox).enqueue(any());

        DdcPublishTaskBO result = dispatcher.dispatch(CHANGE_ID);

        assertThat(result.getStatus()).isEqualTo(PublishStatus.UNKNOWN.name());
        assertThat(result.getFailureStage()).isEqualTo("OUTBOX_ENQUEUE");
        assertThat(config.getPublishedVersion()).isEqualTo(1L);
    }

    @Test
    void staleAttemptIsPermanentlyRejectedBeforeRedisAndSuccessfulDeliveryDoesNotCompleteAck() throws Exception {
        dispatcher.dispatch(CHANGE_ID);
        DdcConfigDispatchPreparedEvent event = capturedEvent;
        task.setAttemptCount(1);
        DdcTenantRegistry tenantRegistry = org.mockito.Mockito.mock(DdcTenantRegistry.class);
        when(tenantRegistry.requireEnabledTenant(101L)).thenReturn(
                DdcTenantBindingDTO.builder().tenantId(101L).identityTenantId("tenant-test").enabled(true).build());
        DdcConfigDispatchDeliveryHandler handler = new DdcConfigDispatchDeliveryHandler(
                new ObjectMapper(), validationUtils, tenantRegistry, new DdcTenantContextExecutor(),
                dispatcherProvider(), converter, route);
        assertThat(new DeliveryHandlerRegistry(List.of(handler)).required(route.channel())).isSameAs(handler);
        DeliveryResult stale = handler.deliver(context(event));
        assertThat(stale.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        verify(redis, never()).dispatch(any(), any());

        task.setAttemptCount(0);
        DeliveryResult delivered = handler.deliver(context(event));

        assertThat(delivered.kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
        assertThat(task.getStatus()).isEqualTo(PublishStatus.PUBLISHING.name());
        verify(transitions, times(1)).refreshAfterAck(CHANGE_ID);
        assertThat(dispatcher.dispatch(CHANGE_ID).getStatus()).isEqualTo(PublishStatus.PUBLISHING.name());
        verify(outbox, times(1)).enqueue(any());
    }

    @Test
    void transientRedisFailureIsRetryableAndDoesNotManufactureClientAck() throws Exception {
        dispatcher.dispatch(CHANGE_ID);
        DdcConfigDispatchPreparedEvent event = capturedEvent;
        DdcTenantRegistry tenantRegistry = tenantRegistry();
        DdcConfigDispatchDeliveryHandler handler = handler(tenantRegistry);
        doThrow(new IllegalStateException("redis unavailable")).when(redis).dispatch(any(), any());

        DeliveryResult result = handler.deliver(context(event));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
        assertThat(config.getPublishedVersion()).isEqualTo(1L);
        assertThat(task.getStatus()).isEqualTo(PublishStatus.PUBLISHING.name());
        verify(transitions, never()).refreshAfterAck(CHANGE_ID);
    }

    @Test
    void permanentRedisConflictFailsTaskWithoutPretendingToReceiveClientAck() throws Exception {
        dispatcher.dispatch(CHANGE_ID);
        DdcConfigDispatchPreparedEvent event = capturedEvent;
        DdcConfigDispatchDeliveryHandler handler = handler(tenantRegistry());
        doThrow(new DdcAdminException("publish idempotency fingerprint conflict"))
                .when(redis).dispatch(any(), any());

        DeliveryResult result = handler.deliver(context(event));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        assertThat(task.getStatus()).isEqualTo(PublishStatus.FAILED.name());
        assertThat(task.getFailureStage()).isEqualTo("REDIS_DISPATCH");
        assertThat(config.getPublishedVersion()).isEqualTo(1L);
        verify(transitions, never()).refreshAfterAck(CHANGE_ID);
    }

    private DeliveryContext context(DdcConfigDispatchPreparedEvent event) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(event);
        var json = mapper.readTree(payload);
        assertThat(json.path("tenantId").asText()).isEqualTo(Long.toString(event.getTenantId()));
        assertThat(json.path("schemaVersion").asText()).isEqualTo("1");
        assertThat(json.path("message").path("content").asText()).isEqualTo(CONTENT);
        assertThat(json.path("message").has("namespace")).isFalse();
        return new DeliveryContext(
                event.getEventId(), route.channel(), route.destination(event), payload,
                "application/json", event.getSchemaVersion(),
                Map.of("tenantId", Long.toString(event.getTenantId()), "eventType", route.eventType()),
                null, 1, 10, clock.instant().plusSeconds(60));
    }

    private DdcConfigDispatchDeliveryHandler handler(DdcTenantRegistry tenantRegistry) {
        return new DdcConfigDispatchDeliveryHandler(
                new ObjectMapper(), validationUtils, tenantRegistry, new DdcTenantContextExecutor(),
                dispatcherProvider(), converter, route);
    }

    private org.springframework.beans.factory.ObjectProvider<DdcPendingPublishDispatcher> dispatcherProvider() {
        return new org.springframework.beans.factory.support.StaticListableBeanFactory(
                Map.of("ddcPendingPublishDispatcher", dispatcher)).getBeanProvider(DdcPendingPublishDispatcher.class);
    }

    @Test
    void registeringTheDeliveryHandlerDoesNotInitializeItsOutboxPublisher() {
        var factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        factory.registerBeanDefinition("ddcPendingPublishDispatcher",
                new org.springframework.beans.factory.support.RootBeanDefinition(
                        DdcPendingPublishDispatcher.class, () -> dispatcher));
        DdcConfigDispatchDeliveryHandler handler = new DdcConfigDispatchDeliveryHandler(
                new ObjectMapper(), validationUtils, tenantRegistry(), new DdcTenantContextExecutor(),
                factory.getBeanProvider(DdcPendingPublishDispatcher.class), converter, route);

        assertThat(new DeliveryHandlerRegistry(List.of(handler)).required(route.channel())).isSameAs(handler);
        assertThat(factory.containsSingleton("ddcPendingPublishDispatcher")).isFalse();
    }

    private DdcTenantRegistry tenantRegistry() {
        DdcTenantRegistry tenantRegistry = org.mockito.Mockito.mock(DdcTenantRegistry.class);
        when(tenantRegistry.requireEnabledTenant(101L)).thenReturn(
                DdcTenantBindingDTO.builder().tenantId(101L).identityTenantId("tenant-test").enabled(true).build());
        return tenantRegistry;
    }

    private static final class SelectedDataSourceTransactionManager implements PlatformTransactionManager {
        private final DataSource dataSource;

        private SelectedDataSourceTransactionManager(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            TransactionSynchronizationManager.setActualTransactionActive(true);
            TransactionSynchronizationManager.initSynchronization();
            TransactionSynchronizationManager.bindResource(dataSource, new Object());
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
            clearTransaction();
        }

        @Override
        public void rollback(TransactionStatus status) {
            clearTransaction();
        }

        private void clearTransaction() {
            if (TransactionSynchronizationManager.hasResource(dataSource)) {
                TransactionSynchronizationManager.unbindResource(dataSource);
            }
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }
}
