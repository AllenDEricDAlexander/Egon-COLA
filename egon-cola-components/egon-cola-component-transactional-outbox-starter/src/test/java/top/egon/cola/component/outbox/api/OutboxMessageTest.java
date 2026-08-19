package top.egon.cola.component.outbox.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.exception.OutboxValidationException;
import top.egon.cola.component.outbox.validation.OutboxMessageValidator;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxMessageTest {

    private final OutboxMessageValidator validator =
            new OutboxMessageValidator(new ObjectMapper(), 1024 * 1024, 64, 16 * 1024);

    @Test
    void shouldBuildImmutableMessageWithDefaults() {
        OutboxMessage message = OutboxMessage.builder()
                .channel("rabbitmq")
                .destination("order-created")
                .payload(Map.of("orderId", "O-1"))
                .header("X-Tenant", "tenant-a")
                .availableAt(Instant.parse("2026-07-24T00:00:00Z"))
                .build();

        validator.validateEnvelope(message);

        assertThat(message.contentType()).isEqualTo("application/json");
        assertThat(message.headers()).containsEntry("X-Tenant", "tenant-a");
        assertThatThrownBy(() -> message.headers().put("X-Test", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectSensitiveAndTransportHeadersCaseInsensitively() {
        OutboxMessage message = OutboxMessage.builder()
                .channel("http")
                .destination("order-callback")
                .payload("{}")
                .header("authorization", "secret")
                .build();

        assertThatThrownBy(() -> validator.validateEnvelope(message))
                .isInstanceOf(OutboxValidationException.class)
                .hasMessageContaining("authorization");
    }

    @Test
    void shouldRejectMissingChannelDestinationAndPayload() {
        OutboxMessage message = OutboxMessage.builder().build();

        assertThatThrownBy(() -> validator.validateEnvelope(message))
                .isInstanceOf(OutboxValidationException.class);
    }
}
