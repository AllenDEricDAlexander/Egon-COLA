package top.egon.cola.component.tianshu.admin.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandlerRegistry;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcRegistryRevisionEventConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcRegistryRevisionEventConverterImpl;
import top.egon.cola.component.tianshu.model.event.DdcRegistryRevisionEvent;
import top.egon.cola.component.tianshu.model.registry.DdcRegistryEvent;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKey;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKind;
import top.egon.cola.component.tianshu.model.tenant.DdcTenantMessageDTO;
import top.egon.cola.component.tianshu.redis.DdcRedisKeys;

import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcRegistryOutboxTest {

    private final ValidationUtils validationUtils = new ValidationUtils(
            Validation.buildDefaultValidatorFactory().getValidator());

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DdcRegistryRevisionEventConverter eventConverter = new DdcRegistryRevisionEventConverterImpl();

    @Test
    void registryRevisionEventIdentityIsStableAndTenantScoped() throws Exception {
        DdcRegistryEvent firstRevision = event(7L, 3L);
        String firstId = DdcRegistryRevisionEvent.eventId(101L, firstRevision);
        DdcRegistryRevisionEvent prepared = eventConverter.toTarget(firstRevision, 101L);

        assertThat(firstId).matches("^[a-f0-9]{64}$");
        assertThat(DdcRegistryRevisionEvent.eventId(101L, firstRevision)).isEqualTo(firstId);
        assertThat(DdcRegistryRevisionEvent.eventId(102L, firstRevision)).isNotEqualTo(firstId);
        assertThat(DdcRegistryRevisionEvent.eventId(101L, event(8L, 3L))).isNotEqualTo(firstId);
        assertThat(DdcRegistryRevisionEvent.eventId(101L, event(7L, 4L))).isNotEqualTo(firstId);
        assertThat(prepared.hasValidIdentity()).isTrue();
        assertThat(new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(prepared))
                .path("tenantId").asText()).isEqualTo("101");
    }

    @Test
    void nestedRegistryRevisionConstraintsRejectInvalidEvent() {
        DdcRegistryRevisionEvent invalid = DdcRegistryRevisionEvent.builder()
                .eventId("0".repeat(64)).tenantId(101L).schemaVersion("1")
                .event(event(-1L, 3L)).build();

        assertThatThrownBy(() -> validationUtils.validate(invalid))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void registryDeliveryUsesTheTransactionalOutboxHandlerContract() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        DdcRegistryEventDeliveryHandler handler = new DdcRegistryEventDeliveryHandler(
                redisson, objectMapper, validationUtils);

        assertThat(handler.channel()).isEqualTo("tianshu.registry-event");
        assertThat(new DeliveryHandlerRegistry(List.of(handler)).required(handler.channel())).isSameAs(handler);
    }

    @Test
    void registryWriterNotificationUsesTheNamedOutboxContract() {
        assertThat(DdcNotificationPublisher.class.getAnnotation(org.springframework.stereotype.Service.class).value())
                .isEqualTo("ddcNotificationPublisher");
        assertThat(DdcNotificationPublisher.CHANNEL).isEqualTo("tianshu.registry-event");
        assertThat(DdcNotificationPublisher.EVENT_TYPE).isEqualTo("TIANSHU_REGISTRY_EVENT");
    }

    @Test
    void publisherFreezesTheTenantEnvelopeAndEnqueuesInsideTheSelectedTransaction() throws Exception {
        TransactionalOutbox outbox = mock(TransactionalOutbox.class);
        List<OutboxMessage> captured = new ArrayList<>();
        doAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            captured.add(invocation.getArgument(0));
            return null;
        }).when(outbox).enqueue(org.mockito.ArgumentMatchers.any(OutboxMessage.class));
        DdcNotificationPublisher publisher = publisher(outbox);
        DdcRegistryEvent sourceEvent = event(7L, 3L);

        publisher.publish(101L, sourceEvent);
        publisher.publish(101L, sourceEvent);

        assertThat(captured).hasSize(2);
        OutboxMessage first = captured.get(0);
        OutboxMessage retry = captured.get(1);
        assertThat(first.channel()).isEqualTo("tianshu.registry-event");
        assertThat(first.destination()).isEqualTo(destination(101L));
        assertThat(first.messageId()).isEqualTo(first.idempotencyKey());
        assertThat(first.messageId()).isEqualTo(retry.messageId());
        assertThat(first.payload()).isEqualTo(retry.payload());
        assertThat(first.headers()).containsEntry("tenantId", "101")
                .containsEntry("eventType", "TIANSHU_REGISTRY_EVENT");
        var json = objectMapper.readTree(first.payload().toString());
        assertThat(json.path("tenantId").asLong()).isEqualTo(101L);
        assertThat(json.path("payload").path("tenantId").asText()).isEqualTo("101");
        assertThat(json.path("payload").path("event").path("serviceRevision").asLong()).isEqualTo(7L);
    }

    @Test
    void enqueueFailureLeavesTheRedisAuthorityWindowObservableAndDoesNotPretendRollback() {
        TransactionalOutbox outbox = mock(TransactionalOutbox.class);
        doThrow(new IllegalStateException("outbox unavailable"))
                .when(outbox).enqueue(org.mockito.ArgumentMatchers.any(OutboxMessage.class));
        DdcNotificationPublisher publisher = publisher(outbox);

        assertThatCode(() -> publisher.publish(101L, event(7L, 3L))).doesNotThrowAnyException();
    }

    @Test
    void handlerPublishesTheFrozenTenantEnvelopeAndDuplicateDeliveryIsSafe() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        DdcRegistryRevisionEvent event = prepared(101L, event(7L, 3L));
        String destination = destination(101L);
        String payload = payload(101L, event);
        when(redisson.getTopic(destination, StringCodec.INSTANCE)).thenReturn(topic);
        DdcRegistryEventDeliveryHandler handler = new DdcRegistryEventDeliveryHandler(
                redisson, objectMapper, validationUtils);

        DeliveryResult first = handler.deliver(context(event.getEventId(), destination, payload, "101"));
        DeliveryResult retry = handler.deliver(context(event.getEventId(), destination, payload, "101"));

        assertThat(first.kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
        assertThat(retry.kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
        verify(topic, times(2)).publish(payload);
    }

    @Test
    void handlerPermanentlyRejectsTenantOrDestinationMismatch() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        DdcRegistryRevisionEvent event = prepared(101L, event(7L, 3L));
        String payload = payload(101L, event);
        DdcRegistryEventDeliveryHandler handler = new DdcRegistryEventDeliveryHandler(
                redisson, objectMapper, validationUtils);

        DeliveryResult wrongTenant = handler.deliver(
                context(event.getEventId(), destination(101L), payload, "202"));
        DeliveryResult wrongDestination = handler.deliver(
                context(event.getEventId(), destination(202L), payload, "101"));
        DdcRegistryRevisionEvent tampered = prepared(101L, event(7L, 3L));
        tampered.setEventId("0".repeat(64));
        DeliveryResult wrongIdentity = handler.deliver(
                context(tampered.getEventId(), destination(101L), payload(101L, tampered), "101"));
        DdcRegistryRevisionEvent wrongSchema = prepared(101L, event(7L, 3L));
        wrongSchema.setSchemaVersion("2");
        DeliveryResult unsupportedSchema = handler.deliver(
                context(wrongSchema.getEventId(), destination(101L), payload(101L, wrongSchema), "101"));

        assertThat(wrongTenant.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        assertThat(wrongDestination.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        assertThat(wrongIdentity.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        assertThat(unsupportedSchema.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        verify(redisson, never()).getTopic(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(StringCodec.INSTANCE));
    }

    @Test
    void handlerMarksTemporaryRedisFailureRetryable() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        DdcRegistryRevisionEvent event = prepared(101L, event(7L, 3L));
        String destination = destination(101L);
        when(redisson.getTopic(destination, StringCodec.INSTANCE)).thenReturn(topic);
        doThrow(new IllegalStateException("redis unavailable")).when(topic).publish(org.mockito.ArgumentMatchers.anyString());
        DdcRegistryEventDeliveryHandler handler = new DdcRegistryEventDeliveryHandler(
                redisson, objectMapper, validationUtils);

        DeliveryResult result = handler.deliver(
                context(event.getEventId(), destination, payload(101L, event), "101"));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
    }

    private DdcRegistryEvent event(long serviceRevision, long catalogRevision) {
        DdcServiceKey serviceKey = new DdcServiceKey(
                "retail", "dev", "orders", DdcServiceKind.RPC_PROVIDER,
                "OrderService", "default", "1.0.0", "grpc");
        return new DdcRegistryEvent(serviceKey, serviceRevision, catalogRevision);
    }

    private DdcRegistryRevisionEvent prepared(long tenantId, DdcRegistryEvent event) {
        return eventConverter.toTarget(event, tenantId);
    }

    private String destination(long tenantId) {
        return DdcRedisKeys.registryTopic(
                tenantId, "retail", "dev", "orders", DdcServiceKind.RPC_PROVIDER, "grpc");
    }

    private String payload(long tenantId, DdcRegistryRevisionEvent event) throws Exception {
        return objectMapper.writeValueAsString(new DdcTenantMessageDTO<>(tenantId, event));
    }

    private DeliveryContext context(String eventId, String destination, String payload, String tenantHeader) {
        return new DeliveryContext(
                eventId, "tianshu.registry-event", destination, payload,
                "application/json", "1", Map.of("tenantId", tenantHeader,
                "eventType", "TIANSHU_REGISTRY_EVENT"), null, 0, 5,
                Instant.parse("2026-10-05T00:00:00Z"));
    }

    private DdcNotificationPublisher publisher(TransactionalOutbox outbox) {
        return new DdcNotificationPublisher(
                outbox,
                new TransactionTemplate(new InMemoryTransactionManager()),
                objectMapper,
                validationUtils,
                eventConverter);
    }

    private static final class InMemoryTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
