package top.egon.cola.component.outbox.delivery.rabbitmq;

import top.egon.cola.component.outbox.autoconfigure.TransactionalOutboxProperties;
import top.egon.cola.component.outbox.exception.OutboxValidationException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class PropertiesRabbitDestinationResolver implements RabbitDestinationResolver {

    private final Map<String, RabbitDeliveryTarget> targets;

    public PropertiesRabbitDestinationResolver(TransactionalOutboxProperties properties) {
        Duration defaultConfirmTimeout = properties.getRabbitmq().getConfirmTimeout();
        Map<String, RabbitDeliveryTarget> configured = new LinkedHashMap<>();
        properties.getRabbitmq().getDestinations().forEach((name, destination) ->
                configured.put(name, new RabbitDeliveryTarget(
                        destination.getExchange(),
                        destination.getRoutingKey(),
                        destination.isMandatory(),
                        destination.getConfirmTimeout() == null
                                ? defaultConfirmTimeout
                                : destination.getConfirmTimeout(),
                        destination.getFixedHeaders()
                )));
        this.targets = Map.copyOf(configured);
    }

    @Override
    public RabbitDeliveryTarget resolve(String destination) {
        RabbitDeliveryTarget target = targets.get(destination);
        if (target == null) {
            throw new OutboxValidationException(
                    "Unknown transactional outbox RabbitMQ destination: " + destination
            );
        }
        return target;
    }
}
