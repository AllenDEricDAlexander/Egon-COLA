package top.egon.cola.component.ddc.listener;

import org.redisson.api.listener.MessageListener;
import top.egon.cola.component.ddc.common.DdcChecksum;
import top.egon.cola.component.ddc.config.DdcProperties;
import top.egon.cola.component.ddc.model.dto.DdcPublishMessage;
import top.egon.cola.component.ddc.service.DdcRefreshService;

public class DdcRedisChangeListener implements MessageListener<DdcPublishMessage> {

    private final DdcProperties properties;

    private final DdcRefreshService refreshService;

    public DdcRedisChangeListener(DdcProperties properties, DdcRefreshService refreshService) {
        this.properties = properties;
        this.refreshService = refreshService;
    }

    @Override
    public void onMessage(CharSequence channel, DdcPublishMessage message) {
        if (message == null || !matchesScope(message) || !matchesChecksum(message)) {
            return;
        }
        refreshService.refresh(message);
    }

    private boolean matchesScope(DdcPublishMessage message) {
        return properties.getAppCode().equals(message.getAppCode())
                && properties.getEnv().equals(message.getEnv())
                && properties.getNamespace().equals(message.getNamespace());
    }

    private boolean matchesChecksum(DdcPublishMessage message) {
        return message.getChecksum() == null || message.getChecksum().isEmpty()
                || message.getChecksum().equals(DdcChecksum.sha256(message));
    }
}
