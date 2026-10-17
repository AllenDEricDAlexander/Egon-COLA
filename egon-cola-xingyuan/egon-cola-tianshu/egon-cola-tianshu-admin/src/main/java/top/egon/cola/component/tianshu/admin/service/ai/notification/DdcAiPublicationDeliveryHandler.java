package top.egon.cola.component.tianshu.admin.service.ai.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.common.exception.OutboxValidationException;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.tianshu.admin.config.DdcOutboxConfiguration.DdcAiPublicationOutboxRouteDTO;
import top.egon.cola.component.tianshu.model.event.DdcAiPublicationEvent;
import top.egon.cola.component.tianshu.model.tenant.DdcTenantMessageDTO;
import top.egon.cola.component.tianshu.redis.DdcRedisKeys;

import java.util.Objects;

/**
 * 把冻结的 EVENT-103 信封原样发布到租户 AI Topic。
 * / Publishes the frozen EVENT-103 envelope unchanged to the tenant AI topic.
 */
@Slf4j
@Component("ddcAiPublicationDeliveryHandler")
@RequiredArgsConstructor
public class DdcAiPublicationDeliveryHandler implements DeliveryHandler {

    static final String CHANNEL = "tianshu.ai-publication";
    static final String SCHEMA_VERSION = "1";
    static final String EVENT_TYPE = "TIANSHU_AI_PUBLICATION";
    private static final TypeReference<DdcTenantMessageDTO<DdcAiPublicationEvent>> ENVELOPE_TYPE =
            new TypeReference<>() { };

    @Qualifier("ddcAdminRedissonClient")
    private final RedissonClient redissonClient;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public void validateDestination(String destination) {
        if (destination == null || !DdcAiPublicationOutboxRouteDTO.TOPIC_PATTERN.matcher(destination).matches()) {
            throw new OutboxValidationException("Invalid Tianshu ai-publication destination");
        }
    }

    @Override
    public DeliveryResult deliver(DeliveryContext context) {
        DdcTenantMessageDTO<DdcAiPublicationEvent> envelope;
        try {
            envelope = parseAndValidate(context);
        } catch (JsonProcessingException | ConstraintViolationException
                 | IllegalArgumentException | OutboxValidationException invalid) {
            log.warn("Rejecting invalid Tianshu ai-publication outbox record: messageId={}",
                    context == null ? null : context.messageId());
            return DeliveryResult.permanentFailure("TIANSHU_AI_PUBLICATION_EVENT_INVALID",
                    "invalid ai publication event");
        }
        try {
            redissonClient.getTopic(context.destination(), StringCodec.INSTANCE).publish(context.payload());
            return DeliveryResult.success();
        } catch (RuntimeException transientFailure) {
            log.warn("Tianshu ai-publication delivery failed and will be retried: tenantId={}, eventId={}",
                    envelope.getTenantId(), envelope.getPayload().getEventId(), transientFailure);
            return DeliveryResult.retryableFailure("TIANSHU_AI_PUBLICATION_DELIVERY_RETRY",
                    "ai topic publish failed");
        }
    }

    private DdcTenantMessageDTO<DdcAiPublicationEvent> parseAndValidate(DeliveryContext context)
            throws JsonProcessingException {
        if (context == null
                || !Objects.equals(context.channel(), CHANNEL)
                || !Objects.equals(context.contentType(), "application/json")
                || !Objects.equals(context.schemaVersion(), SCHEMA_VERSION)) {
            throw new IllegalArgumentException("ai publication outbox envelope does not match the route");
        }
        validateDestination(context.destination());
        DdcTenantMessageDTO<DdcAiPublicationEvent> envelope = objectMapper.readValue(context.payload(), ENVELOPE_TYPE);
        if (envelope == null || envelope.getPayload() == null) {
            throw new IllegalArgumentException("ai publication payload is empty");
        }
        validationUtils.validate(envelope);
        DdcAiPublicationEvent event = envelope.getPayload();
        if (!event.hasValidIdentity()
                || !Objects.equals(event.getTenantId(), envelope.getTenantId())
                || !Objects.equals(context.messageId(), event.getEventId())
                || !Objects.equals(context.headers().get("tenantId"), Long.toString(event.getTenantId()))
                || !Objects.equals(context.headers().get("eventType"), EVENT_TYPE)
                || !context.destination().equals(DdcRedisKeys.aiTopic(event.getTenantId(), event.getBizCode(),
                        event.getEnv(), event.getAppCode()))) {
            throw new IllegalArgumentException("ai publication identity or destination is invalid");
        }
        return envelope;
    }
}
