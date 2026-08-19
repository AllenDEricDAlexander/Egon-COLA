package top.egon.cola.component.outbox.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.rabbitmq.RabbitDeliveryTarget;
import top.egon.cola.component.outbox.delivery.rabbitmq.RabbitPublishOutcome;
import top.egon.cola.component.outbox.delivery.rabbitmq.RabbitTemplateMessagePublisher;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitDeliveryHandlerIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final RabbitDeliveryTarget target = new RabbitDeliveryTarget(
            "orders.events",
            "order.created",
            true,
            Duration.ofMillis(100),
            Map.of("X-Source", "orders")
    );
    private RabbitTemplateMessagePublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RabbitTemplateMessagePublisher(
                rabbitTemplate,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldPublishPersistentMessageAndWaitForCorrelatedAck() throws Exception {
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3);
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(
                eq("orders.events"),
                eq("order.created"),
                any(Message.class),
                any(CorrelationData.class)
        );

        RabbitPublishOutcome outcome = publisher.publish(target, context("message-1", 1));

        assertThat(outcome).isEqualTo(RabbitPublishOutcome.ack());
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(
                eq("orders.events"),
                eq("order.created"),
                messageCaptor.capture(),
                any(CorrelationData.class)
        );
        Message message = messageCaptor.getValue();
        assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).isEqualTo("{}");
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo("message-1");
        assertThat(message.getMessageProperties().getDeliveryMode())
                .isEqualTo(MessageDeliveryMode.PERSISTENT);
        Object schemaVersion =
                message.getMessageProperties().getHeader("x-egon-cola-schema-version");
        Object attempt = message.getMessageProperties().getHeader("x-egon-cola-attempt");
        assertThat(schemaVersion).isEqualTo("1");
        assertThat(attempt).isEqualTo(1);
    }

    @Test
    void shouldPreferMandatoryReturnEvenWhenConfirmIsAck() throws Exception {
        doAnswer(invocation -> {
            Message message = invocation.getArgument(2);
            CorrelationData correlationData = invocation.getArgument(3);
            correlationData.setReturned(new ReturnedMessage(
                    message,
                    312,
                    "NO_ROUTE",
                    "orders.events",
                    "missing"
            ));
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(
                any(String.class),
                any(String.class),
                any(Message.class),
                any(CorrelationData.class)
        );

        RabbitPublishOutcome outcome = publisher.publish(target, context("message-1", 1));

        assertThat(outcome.kind()).isEqualTo(RabbitPublishOutcome.Kind.RETURNED);
        assertThat(outcome.replyCode()).isEqualTo(312);
    }

    @Test
    void shouldReturnNackAndUseUniqueCorrelationForRepeatedMessageId() throws Exception {
        ArgumentCaptor<CorrelationData> correlationCaptor =
                ArgumentCaptor.forClass(CorrelationData.class);
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3);
            correlationData.getFuture().complete(new CorrelationData.Confirm(false, "nack"));
            return null;
        }).when(rabbitTemplate).send(
                any(String.class),
                any(String.class),
                any(Message.class),
                correlationCaptor.capture()
        );

        RabbitPublishOutcome first = publisher.publish(target, context("message-1", 1));
        RabbitPublishOutcome second = publisher.publish(target, context("message-1", 2));

        assertThat(first.kind()).isEqualTo(RabbitPublishOutcome.Kind.NACK);
        assertThat(second.kind()).isEqualTo(RabbitPublishOutcome.Kind.NACK);
        assertThat(correlationCaptor.getAllValues())
                .extracting(CorrelationData::getId)
                .doesNotHaveDuplicates();
    }

    @Test
    void shouldBoundConfirmWaitByDeadline() throws Exception {
        RabbitPublishOutcome outcome = publisher.publish(
                target,
                context("message-1", 1, NOW.plusMillis(10))
        );

        assertThat(outcome).isEqualTo(RabbitPublishOutcome.timeout());
    }

    private DeliveryContext context(String messageId, int attempt) {
        return context(messageId, attempt, NOW.plusSeconds(1));
    }

    private DeliveryContext context(String messageId, int attempt, Instant deadline) {
        return new DeliveryContext(
                messageId,
                "rabbitmq",
                "order-events",
                "{}",
                "application/json",
                "1",
                Map.of("X-Tenant", "tenant-1"),
                "trace-1",
                attempt,
                10,
                deadline
        );
    }
}
