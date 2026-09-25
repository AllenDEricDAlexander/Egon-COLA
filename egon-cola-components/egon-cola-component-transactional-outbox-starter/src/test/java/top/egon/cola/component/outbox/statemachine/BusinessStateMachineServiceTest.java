package top.egon.cola.component.outbox.statemachine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineBuilder;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.autoconfigure.OutboxStateMachineProperties;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessStateMachineServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-25T00:00:00Z");
    private static final String DESTINATION = "order-payment:1";
    private static final ThreadLocal<String> TEST_TENANT_CONTEXT = new ThreadLocal<>();
    private static ValidatorFactory validatorFactory;
    private static ValidationUtils validationUtils;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validationUtils = new ValidationUtils(validatorFactory.getValidator());
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void shouldApplyRealStateMachineTransitionBeforeReturningSuccess() throws Exception {
        BusinessStateMachineRepository repository = mock(BusinessStateMachineRepository.class);
        StateMachineFactory<String, String> factory = factory();
        BusinessStateMachineEvent event = event("event-1", 3L, "PAYMENT_CONFIRMED");
        BusinessStateMachineSnapshotBO snapshot = snapshot("AWAITING_PAYMENT", 3L, null);
        when(repository.lockAndLoad(any(BusinessStateMachineEvent.class), anyString())).thenAnswer(invocation -> {
            assertThat(TEST_TENANT_CONTEXT.get()).isEqualTo(event.getTenantId().toString());
            return snapshot;
        });
        BusinessStateMachineService service = service(repository, factory);
        TEST_TENANT_CONTEXT.set("caller-tenant");
        try {
            DeliveryResult result = service.process(event, context(event, "transport-1"));
            assertThat(result).isEqualTo(DeliveryResult.success());
            assertThat(TEST_TENANT_CONTEXT.get()).isEqualTo("caller-tenant");
        } finally {
            TEST_TENANT_CONTEXT.remove();
        }
        verify(repository).saveTransition(any(BusinessStateMachineEvent.class), any(BusinessStateMachineSnapshotBO.class),
                org.mockito.ArgumentMatchers.eq("PAID"), anyString());
        verify(repository).lockAndLoad(any(BusinessStateMachineEvent.class), anyString());
    }

    @Test
    void shouldShortCircuitMatchingReceiptBeforeVersionValidationOrMachineCreation() throws Exception {
        BusinessStateMachineRepository repository = mock(BusinessStateMachineRepository.class);
        StateMachineFactory<String, String> factory = factory();
        BusinessStateMachineEvent event = event("event-1", 2L, "PAYMENT_CONFIRMED");
        when(repository.lockAndLoad(any(BusinessStateMachineEvent.class), anyString()))
                .thenAnswer(invocation -> snapshot("PAID", 4L, invocation.getArgument(1)));
        BusinessStateMachineService service = service(repository, factory);

        DeliveryResult result = service.process(event, context(event, "transport-retry"));

        assertThat(result).isEqualTo(DeliveryResult.success());
        verify(repository, never()).saveTransition(any(), any(), anyString(), anyString());
        verify(factory, never()).getStateMachine();
    }

    @Test
    void shouldClassifyFutureExpectedVersionAsRetryable() throws Exception {
        BusinessStateMachineRepository repository = mock(BusinessStateMachineRepository.class);
        StateMachineFactory<String, String> factory = factory();
        BusinessStateMachineEvent event = event("event-1", 5L, "PAYMENT_CONFIRMED");
        when(repository.lockAndLoad(any(BusinessStateMachineEvent.class), anyString()))
                .thenReturn(snapshot("AWAITING_PAYMENT", 4L, null));
        BusinessStateMachineService service = service(repository, factory);

        DeliveryResult result = service.process(event, context(event, "transport-1"));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
        assertThat(result.code()).isEqualTo("OUTBOX_SM_VERSION_GAP");
        verify(repository, never()).saveTransition(any(), any(), anyString(), anyString());
        verify(factory, never()).getStateMachine();
    }

    @Test
    void shouldRejectTransportIdentityMismatchBeforeRepositoryAccess() throws Exception {
        BusinessStateMachineRepository repository = mock(BusinessStateMachineRepository.class);
        BusinessStateMachineEvent event = event("event-1", 3L, "PAYMENT_CONFIRMED");
        DeliveryContext validContext = context(event, "transport-1");
        DeliveryContext mismatchedContext = new DeliveryContext(
                validContext.messageId(),
                validContext.channel(),
                validContext.destination(),
                validContext.payload(),
                validContext.contentType(),
                validContext.schemaVersion(),
                Map.of(
                        "egon-event-id", "other-event",
                        "egon-tenant-id", event.getTenantId().toString(),
                        "egon-machine-key", event.getMachineKey(),
                        "egon-definition-version", event.getDefinitionVersion().toString()),
                validContext.traceId(),
                validContext.attempt(),
                validContext.maxAttempts(),
                validContext.deadline());
        BusinessStateMachineService service = service(repository, factory());

        DeliveryResult result = service.process(event, mismatchedContext);

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        assertThat(result.code()).isEqualTo("OUTBOX_SM_EVENT_INVALID");
        verify(repository, never()).lockAndLoad(any(), anyString());
    }

    @Test
    void shouldRejectAChangedPayloadForAnAlreadyAppliedEvent() throws Exception {
        BusinessStateMachineRepository repository = mock(BusinessStateMachineRepository.class);
        BusinessStateMachineEvent event = event("event-1", 3L, "PAYMENT_CONFIRMED");
        when(repository.lockAndLoad(any(BusinessStateMachineEvent.class), anyString()))
                .thenReturn(snapshot("PAID", 4L, "f".repeat(64)));
        BusinessStateMachineService service = service(repository, factory());

        DeliveryResult result = service.process(event, context(event, "transport-retry"));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        assertThat(result.code()).isEqualTo("OUTBOX_SM_EVENT_CONFLICT");
        verify(repository, never()).saveTransition(any(), any(), anyString(), anyString());
    }

    @Test
    void shouldApplyE1AndE2ThenShortCircuitTheE1Replay() throws Exception {
        Map<String, String> receipts = new HashMap<>();
        AtomicReference<String> currentState = new AtomicReference<>("AWAITING_PAYMENT");
        AtomicLong currentVersion = new AtomicLong(1L);
        BusinessStateMachineRepository repository = new BusinessStateMachineRepository() {
            @Override
            public BusinessStateMachineSnapshotBO lockAndLoad(BusinessStateMachineEvent event, String fingerprint) {
                return snapshot(currentState.get(), currentVersion.get(), receipts.get(event.getEventId()));
            }

            @Override
            public void saveTransition(
                    BusinessStateMachineEvent event,
                    BusinessStateMachineSnapshotBO snapshot,
                    String targetState,
                    String fingerprint
            ) {
                assertThat(snapshot.getVersion()).isEqualTo(currentVersion.get());
                currentState.set(targetState);
                currentVersion.incrementAndGet();
                receipts.put(event.getEventId(), fingerprint);
            }
        };
        StateMachineFactory<String, String> factory = factory();
        BusinessStateMachineService service = service(repository, factory);
        BusinessStateMachineEvent first = event("event-1", 1L, "PAYMENT_CONFIRMED");
        BusinessStateMachineEvent second = event("event-2", 2L, "PAYMENT_SETTLED");

        assertThat(service.process(first, context(first, "transport-1"))).isEqualTo(DeliveryResult.success());
        assertThat(service.process(second, context(second, "transport-2"))).isEqualTo(DeliveryResult.success());
        assertThat(service.process(first, context(first, "transport-1-replay"))).isEqualTo(DeliveryResult.success());

        assertThat(currentState.get()).isEqualTo("SETTLED");
        assertThat(currentVersion.get()).isEqualTo(3L);
        assertThat(receipts).containsOnlyKeys("event-1", "event-2");
        verify(factory, times(2)).getStateMachine();
    }

    @Test
    void shouldRejectAStaleExpectedVersionBeforeCreatingAMachine() throws Exception {
        BusinessStateMachineRepository repository = mock(BusinessStateMachineRepository.class);
        StateMachineFactory<String, String> factory = factory();
        BusinessStateMachineEvent event = event("event-1", 2L, "PAYMENT_CONFIRMED");
        when(repository.lockAndLoad(any(BusinessStateMachineEvent.class), anyString()))
                .thenReturn(snapshot("PAID", 3L, null));
        BusinessStateMachineService service = service(repository, factory);

        DeliveryResult result = service.process(event, context(event, "transport-1"));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        assertThat(result.code()).isEqualTo("OUTBOX_SM_STALE_VERSION");
        verify(repository, never()).saveTransition(any(), any(), anyString(), anyString());
        verify(factory, never()).getStateMachine();
    }

    @Test
    void shouldRejectAnEventDeniedByTheRealBusinessStateMachine() throws Exception {
        BusinessStateMachineRepository repository = mock(BusinessStateMachineRepository.class);
        StateMachineFactory<String, String> factory = factory();
        BusinessStateMachineEvent event = event("event-1", 3L, "PAYMENT_REFUNDED");
        when(repository.lockAndLoad(any(BusinessStateMachineEvent.class), anyString()))
                .thenReturn(snapshot("AWAITING_PAYMENT", 3L, null));
        BusinessStateMachineService service = service(repository, factory);

        DeliveryResult result = service.process(event, context(event, "transport-1"));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        assertThat(result.code()).isEqualTo("OUTBOX_SM_TRANSITION_REJECTED");
        verify(repository, never()).saveTransition(any(), any(), anyString(), anyString());
    }

    private static BusinessStateMachineService service(
            BusinessStateMachineRepository repository,
            StateMachineFactory<String, String> factory
    ) {
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
        TransactionTemplate transaction = mock(TransactionTemplate.class);
        when(transaction.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        return new BusinessStateMachineService(
                Map.of(DESTINATION, definition),
                testContextExecutor(),
                transaction,
                new StateMachineExecutionService(validationUtils),
                new OutboxStateMachineProperties(),
                objectMapper(),
                validationUtils,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private static BusinessStateMachineContextExecutor testContextExecutor() {
        return new BusinessStateMachineContextExecutor() {
            @Override
            public <T> T execute(BusinessStateMachineEvent event, Supplier<T> action) {
                String previousTenant = TEST_TENANT_CONTEXT.get();
                TEST_TENANT_CONTEXT.set(event.getTenantId().toString());
                try {
                    return action.get();
                } finally {
                    if (previousTenant == null) {
                        TEST_TENANT_CONTEXT.remove();
                    } else {
                        TEST_TENANT_CONTEXT.set(previousTenant);
                    }
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static StateMachineFactory<String, String> factory() throws Exception {
        StateMachineFactory<String, String> factory = mock(StateMachineFactory.class);
        when(factory.getStateMachine()).thenAnswer(invocation -> paymentMachine());
        return factory;
    }

    private static StateMachine<String, String> paymentMachine() throws Exception {
        StateMachineBuilder.Builder<String, String> builder = StateMachineBuilder.builder();
        builder.configureConfiguration().withConfiguration().autoStartup(false);
        builder.configureStates().withStates().initial("AWAITING_PAYMENT")
                .states(Set.of("AWAITING_PAYMENT", "PAID", "SETTLED"));
        builder.configureTransitions().withExternal()
                .source("AWAITING_PAYMENT")
                .target("PAID")
                .event("PAYMENT_CONFIRMED")
                .and()
                .withExternal()
                .source("PAID")
                .target("SETTLED")
                .event("PAYMENT_SETTLED");
        return builder.build();
    }

    private static BusinessStateMachineEvent event(String eventId, Long expectedVersion, String eventType) {
        return BusinessStateMachineEvent.builder()
                .eventId(eventId)
                .tenantId(7L)
                .machineKey("order-payment")
                .definitionVersion(1)
                .businessId("order-1")
                .eventType(eventType)
                .expectedVersion(expectedVersion)
                .occurredAt(NOW)
                .payload(objectMapper().createObjectNode().put("paymentReference", "pay-1"))
                .build();
    }

    private static DeliveryContext context(BusinessStateMachineEvent event, String messageId) {
        return new DeliveryContext(
                messageId,
                "statemachine",
                DESTINATION,
                "{}",
                "application/json",
                "1",
                Map.of(
                        "egon-event-id", event.getEventId(),
                        "egon-tenant-id", event.getTenantId().toString(),
                        "egon-machine-key", event.getMachineKey(),
                        "egon-definition-version", event.getDefinitionVersion().toString()),
                "trace-1",
                1,
                10,
                NOW.plusSeconds(30)
        );
    }

    private static BusinessStateMachineSnapshotBO snapshot(
            String state,
            Long version,
            String appliedFingerprint
    ) {
        return BusinessStateMachineSnapshotBO.builder()
                .currentState(state)
                .version(version)
                .definitionVersion(1)
                .appliedFingerprint(appliedFingerprint)
                .facts(objectMapper().createObjectNode())
                .build();
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }
}
