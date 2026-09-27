package top.egon.cola.component.tianshu.listener.config;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.RequiredArgsConstructor;
import org.redisson.api.listener.MessageListener;
import org.slf4j.MDC;
import top.egon.cola.component.tianshu.format.DdcChecksum;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcTenantProperties;
import top.egon.cola.component.tianshu.model.config.DdcPublishMessage;
import top.egon.cola.component.tianshu.model.tenant.DdcTenantMessageDTO;
import top.egon.cola.component.tianshu.service.refresh.DdcRefreshService;
import top.egon.cola.component.tianshu.observability.DdcTraceSupport;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.util.Map;

/**
 * 过滤并校验 Redis 配置发布消息，然后将有效消息交给刷新服务。 Filters and validates Redis configuration-publication messages before handing valid messages to the refresh service.
 */
@Slf4j
@RequiredArgsConstructor
public class DdcConfigChangeListener
        implements MessageListener<DdcTenantMessageDTO<DdcPublishMessage>> {

    /**
     * 用于限定本客户端业务、应用和环境的属性。 Properties defining this client's business, application, and environment scope.
     */
    @Qualifier("egon.cola.component.tianshu-top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties")
    private final DdcProperties properties;

    @Qualifier("egon.cola.component.tianshu-top.egon.cola.component.tianshu.autoconfigure.properties.DdcTenantProperties")
    private final DdcTenantProperties tenantProperties;

    /**
     * 执行配置版本检查、应用和确认的刷新服务。 Refresh service that performs version checks, application, and acknowledgement.
     */
    @Qualifier("ddcRefreshService")
    private final DdcRefreshService refreshService;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    /**
     * 在追踪作用域内忽略空、跨作用域或摘要无效消息，并刷新有效消息。 Ignores null, cross-scope, or checksum-invalid messages within a trace scope and refreshes valid messages.
     *
     * @param channel 接收消息的 Redis 频道。 Redis channel that delivered the message
     * @param envelope tenant-validated outer message envelope
     */
    @Override
    public void onMessage(CharSequence channel,
                          @Nullable DdcTenantMessageDTO<DdcPublishMessage> envelope) {
        try (DdcTraceSupport.Scope ignored =
                     DdcTraceSupport.openOperation("redis-change")) {
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
            DdcPublishMessage message = envelope.getPayload();
            if (!matchesScope(message)
                    || !matchesChecksum(message)
                    || !matchesResourceChecksum(message)) {
                return;
            }
            runWithTenant(tenantId, () -> refreshService.refresh(message));
        }
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

    /**
     * 判断消息是否属于当前业务、应用和环境。 Determines whether a message belongs to the current business, application, and environment.
     *
     * @param message 发布消息。 publication message
     * @return 作用域完全匹配时为 {@code true}。 {@code true} when the scope matches exactly
     */
    private boolean matchesScope(DdcPublishMessage message) {
        return properties.getBizCode().equals(message.getBizCode())
                && properties.getAppCode().equals(message.getAppCode())
                && properties.getEnv().equals(message.getEnv());
    }

    /**
     * 校验发布消息整体摘要非空且与重新计算结果一致。 Validates that the publication checksum is present and matches a fresh calculation.
     *
     * @param message 发布消息。 publication message
     * @return 整体摘要有效时为 {@code true}。 {@code true} when the message checksum is valid
     */
    private boolean matchesChecksum(DdcPublishMessage message) {
        return message.getChecksum() != null
                && !message.getChecksum().isBlank()
                && message.getChecksum().equals(DdcChecksum.sha256(message));
    }

    /**
     * 校验配置内容摘要与消息内容一致。 Validates that the resource checksum matches the message content.
     *
     * @param message 发布消息。 publication message
     * @return 内容摘要有效时为 {@code true}。 {@code true} when the resource checksum is valid
     */
    private boolean matchesResourceChecksum(DdcPublishMessage message) {
        if (message.getContent() == null) {
            return message.getResourceChecksum() != null
                    && !message.getResourceChecksum().isBlank();
        }
        return message.getResourceChecksum() != null
                && message.getResourceChecksum().equals(DdcChecksum.resource(
                message.getResourceName(),
                message.getFormat(),
                message.getContent()
        ));
    }
}
