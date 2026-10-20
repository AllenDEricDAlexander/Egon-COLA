package top.egon.cola.component.tianshu.admin.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.outbox.common.exception.OutboxValidationException;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.tianshu.admin.common.DdcAdminException;
import top.egon.cola.component.tianshu.admin.config.DdcOutboxConfiguration.DdcConfigDispatchOutboxRouteDTO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcConfigDispatchEventConverter;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantBindingDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextDTO;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantContextExecutor;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantRegistry;
import top.egon.cola.component.tianshu.admin.service.publish.DdcPendingPublishDispatcher;
import top.egon.cola.component.tianshu.model.event.DdcConfigDispatchPreparedEvent;

import java.util.Objects;

@Slf4j
@Component("ddcConfigDispatchDeliveryHandler")
@RequiredArgsConstructor
public class DdcConfigDispatchDeliveryHandler implements DeliveryHandler {

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Qualifier("ddcTenantRegistry")
    private final DdcTenantRegistry tenantRegistry;

    @Qualifier("ddcTenantContextExecutor")
    private final DdcTenantContextExecutor tenantContextExecutor;

    /** Resolves the publisher after Outbox has finished registering its delivery handlers. */
    @Qualifier("ddcPendingPublishDispatcher")
    private final ObjectProvider<DdcPendingPublishDispatcher> dispatchers;

    @Qualifier("ddcConfigDispatchEventConverterImpl")
    private final DdcConfigDispatchEventConverter eventConverter;

    @Qualifier("ddcConfigDispatchOutboxRoute")
    private final DdcConfigDispatchOutboxRouteDTO outboxRoute;

    @Override
    public String channel() {
        return outboxRoute.channel();
    }

    @Override
    public void validateDestination(String destination) {
        outboxRoute.validateDestination(destination);
    }

    @Override
    public DeliveryResult deliver(DeliveryContext context) {
        DdcConfigDispatchPreparedEvent event;
        try {
            event = parseAndValidate(context);
        } catch (JsonProcessingException | ConstraintViolationException
                 | IllegalArgumentException | OutboxValidationException invalid) {
            log.warn("Rejecting invalid Tianshu config dispatch outbox record: messageId={}",
                    context == null ? null : context.messageId());
            return DeliveryResult.permanentFailure("TIANSHU_CONFIG_EVENT_INVALID", "invalid config dispatch event");
        }
        if (event == null) {
            return DeliveryResult.permanentFailure("TIANSHU_CONFIG_EVENT_INVALID", "invalid config dispatch event");
        }

        DdcTenantBindingDTO binding;
        try {
            binding = tenantRegistry.requireEnabledTenant(event.getTenantId());
        } catch (IllegalArgumentException unregisteredTenant) {
            log.warn("Rejecting Tianshu config dispatch for an unregistered tenant: tenantId={}, eventId={}",
                    event.getTenantId(), event.getEventId());
            return DeliveryResult.permanentFailure("TIANSHU_CONFIG_TENANT_INVALID", "tenant is not enabled");
        }
        DdcTenantContextDTO tenantContext = new DdcTenantContextDTO(
                binding.getTenantId(), binding.getIdentityTenantId(), "system:outbox:" + event.getEventId());
        try {
            boolean delivered = tenantContextExecutor.call(
                    tenantContext, () -> dispatchers.getObject().deliverPrepared(event));
            if (!delivered) {
                return DeliveryResult.permanentFailure(
                        "TIANSHU_CONFIG_ATTEMPT_STALE", "publish attempt is no longer current");
            }
            return DeliveryResult.success();
        } catch (DdcAdminException permanentFailure) {
            dispatchers.getObject().failPrepared(event, "REDIS_DISPATCH", permanentFailure.getMessage());
            log.warn("Tianshu config dispatch was rejected by the current publish state: tenantId={}, eventId={}",
                    event.getTenantId(), event.getEventId());
            return DeliveryResult.permanentFailure("TIANSHU_CONFIG_DISPATCH_REJECTED", "publish dispatch rejected");
        } catch (RuntimeException transientFailure) {
            log.warn("Tianshu config dispatch delivery failed and will be retried: tenantId={}, eventId={}",
                    event.getTenantId(), event.getEventId(), transientFailure);
            return DeliveryResult.retryableFailure("TIANSHU_CONFIG_DISPATCH_RETRY", "publish dispatch failed");
        }
    }

    private DdcConfigDispatchPreparedEvent parseAndValidate(DeliveryContext context)
            throws JsonProcessingException {
        if (context == null
                || !Objects.equals(context.channel(), channel())
                || !Objects.equals(context.contentType(), "application/json")
                || !Objects.equals(context.schemaVersion(), outboxRoute.schemaVersion())) {
            throw new IllegalArgumentException("outbox event envelope does not match the route");
        }
        outboxRoute.validateDestination(context.destination());
        DdcConfigDispatchPreparedEvent event = objectMapper.readValue(
                context.payload(), DdcConfigDispatchPreparedEvent.class);
        if (event == null) {
            throw new IllegalArgumentException("prepared config event payload is empty");
        }
        validationUtils.validate(event);
        if (!Objects.equals(context.messageId(), event.getEventId())
                || !eventConverter.isValid(event)
                || !outboxRoute.matchesDestination(event, context.destination())
                || !Objects.equals(context.headers().get("tenantId"), Long.toString(event.getTenantId()))
                || !Objects.equals(context.headers().get("eventType"), outboxRoute.eventType())) {
            throw new IllegalArgumentException("prepared event identity or destination is invalid");
        }
        return event;
    }
}
