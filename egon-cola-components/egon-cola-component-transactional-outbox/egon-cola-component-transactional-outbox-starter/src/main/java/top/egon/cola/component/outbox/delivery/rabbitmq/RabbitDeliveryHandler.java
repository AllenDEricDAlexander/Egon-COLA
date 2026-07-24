package top.egon.cola.component.outbox.delivery.rabbitmq;

import org.springframework.amqp.AmqpException;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitDeliveryHandler implements DeliveryHandler {

    private final RabbitDestinationResolver destinationResolver;
    private final RabbitMessagePublisher publisher;

    public RabbitDeliveryHandler(
            RabbitDestinationResolver destinationResolver,
            RabbitMessagePublisher publisher
    ) {
        this.destinationResolver = destinationResolver;
        this.publisher = publisher;
    }

    @Override
    public String channel() {
        return "rabbitmq";
    }

    @Override
    public void validateDestination(String destination) {
        destinationResolver.resolve(destination);
    }

    @Override
    public DeliveryResult deliver(DeliveryContext context) throws Exception {
        RabbitDeliveryTarget target = destinationResolver.resolve(context.destination());
        try {
            RabbitPublishOutcome outcome = publisher.publish(target, context);
            return switch (outcome.kind()) {
                case ACK -> DeliveryResult.success();
                case NACK -> DeliveryResult.retryableFailure("RABBIT_NACK", outcome.reason());
                case TIMEOUT -> DeliveryResult.retryableFailure(
                        "RABBIT_CONFIRM_TIMEOUT",
                        "RABBIT_CONFIRM_TIMEOUT"
                );
                case RETURNED -> DeliveryResult.permanentFailure(
                        "RABBIT_UNROUTABLE",
                        outcome.reason()
                );
            };
        } catch (AmqpException | IOException | TimeoutException exception) {
            return DeliveryResult.retryableFailure(
                    "RABBIT_CONNECTION_ERROR",
                    exception.getClass().getSimpleName()
            );
        }
    }
}
