package top.egon.cola.component.outbox.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.outbox.exception.OutboxSerializationException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonOutboxMessageSerializerTest {

    private final JacksonOutboxMessageSerializer serializer =
            new JacksonOutboxMessageSerializer(new ObjectMapper());

    @Test
    void shouldSerializeObjectWithApplicationObjectMapper() {
        SerializedOutboxPayload serialized =
                serializer.serialize(new SamplePayload("O-1"), "application/json");

        assertThat(serialized.text()).isEqualTo("{\"orderId\":\"O-1\"}");
        assertThat(serialized.utf8Bytes())
                .isEqualTo(serialized.text().getBytes(StandardCharsets.UTF_8).length);
    }

    @Test
    void shouldPreserveCompatibleStringPayload() {
        assertThat(serializer.serialize("{\"orderId\":\"O-1\"}", "application/json").text())
                .isEqualTo("{\"orderId\":\"O-1\"}");
        assertThat(serializer.serialize("plain", "text/plain").text()).isEqualTo("plain");
    }

    @Test
    void shouldWrapJacksonFailureWithoutIncludingPayload() {
        assertThatThrownBy(() -> serializer.serialize(new SelfReference(), "application/json"))
                .isInstanceOf(OutboxSerializationException.class)
                .hasMessage("Failed to serialize outbox payload");
    }

    record SamplePayload(String orderId) {
    }

    static final class SelfReference {

        public SelfReference getSelf() {
            return this;
        }
    }
}
