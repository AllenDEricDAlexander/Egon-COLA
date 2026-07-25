package top.egon.cola.component.gateway.admin.infrastructure.messaging;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GatewayKafkaCallEventConsumer
        implements SmartLifecycle {

    private final KafkaConsumer<String, byte[]> consumer;

    private final GatewayCallEventConsumerHandler handler;

    private final Settings settings;

    private final AtomicBoolean running = new AtomicBoolean();

    private Thread worker;

    public GatewayKafkaCallEventConsumer(
            GatewayCallEventConsumerHandler handler,
            Settings settings) {
        this.handler = Objects.requireNonNull(handler, "handler");
        this.settings = Objects.requireNonNull(settings, "settings");
        consumer = new KafkaConsumer<>(properties(settings));
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        consumer.subscribe(java.util.List.of(settings.topic()));
        worker = Thread.ofPlatform()
                .daemon(true)
                .name("gateway-admin-call-event-consumer")
                .start(this::consume);
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        consumer.wakeup();
        if (worker != null) {
            try {
                worker.join(settings.closeTimeout().toMillis());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        consumer.close(settings.closeTimeout());
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    private void consume() {
        try {
            while (running.get()) {
                ConsumerRecords<String, byte[]> records =
                        consumer.poll(settings.pollTimeout());
                for (ConsumerRecord<String, byte[]> record : records) {
                    handler.handle(record);
                    consumer.commitSync(
                            Map.of(
                                    new TopicPartition(
                                            record.topic(),
                                            record.partition()
                                    ),
                                    new OffsetAndMetadata(
                                            record.offset() + 1
                                    )
                            ),
                            settings.commitTimeout()
                    );
                }
            }
        } catch (WakeupException wakeup) {
            if (running.get()) {
                throw wakeup;
            }
        } finally {
            running.set(false);
        }
    }

    private Properties properties(Settings settings) {
        Properties properties = new Properties();
        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                required(settings.bootstrapServers(), "bootstrapServers")
        );
        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                required(settings.groupId(), "groupId")
        );
        properties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName()
        );
        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ByteArrayDeserializer.class.getName()
        );
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        properties.putAll(settings.additionalProperties());
        return properties;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    public record Settings(
            String bootstrapServers,
            String topic,
            String groupId,
            Duration pollTimeout,
            Duration commitTimeout,
            Duration closeTimeout,
            Map<String, Object> additionalProperties
    ) {

        public Settings {
            pollTimeout = pollTimeout == null
                    ? Duration.ofMillis(500)
                    : pollTimeout;
            commitTimeout = commitTimeout == null
                    ? Duration.ofSeconds(5)
                    : commitTimeout;
            closeTimeout = closeTimeout == null
                    ? Duration.ofSeconds(5)
                    : closeTimeout;
            additionalProperties = additionalProperties == null
                    ? Map.of()
                    : Map.copyOf(additionalProperties);
        }
    }
}
