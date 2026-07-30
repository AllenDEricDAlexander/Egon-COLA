package top.egon.cola.component.gateway.engine.observability;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class KafkaGatewayCallEventSinkTest {

    @Test
    void acceptsDeliveryTimeoutShorterThanKafkaDefaultRequestTimeout() {
        assertDoesNotThrow(() -> {
            KafkaGatewayCallEventSink sink = new KafkaGatewayCallEventSink(
                    new KafkaGatewayCallEventSink.Settings(
                            "127.0.0.1:1",
                            "gateway-calls",
                            Duration.ofSeconds(1),
                            Duration.ZERO,
                            Map.of()
                    )
            );
            sink.close();
        });
    }

    @Test
    void usesGroupPartitionKeyAndRequiredHeaders() {
        MockProducer<String, byte[]> producer = new MockProducer<>(
                true,
                new StringSerializer(),
                new ByteArraySerializer()
        );
        KafkaGatewayCallEventSink sink = new KafkaGatewayCallEventSink(
                new KafkaGatewayCallEventSink.Settings(
                        "unused:9092",
                        "gateway-calls",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1),
                        Map.of()
                ),
                producer
        );
        GatewayCallObservation observation = GatewayCallObservation.start(
                GatewayTraceContext.fromHeaders(null, null, null),
                "HTTP",
                "PUBLIC",
                "engine-1"
        );
        observation.route(
                "GET",
                "/orders/{id}",
                "group-1",
                "release-1",
                "operation-1",
                "route-1"
        );
        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);

        sink.send(observation.complete(
                "COMPLETE",
                "SUCCESS",
                null,
                200,
                null
        ).orElseThrow(), payload);

        assertEquals(1, producer.history().size());
        assertEquals("gateway-calls", producer.history().getFirst().topic());
        assertEquals("group-1", producer.history().getFirst().key());
        assertEquals(
                "v1",
                new String(
                        producer.history().getFirst().headers()
                                .lastHeader("event-schema-version")
                                .value(),
                        StandardCharsets.UTF_8
                )
        );
    }
}
