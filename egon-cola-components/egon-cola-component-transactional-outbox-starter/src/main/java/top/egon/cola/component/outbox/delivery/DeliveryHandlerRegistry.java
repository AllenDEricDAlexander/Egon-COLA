package top.egon.cola.component.outbox.delivery;

import top.egon.cola.component.outbox.exception.OutboxConfigurationException;
import top.egon.cola.component.outbox.exception.OutboxValidationException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class DeliveryHandlerRegistry {

    private static final Pattern CHANNEL_PATTERN = Pattern.compile("[a-z][a-z0-9._-]{0,63}");

    private final Map<String, DeliveryHandler> handlers;

    public DeliveryHandlerRegistry(List<DeliveryHandler> handlers) {
        Map<String, DeliveryHandler> registered = new LinkedHashMap<>();
        for (DeliveryHandler handler : handlers) {
            if (handler == null) {
                throw new OutboxConfigurationException("Transactional outbox delivery handler is null");
            }
            String channel = handler.channel() == null ? null : handler.channel().trim();
            if (channel == null || !CHANNEL_PATTERN.matcher(channel).matches()) {
                throw new OutboxConfigurationException(
                        "Invalid transactional outbox delivery channel: " + channel
                );
            }
            if (registered.putIfAbsent(channel, handler) != null) {
                throw new OutboxConfigurationException(
                        "Duplicate transactional outbox delivery channel: " + channel
                );
            }
        }
        this.handlers = Collections.unmodifiableMap(registered);
    }

    public DeliveryHandler required(String channel) {
        DeliveryHandler handler = handlers.get(channel);
        if (handler == null) {
            throw new OutboxValidationException("No transactional outbox handler for channel");
        }
        return handler;
    }
}
