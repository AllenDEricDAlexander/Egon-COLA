package top.egon.cola.component.tianshu.admin.service.ai.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.component.tianshu.admin.config.DdcOutboxConfiguration.DdcAiPublicationOutboxRouteDTO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiPublicationBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiPublicationEventConverter;
import top.egon.cola.component.tianshu.model.event.DdcAiPublicationEvent;
import top.egon.cola.component.tianshu.model.tenant.DdcTenantMessageDTO;

import java.time.Clock;

/**
 * 在发布/回滚/推广的命令事务内入队 EVENT-103；入队失败使整个发布回滚。
 * / Enqueues EVENT-103 inside the publish, rollback or promote command transaction; an enqueue failure rolls the
 * whole publication back.
 */
@Slf4j
@Service("ddcAiPublicationNotifier")
@RequiredArgsConstructor
@Validated
public class DdcAiPublicationNotifier {

    @Qualifier("transactionalOutbox")
    private final TransactionalOutbox transactionalOutbox;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Qualifier("ddcAiPublicationEventConverterImpl")
    private final DdcAiPublicationEventConverter eventConverter;

    @Qualifier("ddcAiPublicationOutboxRoute")
    private final DdcAiPublicationOutboxRouteDTO route;

    @Qualifier("ddcPublishClock")
    private final Clock clock;

    public void enqueue(@NotNull @Valid DdcAiPublicationBO publication, @NotNull DdcAiResourceBO resource,
            @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$") String contentSha256, @NotBlank String actor) {
        Long tenantId = EgonColaTenantIdProvider.currentTenantId();
        DdcAiPublicationEvent event = eventConverter.toEvent(publication, resource, contentSha256, actor,
                clock.instant(), tenantId);
        validationUtils.validate(event);
        String payload;
        try {
            payload = objectMapper.writeValueAsString(new DdcTenantMessageDTO<>(tenantId, event));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("AI_PUBLICATION_EVENT_SERIALIZE_FAILED", failure);
        }
        transactionalOutbox.enqueue(OutboxMessage.builder()
                .messageId(event.getEventId())
                .idempotencyKey(event.getEventId())
                .channel(route.channel())
                .destination(route.destination(tenantId, resource.getBizCode(), resource.getEnvCode(),
                        resource.getAppCode()))
                .payload(payload)
                .contentType("application/json")
                .schemaVersion(route.schemaVersion())
                .header("tenantId", Long.toString(tenantId))
                .header("eventType", route.eventType())
                .build());
        log.info("AI_PUBLICATION_ENQUEUED tenantId={} eventId={} resourceId={} versionNo={}", tenantId,
                event.getEventId(), resource.getId(), event.getVersionNo());
    }
}
