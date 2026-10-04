package top.egon.cola.component.tianshu.listener.config;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.redisson.api.listener.MessageListener;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcTenantProperties;
import top.egon.cola.component.tianshu.format.DdcChecksum;
import top.egon.cola.component.tianshu.model.config.DdcPublishMessage;
import top.egon.cola.component.tianshu.model.event.DdcConfigDispatchPreparedEvent;
import top.egon.cola.component.tianshu.model.tenant.DdcTenantMessageDTO;
import top.egon.cola.component.tianshu.observability.DdcTraceSupport;
import top.egon.cola.component.tianshu.service.refresh.DdcRefreshService;

import java.util.Map;

/**
 * 过滤并校验Redis配置发布事件，然后将有效消息交给刷新服务。
 * Validates Redis configuration-publication events before handing valid messages to the refresh service.
 */
@Slf4j
@RequiredArgsConstructor
public class DdcConfigChangeListener implements MessageListener<DdcTenantMessageDTO<?>> {

    /** 用于限定本客户端业务、应用和环境的属性。 Properties defining this client's scope. */
    @Qualifier("egon.cola.component.tianshu-top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties")
    private final DdcProperties properties;

    /** 当前客户端的可信租户配置。 Trusted tenant configuration for this client. */
    @Qualifier("egon.cola.component.tianshu-top.egon.cola.component.tianshu.autoconfigure.properties.DdcTenantProperties")
    private final DdcTenantProperties tenantProperties;

    /** 执行配置版本检查、应用和确认的刷新服务。 Refresh service that checks, applies, and acknowledges configuration. */
    @Qualifier("ddcRefreshService")
    private final DdcRefreshService refreshService;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    /**
     * 忽略空、跨租户、未知或摘要无效事件，并刷新有效配置消息。
     * Ignores empty, cross-tenant, unknown, or checksum-invalid events before refreshing valid messages.
     *
     * @param channel 收到消息的Redis channel / Redis channel that delivered the event
     * @param envelope tenant-validated outer message envelope
     */
    @Override
    public void onMessage(CharSequence channel, @Nullable DdcTenantMessageDTO<?> envelope) {
        try (DdcTraceSupport.Scope ignored = DdcTraceSupport.openOperation("redis-change")) {
            long tenantId = tenantProperties.requireTenantId();
            if (envelope == null) {
                log.warn("Discarding Tianshu configuration message with a missing tenant envelope");
                return;
            }
            try {
                validationUtils.validate(envelope);
            } catch (ConstraintViolationException invalidEnvelope) {
                log.warn("Discarding Tianshu configuration message with invalid envelope constraints");
                return;
            }
            if (!envelope.belongsToTenant(tenantId)) {
                log.warn("Discarding Tianshu configuration message with an invalid tenant envelope");
                return;
            }

            DdcPublishMessage message = payloadMessage(tenantId, envelope.getPayload());
            if (message == null || !matchesScope(message)
                    || !matchesChecksum(message) || !matchesResourceChecksum(message)) {
                return;
            }
            runWithTenant(tenantId, () -> refreshService.refresh(message));
        }
    }

    private DdcPublishMessage payloadMessage(long tenantId, Object payload) {
        if (payload instanceof DdcConfigDispatchPreparedEvent event) {
            if (!Long.valueOf(tenantId).equals(event.getTenantId())
                    || !"1".equals(event.getSchemaVersion())
                    || !event.hasValidIdentityAndChecksum()) {
                log.warn("Discarding Tianshu config event with invalid tenant, schema or identity: tenantId={}, eventId={}",
                        tenantId, event.getEventId());
                return null;
            }
            return event.getMessage();
        }
        if (payload instanceof DdcPublishMessage legacyMessage) {
            return legacyMessage;
        }
        log.warn("Discarding Tianshu configuration message with unsupported payload type: {}",
                payload == null ? "null" : payload.getClass().getName());
        return null;
    }

    private void runWithTenant(long tenantId, Runnable action) {
        Map<String, String> previous = MDC.getCopyOfContextMap();
        try {
            MDC.put("tenantId", Long.toString(tenantId));
            action.run();
        } finally {
            if (previous == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(previous);
            }
        }
    }

    /** 判断消息是否属于当前业务、应用和环境。 Determines whether the message matches this client's scope. */
    private boolean matchesScope(DdcPublishMessage message) {
        return properties.getBizCode().equals(message.getBizCode())
                && properties.getAppCode().equals(message.getAppCode())
                && properties.getEnv().equals(message.getEnv());
    }

    /** 校验发布消息整体摘要。 Validates the publication-message checksum. */
    private boolean matchesChecksum(DdcPublishMessage message) {
        return message.getChecksum() != null
                && message.getChecksum().equals(DdcChecksum.sha256(message));
    }

    /** 校验配置正文摘要。 Validates the configuration content checksum. */
    private boolean matchesResourceChecksum(DdcPublishMessage message) {
        if (message.getContent() == null) {
            return message.getResourceChecksum() != null
                    && !message.getResourceChecksum().isBlank();
        }
        return message.getResourceChecksum() != null
                && message.getResourceChecksum().equals(DdcChecksum.resource(
                        message.getResourceName(), message.getFormat(), message.getContent()));
    }
}
