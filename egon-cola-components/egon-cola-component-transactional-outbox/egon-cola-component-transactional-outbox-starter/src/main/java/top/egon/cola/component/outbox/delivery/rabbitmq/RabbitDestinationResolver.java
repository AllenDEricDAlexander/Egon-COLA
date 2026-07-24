package top.egon.cola.component.outbox.delivery.rabbitmq;

public interface RabbitDestinationResolver {

    RabbitDeliveryTarget resolve(String destination);
}
