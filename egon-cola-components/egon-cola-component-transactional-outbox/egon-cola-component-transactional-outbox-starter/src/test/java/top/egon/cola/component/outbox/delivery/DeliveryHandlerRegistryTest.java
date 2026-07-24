package top.egon.cola.component.outbox.delivery;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.exception.OutboxConfigurationException;
import top.egon.cola.component.outbox.exception.OutboxValidationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryHandlerRegistryTest {

    @Test
    void shouldRejectDuplicateChannelsAtStartup() {
        DeliveryHandler first = handler("http");
        DeliveryHandler second = handler("http");

        assertThatThrownBy(() -> new DeliveryHandlerRegistry(List.of(first, second)))
                .isInstanceOf(OutboxConfigurationException.class)
                .hasMessageContaining("http");
    }

    @Test
    void shouldResolveExactlyOneHandlerAndValidateDestination() {
        DeliveryHandler handler = handler("custom");
        DeliveryHandlerRegistry registry = new DeliveryHandlerRegistry(List.of(handler));

        assertThat(registry.required("custom")).isSameAs(handler);
        assertThatThrownBy(() -> registry.required("missing"))
                .isInstanceOf(OutboxValidationException.class);
    }

    private DeliveryHandler handler(String channel) {
        return new DeliveryHandler() {
            @Override
            public String channel() {
                return channel;
            }

            @Override
            public void validateDestination(String destination) {
            }

            @Override
            public DeliveryResult deliver(DeliveryContext context) {
                return DeliveryResult.success();
            }
        };
    }
}
