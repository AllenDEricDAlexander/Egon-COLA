package top.egon.cola.component.outbox.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.api.UuidOutboxIdGenerator;
import top.egon.cola.component.outbox.autoconfigure.OutboxMpStorageProperties;
import top.egon.cola.component.outbox.autoconfigure.OutboxStateMachineProperties;
import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.deadletter.OutboxDeadLetterNotifier;
import top.egon.cola.component.outbox.delivery.DefaultDeliveryFailureClassifier;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandlerRegistry;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.outbox.delivery.statemachine.BusinessStateMachineDeliveryHandler;
import top.egon.cola.component.outbox.dispatch.OutboxDispatcher;
import top.egon.cola.component.outbox.dispatch.OutboxWorkerIdentity;
import top.egon.cola.component.outbox.observability.NoopOutboxMetrics;
import top.egon.cola.component.outbox.retry.OutboxRetryPolicy;
import top.egon.cola.component.outbox.serialization.JacksonOutboxMessageSerializer;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineContextExecutor;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineDefinitionStrategy;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineEvent;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineRepository;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineService;
import top.egon.cola.component.outbox.statemachine.BusinessStateMachineSnapshotBO;
import top.egon.cola.component.outbox.statemachine.OutboxLifecycleService;
import top.egon.cola.component.outbox.statemachine.StateMachineExecutionService;
import top.egon.cola.component.outbox.store.OutboxStatus;
import top.egon.cola.component.outbox.store.OutboxStore;
import top.egon.cola.component.outbox.transaction.DefaultTransactionalOutbox;
import top.egon.cola.component.outbox.transaction.OutboxAfterCommitBuffer;
import top.egon.cola.component.outbox.transaction.OutboxTransactionGuard;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessStateMachineIntegrationTest extends PostgresqlOutboxTestSupport {

    private static final String DESTINATION = "order-payment:1";
    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();
    private static ValidatorFactory validatorFactory;
    private static ValidationUtils validationUtils;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validationUtils = new ValidationUtils(validatorFactory.getValidator());
        jdbcTemplate.execute("""
                ALTER TABLE public.outbox_test_order
                    ADD COLUMN IF NOT EXISTS tenant_id bigint NOT NULL DEFAULT 7,
                    ADD COLUMN IF NOT EXISTS business_id varchar(128) NOT NULL DEFAULT 'order-1',
                    ADD COLUMN IF NOT EXISTS definition_version integer NOT NULL DEFAULT 1,
                    ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 1,
                    ADD COLUMN IF NOT EXISTS facts jsonb NOT NULL DEFAULT '{}'::jsonb,
                    ADD COLUMN IF NOT EXISTS applied_receipts jsonb NOT NULL DEFAULT '{}'::jsonb
                """);
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void prepareBusinessFixture() {
        jdbcTemplate.update("""
                INSERT INTO public.outbox_test_order
                    (id, state, tenant_id, business_id, definition_version, version, facts, applied_receipts)
                VALUES (1, 'AWAITING_PAYMENT', 7, 'order-1', 1, 1, '{}'::jsonb, '{}'::jsonb)
                """);
    }

    @AfterEach
    void clearTenantContext() {
        CURRENT_TENANT.remove();
    }

    @Test
    void shouldReplayAfterTechnicalAcknowledgementFailureWithoutApplyingBusinessEventTwice() throws Exception {
        JdbcBusinessStateMachineRepository repository = new JdbcBusinessStateMachineRepository(jdbcTemplate, objectMapper);
        BusinessStateMachineDeliveryHandler handler = handler(repository, false);
        DeliveryHandlerRegistry registry = new DeliveryHandlerRegistry(List.of(handler));
        TransactionalOutbox transactionalOutbox = transactionalOutbox(registry);
        BusinessStateMachineEvent event = event("business-event-1");

        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                transactionalOutbox.enqueue(message("transport-message-1", "idempotency-1", event)));

        OutboxStore store = outboxStore();
        dispatcher(store, registry, true).submitDue();

        assertThat(status("transport-message-1")).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(businessState()).isEqualTo("PAID");
        assertThat(businessVersion()).isEqualTo(2L);
        assertThat(receiptCount()).isEqualTo(1);

        jdbcTemplate.update("""
                UPDATE egon_outbox.egon_cola_outbox_message
                SET locked_until = clock_timestamp() - interval '1 second'
                WHERE message_id = 'transport-message-1'
                """);
        dispatcher(store, registry, false).submitDue();

        assertThat(status("transport-message-1")).isEqualTo(OutboxStatus.SUCCEEDED);
        assertThat(businessState()).isEqualTo("PAID");
        assertThat(businessVersion()).isEqualTo(2L);
        assertThat(receiptCount()).isEqualTo(1);
        assertThat(attemptCount("transport-message-1")).isEqualTo(2);
    }

    @Test
    void shouldRollbackStateAndReceiptTogetherWhenRepositoryFailsAfterWriting() throws Exception {
        JdbcBusinessStateMachineRepository repository = new JdbcBusinessStateMachineRepository(jdbcTemplate, objectMapper);
        BusinessStateMachineDeliveryHandler handler = handler(repository, true);
        BusinessStateMachineEvent event = event("business-event-rollback");
        DeliveryContext deliveryContext = deliveryContext("transport-message-rollback", event);

        DeliveryResult failed = handler.deliver(deliveryContext);

        assertThat(failed.kind()).isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
        assertThat(businessState()).isEqualTo("AWAITING_PAYMENT");
        assertThat(businessVersion()).isEqualTo(1L);
        assertThat(receiptCount()).isZero();

        DeliveryResult retried = handler.deliver(deliveryContext);

        assertThat(retried).isEqualTo(DeliveryResult.success());
        assertThat(businessState()).isEqualTo("PAID");
        assertThat(businessVersion()).isEqualTo(2L);
        assertThat(receiptCount()).isEqualTo(1);
    }

    private BusinessStateMachineDeliveryHandler handler(
            JdbcBusinessStateMachineRepository repository,
            boolean failAfterWrite
    ) throws Exception {
        repository.failAfterWrite(failAfterWrite);
        StateMachineFactory<String, String> factory = machineFactory();
        BusinessStateMachineDefinitionStrategy definition = new BusinessStateMachineDefinitionStrategy() {
            @Override
            public String destination() {
                return DESTINATION;
            }

            @Override
            public StateMachineFactory<String, String> stateMachineFactory() {
                return factory;
            }

            @Override
            public BusinessStateMachineRepository repository() {
                return repository;
            }

            @Override
            public void validateEvent(BusinessStateMachineEvent event) {
            }
        };
        Map<String, BusinessStateMachineDefinitionStrategy> definitions = Map.of(DESTINATION, definition);
        TransactionTemplate businessTransaction = new TransactionTemplate(transactionManager);
        businessTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        businessTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        businessTransaction.setTimeout(5);
        BusinessStateMachineService service = new BusinessStateMachineService(
                definitions,
                tenantContextExecutor(),
                businessTransaction,
                new StateMachineExecutionService(validationUtils),
                new OutboxStateMachineProperties(),
                objectMapper,
                validationUtils,
                context.getBean("outboxStateMachineClock", Clock.class)
        );
        return new BusinessStateMachineDeliveryHandler(service, definitions, objectMapper, validationUtils);
    }

    @SuppressWarnings("unchecked")
    private StateMachineFactory<String, String> machineFactory() throws Exception {
        StateMachineFactory<String, String> factory = mock(StateMachineFactory.class);
        when(factory.getStateMachine()).thenAnswer(invocation -> paymentMachine());
        return factory;
    }

    private StateMachine<String, String> paymentMachine() throws Exception {
        StateMachineBuilder.Builder<String, String> builder = StateMachineBuilder.builder();
        builder.configureConfiguration().withConfiguration().autoStartup(false);
        builder.configureStates().withStates().initial("AWAITING_PAYMENT")
                .states(Set.of("AWAITING_PAYMENT", "PAID"));
        builder.configureTransitions().withExternal()
                .source("AWAITING_PAYMENT")
                .target("PAID")
                .event("PAYMENT_CONFIRMED");
        return builder.build();
    }

    private BusinessStateMachineContextExecutor tenantContextExecutor() {
        return new BusinessStateMachineContextExecutor() {
            @Override
            public <T> T execute(BusinessStateMachineEvent event, Supplier<T> action) {
                Long previousTenant = CURRENT_TENANT.get();
                CURRENT_TENANT.set(event.getTenantId());
                try {
                    return action.get();
                } finally {
                    if (previousTenant == null) {
                        CURRENT_TENANT.remove();
                    } else {
                        CURRENT_TENANT.set(previousTenant);
                    }
                }
            }
        };
    }

    private TransactionalOutbox transactionalOutbox(DeliveryHandlerRegistry registry) {
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        return new DefaultTransactionalOutbox(
                new OutboxMessageValidator(objectMapper, 1_048_576, 64, 16_384, validationUtils),
                new JacksonOutboxMessageSerializer(objectMapper),
                new UuidOutboxIdGenerator(),
                objectMapper,
                new OutboxTransactionGuard(dataSource),
                outboxStore(),
                registry,
                new OutboxAfterCommitBuffer((ApplicationEventPublisher) event -> { }, new NoopOutboxMetrics()),
                new NoopOutboxMetrics(),
                properties
        );
    }

    private OutboxDispatcher dispatcher(
            OutboxStore store,
            DeliveryHandlerRegistry registry,
            boolean failNextSuccess
    ) {
        TransactionalOutboxProperties properties = new TransactionalOutboxProperties();
        properties.getPolling().setBatchSize(10);
        properties.getPolling().setConcurrency(1);
        properties.getDelivery().setQueueCapacity(10);
        properties.getDelivery().setLeaseDuration(Duration.ofSeconds(30));
        OutboxStore dispatchStore = store;
        if (failNextSuccess) {
            dispatchStore = failingSuccessStore(store, new AtomicBoolean(true));
        }
        return new OutboxDispatcher(
                dispatchStore,
                registry,
                new DefaultDeliveryFailureClassifier(),
                attempt -> Duration.ofMillis(10),
                new OutboxDeadLetterNotifier(List.of()),
                new NoopOutboxMetrics(),
                new OutboxWorkerIdentity("business-state-machine-test"),
                Runnable::run,
                properties,
                context.getBean("outboxStateMachineClock", Clock.class),
                context.getBean(OutboxLifecycleService.class),
                context.getBean("outboxMpStorageProperties", OutboxMpStorageProperties.class)
        );
    }

    private OutboxStore failingSuccessStore(OutboxStore delegate, AtomicBoolean failNextSuccess) {
        return (OutboxStore) Proxy.newProxyInstance(
                OutboxStore.class.getClassLoader(),
                new Class<?>[]{OutboxStore.class},
                (proxy, method, args) -> {
                    if ("markSucceeded".equals(method.getName()) && failNextSuccess.compareAndSet(true, false)) {
                        return false;
                    }
                    try {
                        return method.invoke(delegate, args);
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                }
        );
    }

    private OutboxMessage message(String messageId, String idempotencyKey, BusinessStateMachineEvent event) {
        return OutboxMessage.builder()
                .messageId(messageId)
                .idempotencyKey(idempotencyKey)
                .channel(BusinessStateMachineDeliveryHandler.CHANNEL)
                .destination(DESTINATION)
                .payload(event)
                .schemaVersion("1")
                .headers(headers(event))
                .traceId("business-state-machine-integration")
                .availableAt(Instant.now(Clock.systemUTC()).minusSeconds(1))
                .build();
    }

    private DeliveryContext deliveryContext(String messageId, BusinessStateMachineEvent event)
            throws JsonProcessingException {
        return new DeliveryContext(
                messageId,
                BusinessStateMachineDeliveryHandler.CHANNEL,
                DESTINATION,
                objectMapper.writeValueAsString(event),
                "application/json",
                "1",
                headers(event),
                "business-state-machine-integration",
                1,
                10,
                Instant.now(Clock.systemUTC()).plusSeconds(30)
        );
    }

    private Map<String, String> headers(BusinessStateMachineEvent event) {
        return Map.of(
                "egon-event-id", event.getEventId(),
                "egon-tenant-id", event.getTenantId().toString(),
                "egon-machine-key", event.getMachineKey(),
                "egon-definition-version", event.getDefinitionVersion().toString()
        );
    }

    private BusinessStateMachineEvent event(String eventId) {
        return BusinessStateMachineEvent.builder()
                .eventId(eventId)
                .tenantId(7L)
                .machineKey("order-payment")
                .definitionVersion(1)
                .businessId("order-1")
                .eventType("PAYMENT_CONFIRMED")
                .expectedVersion(1L)
                .occurredAt(Instant.now(Clock.systemUTC()))
                .payload(objectMapper.createObjectNode().put("paymentReference", "payment-1"))
                .build();
    }

    private OutboxStatus status(String messageId) {
        return OutboxStatus.valueOf(jdbcTemplate.queryForObject("""
                SELECT status FROM egon_outbox.egon_cola_outbox_message WHERE message_id = ?
                """, String.class, messageId));
    }

    private String businessState() {
        return jdbcTemplate.queryForObject(
                "SELECT state FROM public.outbox_test_order WHERE id = 1", String.class);
    }

    private long businessVersion() {
        return jdbcTemplate.queryForObject(
                "SELECT version FROM public.outbox_test_order WHERE id = 1", Long.class);
    }

    private int receiptCount() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM public.outbox_test_order AS order_row "
                        + "CROSS JOIN LATERAL jsonb_object_keys(order_row.applied_receipts) AS receipt(key) "
                        + "WHERE order_row.id = 1",
                Integer.class);
    }

    private int attemptCount(String messageId) {
        return jdbcTemplate.queryForObject("""
                SELECT attempt_count FROM egon_outbox.egon_cola_outbox_message WHERE message_id = ?
                """, Integer.class, messageId);
    }

    private static final class JdbcBusinessStateMachineRepository implements BusinessStateMachineRepository {

        private final JdbcTemplate jdbcTemplate;
        private final ObjectMapper objectMapper;
        private final AtomicBoolean failAfterWrite = new AtomicBoolean();

        private JdbcBusinessStateMachineRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
            this.jdbcTemplate = jdbcTemplate;
            this.objectMapper = objectMapper;
        }

        private void failAfterWrite(boolean enabled) {
            failAfterWrite.set(enabled);
        }

        @Override
        public BusinessStateMachineSnapshotBO lockAndLoad(BusinessStateMachineEvent event, String fingerprint) {
            requireTenant(event);
            return jdbcTemplate.query("""
                    SELECT state, version, definition_version, applied_receipts ->> ? AS applied_fingerprint, facts::text
                    FROM public.outbox_test_order
                    WHERE id = 1 AND tenant_id = ? AND business_id = ? AND definition_version = ?
                    FOR UPDATE
                    """, resultSet -> {
                if (!resultSet.next()) {
                    return null;
                }
                try {
                    ObjectNode facts = (ObjectNode) objectMapper.readTree(resultSet.getString("facts"));
                    return BusinessStateMachineSnapshotBO.builder()
                            .currentState(resultSet.getString("state"))
                            .version(resultSet.getLong("version"))
                            .definitionVersion(resultSet.getInt("definition_version"))
                            .appliedFingerprint(resultSet.getString("applied_fingerprint"))
                            .facts(facts)
                            .build();
                } catch (JsonProcessingException failure) {
                    throw new IllegalStateException("Business state-machine facts are invalid JSON", failure);
                }
            }, event.getEventId(), event.getTenantId(), event.getBusinessId(), event.getDefinitionVersion());
        }

        @Override
        public void saveTransition(
                BusinessStateMachineEvent event,
                BusinessStateMachineSnapshotBO snapshot,
                String targetState,
                String fingerprint
        ) {
            requireTenant(event);
            int updated = jdbcTemplate.update("""
                    UPDATE public.outbox_test_order
                    SET state = ?, version = version + 1,
                        facts = CAST(? AS jsonb),
                        applied_receipts = COALESCE(applied_receipts, '{}'::jsonb) || jsonb_build_object(?, ?)
                    WHERE id = 1 AND tenant_id = ? AND business_id = ? AND definition_version = ?
                      AND version = ? AND state = ?
                    """,
                    targetState,
                    json(snapshot.getFacts()),
                    event.getEventId(),
                    fingerprint,
                    event.getTenantId(),
                    event.getBusinessId(),
                    event.getDefinitionVersion(),
                    snapshot.getVersion(),
                    snapshot.getCurrentState());
            if (updated != 1) {
                throw new TransientDataAccessResourceException("Business state-machine aggregate changed concurrently");
            }
            if (failAfterWrite.compareAndSet(true, false)) {
                throw new TransientDataAccessResourceException("Injected failure after business state and receipt write");
            }
        }

        private void requireTenant(BusinessStateMachineEvent event) {
            if (!event.getTenantId().equals(CURRENT_TENANT.get())) {
                throw new SecurityException("Business state-machine tenant context does not match the event");
            }
        }

        private String json(ObjectNode facts) {
            try {
                return objectMapper.writeValueAsString(facts);
            } catch (JsonProcessingException failure) {
                throw new IllegalStateException("Business state-machine facts cannot be serialized", failure);
            }
        }
    }
}
