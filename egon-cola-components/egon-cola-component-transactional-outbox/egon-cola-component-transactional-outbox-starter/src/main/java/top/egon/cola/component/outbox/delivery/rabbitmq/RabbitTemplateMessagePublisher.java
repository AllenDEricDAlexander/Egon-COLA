package top.egon.cola.component.outbox.delivery.rabbitmq;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RabbitTemplateMessagePublisher implements RabbitMessagePublisher {

    private static final Set<String> TRANSPORT_HEADERS = Set.of(
            "content-type",
            "content-length",
            "message-id",
            "delivery-mode",
            "expiration",
            "priority",
            "reply-to",
            "correlation-id",
            "user-id",
            "app-id",
            "cluster-id",
            "type",
            "timestamp"
    );

    private final RabbitTemplate rabbitTemplate;
    private final Clock clock;

    public RabbitTemplateMessagePublisher(RabbitTemplate rabbitTemplate, Clock clock) {
        this.rabbitTemplate = rabbitTemplate;
        this.clock = clock;
    }

    @Override
    public RabbitPublishOutcome publish(
            RabbitDeliveryTarget target,
            DeliveryContext context
    ) throws Exception {
        Duration remaining = Duration.between(clock.instant(), context.deadline());
        if (remaining.isZero() || remaining.isNegative()) {
            return RabbitPublishOutcome.timeout();
        }
        Duration wait = shorter(target.confirmTimeout(), remaining);
        Message message = createMessage(target, context);
        CorrelationData correlationData = new CorrelationData(
                context.messageId() + "-" + context.attempt() + "-" + UUID.randomUUID()
        );

        rabbitTemplate.send(
                target.exchange(),
                target.routingKey(),
                message,
                correlationData
        );

        CorrelationData.Confirm confirm;
        try {
            confirm = correlationData.getFuture().get(wait.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            return RabbitPublishOutcome.timeout();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw exception;
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) {
                throw checked;
            }
            throw new IllegalStateException("RabbitMQ publisher confirm failed", cause);
        }

        ReturnedMessage returned = correlationData.getReturned();
        if (returned != null) {
            return RabbitPublishOutcome.returned(
                    returned.getReplyCode(),
                    returned.getReplyText()
            );
        }
        return confirm.isAck()
                ? RabbitPublishOutcome.ack()
                : RabbitPublishOutcome.nack(confirm.getReason());
    }

    private Message createMessage(
            RabbitDeliveryTarget target,
            DeliveryContext context
    ) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(context.contentType());
        messageProperties.setMessageId(context.messageId());
        messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        applyHeaders(messageProperties, target.fixedHeaders());
        applyHeaders(messageProperties, context.headers());
        if (context.schemaVersion() != null) {
            messageProperties.setHeader(
                    "x-egon-cola-schema-version",
                    context.schemaVersion()
            );
        }
        messageProperties.setHeader("x-egon-cola-attempt", context.attempt());
        return new Message(context.payload().getBytes(StandardCharsets.UTF_8), messageProperties);
    }

    private void applyHeaders(MessageProperties target, Map<String, String> source) {
        source.forEach((name, value) -> {
            String normalized = name.toLowerCase(Locale.ROOT);
            if (TRANSPORT_HEADERS.contains(normalized)
                    || normalized.startsWith("amqp_")
                    || normalized.startsWith("spring_")
                    || OutboxMessageValidator.isForbiddenHeader(name)) {
                return;
            }
            target.setHeader(name, value);
        });
    }

    private Duration shorter(Duration configured, Duration remaining) {
        return configured.compareTo(remaining) <= 0 ? configured : remaining;
    }
}
