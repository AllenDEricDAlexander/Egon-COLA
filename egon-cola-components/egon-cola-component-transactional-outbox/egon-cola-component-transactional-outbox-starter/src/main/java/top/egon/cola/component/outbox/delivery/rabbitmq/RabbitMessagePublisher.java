package top.egon.cola.component.outbox.delivery.rabbitmq;

import top.egon.cola.component.outbox.delivery.DeliveryContext;

public interface RabbitMessagePublisher {

    RabbitPublishOutcome publish(
            RabbitDeliveryTarget target,
            DeliveryContext context
    ) throws Exception;
}
