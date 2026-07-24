package top.egon.cola.component.outbox.delivery.rabbitmq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.amqp.AmqpConnectException;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryResult;
import top.egon.cola.component.outbox.exception.OutboxValidationException;

import java.net.ConnectException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class RabbitDeliveryHandlerTest {

    private final RecordingPublisher publisher = new RecordingPublisher();
    private final RabbitDeliveryTarget target = new RabbitDeliveryTarget(
            "orders.events",
            "order.created",
            true,
            Duration.ofSeconds(1),
            Map.of()
    );
    private RabbitDeliveryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RabbitDeliveryHandler(
                destination -> {
                    if (!"order-events".equals(destination)) {
                        throw new OutboxValidationException("Unknown Rabbit destination: " + destination);
                    }
                    return target;
                },
                publisher
        );
    }

    @ParameterizedTest
    @MethodSource("outcomes")
    void shouldClassifyBrokerOutcome(
            RabbitPublishOutcome outcome,
            DeliveryResult.Kind expected,
            String code
    ) throws Exception {
        publisher.outcome = outcome;

        DeliveryResult result = handler.deliver(context("message-1", 1));

        assertThat(result.kind()).isEqualTo(expected);
        assertThat(result.code()).isEqualTo(code);
    }

    static Stream<Arguments> outcomes() {
        return Stream.of(
                arguments(RabbitPublishOutcome.ack(), DeliveryResult.Kind.SUCCESS, null),
                arguments(
                        RabbitPublishOutcome.nack("broker nack"),
                        DeliveryResult.Kind.RETRYABLE_FAILURE,
                        "RABBIT_NACK"
                ),
                arguments(
                        RabbitPublishOutcome.timeout(),
                        DeliveryResult.Kind.RETRYABLE_FAILURE,
                        "RABBIT_CONFIRM_TIMEOUT"
                ),
                arguments(
                        RabbitPublishOutcome.returned(312, "NO_ROUTE"),
                        DeliveryResult.Kind.PERMANENT_FAILURE,
                        "RABBIT_UNROUTABLE"
                )
        );
    }

    @Test
    void shouldRejectUnknownDestination() {
        assertThatThrownBy(() -> handler.validateDestination("missing"))
                .isInstanceOf(OutboxValidationException.class);
    }

    @Test
    void shouldClassifyConnectionFailureWithoutLeakingDetails() throws Exception {
        publisher.failure = new AmqpConnectException(new ConnectException("secret-host"));

        DeliveryResult result = handler.deliver(context("message-1", 1));

        assertThat(result.kind()).isEqualTo(DeliveryResult.Kind.RETRYABLE_FAILURE);
        assertThat(result.code()).isEqualTo("RABBIT_CONNECTION_ERROR");
        assertThat(result.message()).isEqualTo("AmqpConnectException");
    }

    @Test
    void shouldPreserveMessageIdAcrossAttempts() throws Exception {
        publisher.outcome = RabbitPublishOutcome.ack();

        handler.deliver(context("message-1", 1));
        handler.deliver(context("message-1", 2));

        assertThat(publisher.messageIds).containsExactly("message-1", "message-1");
    }

    private DeliveryContext context(String messageId, int attempt) {
        return new DeliveryContext(
                messageId,
                "rabbitmq",
                "order-events",
                "{}",
                "application/json",
                "1",
                Map.of(),
                "trace-1",
                attempt,
                10,
                Instant.now().plusSeconds(5)
        );
    }

    private static final class RecordingPublisher implements RabbitMessagePublisher {

        private final List<String> messageIds = new ArrayList<>();
        private RabbitPublishOutcome outcome;
        private Exception failure;

        @Override
        public RabbitPublishOutcome publish(
                RabbitDeliveryTarget target,
                DeliveryContext context
        ) throws Exception {
            messageIds.add(context.messageId());
            if (failure != null) {
                throw failure;
            }
            return outcome;
        }
    }
}
