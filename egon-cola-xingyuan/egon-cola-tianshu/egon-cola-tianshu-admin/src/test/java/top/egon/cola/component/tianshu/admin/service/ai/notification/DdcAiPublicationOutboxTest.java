package top.egon.cola.component.tianshu.admin.service.ai.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.slf4j.MDC;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.outbox.common.exception.OutboxValidationException;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandlerRegistry;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.tianshu.admin.config.DdcOutboxConfiguration;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiPublicationBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiPublicationEventConverterImpl;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiPublicationOperationEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.redis.DdcRedisKeys;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DdcAiPublicationOutboxTest {

    private static final Instant NOW = Instant.parse("2026-10-17T05:00:00Z");
    private static final String SHA = "d".repeat(64);

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final ValidationUtils validationUtils = new ValidationUtils(
            Validation.buildDefaultValidatorFactory().getValidator());
    private final TransactionalOutbox outbox = mock(TransactionalOutbox.class);
    private final DdcOutboxConfiguration.DdcAiPublicationOutboxRouteDTO route =
            new DdcOutboxConfiguration().ddcAiPublicationOutboxRoute();
    private final DdcAiPublicationNotifier notifier = new DdcAiPublicationNotifier(outbox, objectMapper,
            validationUtils, new DdcAiPublicationEventConverterImpl(), route, Clock.fixed(NOW, ZoneOffset.UTC));
    private final RedissonClient redisson = mock(RedissonClient.class);
    private final DdcAiPublicationDeliveryHandler handler =
            new DdcAiPublicationDeliveryHandler(redisson, objectMapper, validationUtils);

    @BeforeEach
    void bindTenant() {
        MDC.put("tenantId", "42");
    }

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void enqueuesATenantEnvelopeToTheAiTopicWithAStableEventId() throws Exception {
        notifier.enqueue(publication(), resource(), SHA, "system:ai-optimizer");

        OutboxMessage message = captured();
        assertThat(message.channel()).isEqualTo("tianshu.ai-publication");
        assertThat(message.destination()).isEqualTo(DdcRedisKeys.aiTopic(42L, "xingyuan", "dev", "order"));
        assertThat(message.messageId()).isEqualTo("ai-pub-555");
        assertThat(message.idempotencyKey()).isEqualTo("ai-pub-555");
        assertThat(message.headers()).containsEntry("tenantId", "42").containsEntry("eventType", "TIANSHU_AI_PUBLICATION");
        JsonNode payload = objectMapper.readTree((String) message.payload());
        assertThat(payload.path("tenantId").asLong()).isEqualTo(42L);
        assertThat(payload.at("/payload/versionNo").asText()).isEqualTo("3");
        assertThat(payload.at("/payload/kind").asText()).isEqualTo("SKILL");
        assertThat(payload.at("/payload/operation").asText()).isEqualTo("PUBLISH");
        assertThat(payload.at("/payload/actor").asText()).isEqualTo("system:ai-optimizer");
        assertThat(payload.at("/payload/occurredAt").asText()).isEqualTo("2026-10-17T05:00:00Z");
    }

    @Test
    void enqueueFailureIsNotSwallowed() {
        doThrow(new IllegalStateException("outbox down")).when(outbox).enqueue(org.mockito.ArgumentMatchers.any());

        assertThatThrownBy(() -> notifier.enqueue(publication(), resource(), SHA, "user-1"))
                .isInstanceOf(IllegalStateException.class).hasMessage("outbox down");
    }

    @Test
    void deliveryPublishesThePayloadUnchanged() throws Exception {
        notifier.enqueue(publication(), resource(), SHA, "user-1");
        OutboxMessage message = captured();
        RTopic topic = mock(RTopic.class);
        when(redisson.getTopic(message.destination(), StringCodec.INSTANCE)).thenReturn(topic);

        DeliveryResult result = handler.deliver(context(message, message.destination()));

        assertThat(result).isEqualTo(DeliveryResult.success());
        verify(topic).publish((String) message.payload());
        assertThat(new DeliveryHandlerRegistry(List.of(handler)).required("tianshu.ai-publication")).isSameAs(handler);
    }

    @Test
    void deliveryRejectsForeignDestinationsAndRetriesRedisFailures() throws Exception {
        notifier.enqueue(publication(), resource(), SHA, "user-1");
        OutboxMessage message = captured();

        assertThatThrownBy(() -> handler.validateDestination(
                DdcRedisKeys.topic(42L, "xingyuan", "dev", "order"))).isInstanceOf(OutboxValidationException.class);
        DeliveryResult foreign = handler.deliver(context(message, DdcRedisKeys.aiTopic(43L, "xingyuan", "dev", "order")));
        assertThat(foreign.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        DeliveryResult garbage = handler.deliver(new DeliveryContext("ai-pub-555", "tianshu.ai-publication",
                message.destination(), "{not json", "application/json", "1", message.headers(), null, 1, 10, NOW));
        assertThat(garbage.kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        verifyNoInteractions(redisson);

        RTopic topic = mock(RTopic.class);
        when(redisson.getTopic(message.destination(), StringCodec.INSTANCE)).thenReturn(topic);
        when(topic.publish(anyString())).thenThrow(new IllegalStateException("redis down"));
        assertThat(handler.deliver(context(message, message.destination())).kind())
                .isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
    }

    private OutboxMessage captured() {
        ArgumentCaptor<OutboxMessage> message = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outbox).enqueue(message.capture());
        return message.getValue();
    }

    private static DeliveryContext context(OutboxMessage message, String destination) {
        return new DeliveryContext(message.messageId(), message.channel(), destination, (String) message.payload(),
                message.contentType(), message.schemaVersion(), Map.copyOf(message.headers()), null, 1, 10,
                NOW.plusSeconds(10));
    }

    private static DdcAiPublicationBO publication() {
        return DdcAiPublicationBO.builder().id(555L).resourceId(7L).operation(DdcAiPublicationOperationEnum.PUBLISH)
                .targetVersionId(9L).targetVersionNo(3L).resultRevision(8L).build();
    }

    private static DdcAiResourceBO resource() {
        return DdcAiResourceBO.builder().id(7L).kind(DdcAiKindEnum.SKILL).bizCode("xingyuan").namespaceCode("default")
                .envCode("dev").appCode("order").resourceKey("refund-skill").build();
    }
}
