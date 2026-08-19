package top.egon.cola.component.outbox.delivery.rabbitmq;

import java.time.Duration;
import java.util.Map;

public record RabbitDeliveryTarget(
        String exchange,
        String routingKey,
        boolean mandatory,
        Duration confirmTimeout,
        Map<String, String> fixedHeaders
) {

    public RabbitDeliveryTarget {
        fixedHeaders = Map.copyOf(fixedHeaders);
    }
}
