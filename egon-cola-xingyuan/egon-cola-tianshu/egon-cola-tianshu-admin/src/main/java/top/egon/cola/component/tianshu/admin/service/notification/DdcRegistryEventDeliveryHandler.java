package top.egon.cola.component.tianshu.admin.service.notification;

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
import top.egon.cola.component.tianshu.model.event.DdcRegistryRevisionEvent;
import top.egon.cola.component.tianshu.model.registry.DdcRegistryEvent;
import top.egon.cola.component.tianshu.model.registry.DdcServiceKey;
import top.egon.cola.component.tianshu.model.tenant.DdcTenantMessageDTO;
import top.egon.cola.component.tianshu.redis.DdcRedisKeys;

import java.util.Objects;
import java.util.regex.Pattern;

/** Delivers frozen tenant-scoped registry notifications through the existing StringCodec topic. */
@Slf4j
@Component("ddcRegistryEventDeliveryHandler")
@RequiredArgsConstructor
public class DdcRegistryEventDeliveryHandler implements DeliveryHandler {

    private static final String CHANNEL = DdcNotificationPublisher.CHANNEL;
    private static final String EVENT_TYPE = DdcNotificationPublisher.EVENT_TYPE;
    private static final String SCHEMA_VERSION = DdcNotificationPublisher.SCHEMA_VERSION;
    private static final TypeReference<DdcTenantMessageDTO<DdcRegistryRevisionEvent>> ENVELOPE_TYPE =
            new TypeReference<>() { };
    private static final Pattern DESTINATION_PATTERN = Pattern.compile(
            "^tianshu:v4:tenant:[1-9][0-9]*:\\{[a-f0-9]{64}}:registry:topic:[^\\r\\n]+$");

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
        if (destination == null || !DESTINATION_PATTERN.matcher(destination).matches()) {
            throw new OutboxValidationException("Invalid Tianshu registry event destination");
        }
    }

    @Override
    public DeliveryResult deliver(DeliveryContext context) {
        DdcTenantMessageDTO<DdcRegistryRevisionEvent> envelope;
        try {
            envelope = parseAndValidate(context);
        } catch (JsonProcessingException | ConstraintViolationException
                 | IllegalArgumentException | OutboxValidationException invalid) {
            log.warn("Rejecting invalid Tianshu registry outbox record: messageId={}",
                    context == null ? null : context.messageId());
            return DeliveryResult.permanentFailure("TIANSHU_REGISTRY_EVENT_INVALID", "invalid registry event");
        }

        try {
            redissonClient.getTopic(context.destination(), StringCodec.INSTANCE).publish(context.payload());
            return DeliveryResult.success();
        } catch (RuntimeException transientFailure) {
            log.warn("Tianshu registry topic delivery failed and will be retried: tenantId={}, eventId={}",
                    envelope.getTenantId(), envelope.getPayload().getEventId(), transientFailure);
            return DeliveryResult.retryableFailure("TIANSHU_REGISTRY_DELIVERY_RETRY", "registry topic publish failed");
        }
    }

    private DdcTenantMessageDTO<DdcRegistryRevisionEvent> parseAndValidate(DeliveryContext context)
            throws JsonProcessingException {
        if (context == null
                || !Objects.equals(context.channel(), CHANNEL)
                || !Objects.equals(context.contentType(), "application/json")
                || !Objects.equals(context.schemaVersion(), SCHEMA_VERSION)) {
            throw new IllegalArgumentException("registry outbox envelope does not match the route");
        }
        validateDestination(context.destination());
        DdcTenantMessageDTO<DdcRegistryRevisionEvent> envelope =
                objectMapper.readValue(context.payload(), ENVELOPE_TYPE);
        if (envelope == null || envelope.getPayload() == null) {
            throw new IllegalArgumentException("registry event payload is empty");
        }
        validationUtils.validate(envelope);
        DdcRegistryRevisionEvent event = envelope.getPayload();
        if (!event.hasValidIdentity()
                || !Objects.equals(event.getTenantId(), envelope.getTenantId())
                || !Objects.equals(context.messageId(), event.getEventId())
                || !Objects.equals(context.headers().get("tenantId"), Long.toString(event.getTenantId()))
                || !Objects.equals(context.headers().get("eventType"), EVENT_TYPE)
                || !destinationMatches(event, context.destination())) {
            throw new IllegalArgumentException("registry event identity or destination is invalid");
        }
        return envelope;
    }

    private boolean destinationMatches(DdcRegistryRevisionEvent event, String destination) {
        DdcRegistryEvent registryEvent = event.getEvent();
        DdcServiceKey serviceKey = registryEvent.serviceKey();
        return destination.equals(DdcRedisKeys.registryTopic(
                event.getTenantId(),
                serviceKey.bizCode(), serviceKey.env(), serviceKey.appCode(),
                serviceKey.serviceKind(), serviceKey.protocol()));
    }
}
