package top.egon.cola.component.tianshu.admin.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcRegistryRevisionEventConverter;
import top.egon.cola.component.tianshu.model.event.DdcRegistryRevisionEvent;
import top.egon.cola.component.tianshu.model.registry.DdcRegistryEvent;
import top.egon.cola.component.tianshu.model.tenant.DdcTenantMessageDTO;
import top.egon.cola.component.tianshu.redis.DdcRedisKeys;

/** Freezes registry revisions and enqueues them after the authoritative Redis mutation. */
@Slf4j
@Service("ddcNotificationPublisher")
@RequiredArgsConstructor
@Validated
public class DdcNotificationPublisher {

    public static final String CHANNEL = "tianshu.registry-event";
    public static final String EVENT_TYPE = "TIANSHU_REGISTRY_EVENT";
    public static final String SCHEMA_VERSION = "1";

    @Qualifier("transactionalOutbox")
    private final TransactionalOutbox transactionalOutbox;

    @Qualifier("ddcPublishTransactionTemplate")
    private final TransactionTemplate transactionTemplate;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Qualifier("ddcRegistryRevisionEventConverterImpl")
    private final DdcRegistryRevisionEventConverter eventConverter;

    public void publish(@NotNull @Positive Long tenantId, @Valid @NotNull DdcRegistryEvent sourceEvent) {
        validationUtils.validate(sourceEvent);
        DdcRegistryRevisionEvent event = eventConverter.toTarget(sourceEvent, tenantId);
        validationUtils.validate(event);
        String destination = DdcRedisKeys.registryTopic(
                tenantId,
                sourceEvent.serviceKey().bizCode(),
                sourceEvent.serviceKey().env(),
                sourceEvent.serviceKey().appCode(),
                sourceEvent.serviceKey().serviceKind(),
                sourceEvent.serviceKey().protocol());

        try {
            String payload = objectMapper.writeValueAsString(new DdcTenantMessageDTO<>(tenantId, event));
            OutboxMessage outboxMessage = OutboxMessage.builder()
                    .messageId(event.getEventId())
                    .idempotencyKey(event.getEventId())
                    .channel(CHANNEL)
                    .destination(destination)
                    .payload(payload)
                    .contentType("application/json")
                    .schemaVersion(SCHEMA_VERSION)
                    .header("tenantId", Long.toString(tenantId))
                    .header("eventType", EVENT_TYPE)
                    .build();
            transactionTemplate.executeWithoutResult(status -> transactionalOutbox.enqueue(outboxMessage));
        } catch (JsonProcessingException | RuntimeException failure) {
            log.warn("Tianshu registry state was updated but its Outbox notification could not be enqueued; "
                            + "tenantId={}, eventId={}, serviceRevision={}, catalogRevision={}",
                    tenantId, event.getEventId(), sourceEvent.serviceRevision(), sourceEvent.catalogRevision(),
                    failure);
        }
    }
}
