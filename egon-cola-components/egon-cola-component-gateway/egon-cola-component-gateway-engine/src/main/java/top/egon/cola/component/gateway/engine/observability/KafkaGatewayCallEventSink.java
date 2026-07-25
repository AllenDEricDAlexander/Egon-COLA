package top.egon.cola.component.gateway.engine.observability;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public final class KafkaGatewayCallEventSink
        implements GatewayCallEventSink {

    private final Producer<String, byte[]> producer;

    private final String topic;

    private final Duration closeTimeout;

    private final AtomicLong acknowledged = new AtomicLong();

    private final AtomicLong failed = new AtomicLong();

    public KafkaGatewayCallEventSink(Settings settings) {
        this(settings, createProducer(settings));
    }

    KafkaGatewayCallEventSink(
            Settings settings,
            Producer<String, byte[]> producer) {
        this.producer = Objects.requireNonNull(producer, "producer");
        topic = required(settings.topic(), "topic");
        closeTimeout = Objects.requireNonNull(
                settings.closeTimeout(),
                "closeTimeout"
        );
    }

    @Override
    public void send(GatewayCallEventV1 event, byte[] payload) {
        String key = event.routing().gatewayGroupId().isBlank()
                ? event.trace().traceId()
                : event.routing().gatewayGroupId();
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(
                topic,
                null,
                key,
                payload,
                List.of(
                        header("content-type", "application/json"),
                        header("event-schema-version", "v1"),
                        header("event-id", event.eventId()),
                        header("trace-id", event.trace().traceId())
                )
        );
        producer.send(record, (metadata, error) -> {
            if (error == null) {
                acknowledged.incrementAndGet();
            } else {
                failed.incrementAndGet();
            }
        });
    }

    public Delivery delivery() {
        return new Delivery(acknowledged.get(), failed.get());
    }

    @Override
    public void close() {
        producer.close(closeTimeout);
    }

    private static Producer<String, byte[]> createProducer(Settings settings) {
        Properties properties = new Properties();
        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                required(settings.bootstrapServers(), "bootstrapServers")
        );
        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName()
        );
        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                ByteArraySerializer.class.getName()
        );
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        properties.put(
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                Math.toIntExact(settings.deliveryTimeout().toMillis())
        );
        properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        properties.putAll(settings.additionalProperties());
        return new KafkaProducer<>(properties);
    }

    private static RecordHeader header(String name, String value) {
        return new RecordHeader(
                name,
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    public record Settings(
            String bootstrapServers,
            String topic,
            Duration deliveryTimeout,
            Duration closeTimeout,
            Map<String, Object> additionalProperties
    ) {

        public Settings {
            deliveryTimeout = deliveryTimeout == null
                    ? Duration.ofSeconds(10)
                    : deliveryTimeout;
            closeTimeout = closeTimeout == null
                    ? Duration.ofSeconds(2)
                    : closeTimeout;
            additionalProperties = additionalProperties == null
                    ? Map.of()
                    : Map.copyOf(additionalProperties);
        }
    }

    public record Delivery(long acknowledged, long failed) {
    }
}
