package top.egon.cola.component.ddc.listener;

import org.redisson.api.listener.MessageListener;
import top.egon.cola.component.ddc.common.DdcChecksum;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.service.DdcRefreshService;
import top.egon.cola.component.ddc.trace.DdcTraceSupport;

/**
 * 过滤并校验 Redis 配置发布消息，然后将有效消息交给刷新服务。 Filters and validates Redis configuration-publication messages before handing valid messages to the refresh service.
 */
public class DdcRedisChangeListener implements MessageListener<DdcPublishMessage> {

    /**
     * 用于限定本客户端业务、应用和环境的属性。 Properties defining this client's business, application, and environment scope.
     */
    private final DdcProperties properties;

    /**
     * 执行配置版本检查、应用和确认的刷新服务。 Refresh service that performs version checks, application, and acknowledgement.
     */
    private final DdcRefreshService refreshService;

    /**
     * 创建作用域受限的 Redis 配置变更监听器。 Creates a scope-restricted Redis configuration-change listener.
     *
     * @param properties     DDC 作用域属性。 DDC scope properties
     * @param refreshService 配置刷新服务。 configuration refresh service
     */
    public DdcRedisChangeListener(DdcProperties properties, DdcRefreshService refreshService) {
        this.properties = properties;
        this.refreshService = refreshService;
    }

    /**
     * 在追踪作用域内忽略空、跨作用域或摘要无效消息，并刷新有效消息。 Ignores null, cross-scope, or checksum-invalid messages within a trace scope and refreshes valid messages.
     *
     * @param channel 接收消息的 Redis 频道。 Redis channel that delivered the message
     * @param message 发布消息。 publication message
     */
    @Override
    public void onMessage(CharSequence channel, DdcPublishMessage message) {
        try (DdcTraceSupport.Scope ignored =
                     DdcTraceSupport.openOperation("redis-change")) {
            if (message == null
                    || !matchesScope(message)
                    || !matchesChecksum(message)
                    || !matchesContentChecksum(message)) {
                return;
            }
            refreshService.refresh(message);
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
     * 校验配置内容摘要与消息内容一致。 Validates that the content checksum matches the message content.
     *
     * @param message 发布消息。 publication message
     * @return 内容摘要有效时为 {@code true}。 {@code true} when the content checksum is valid
     */
    private boolean matchesContentChecksum(DdcPublishMessage message) {
        return message.getContentChecksum() != null
                && message.getContentChecksum().equals(DdcChecksum.content(message.getConfigValue()));
    }
}
