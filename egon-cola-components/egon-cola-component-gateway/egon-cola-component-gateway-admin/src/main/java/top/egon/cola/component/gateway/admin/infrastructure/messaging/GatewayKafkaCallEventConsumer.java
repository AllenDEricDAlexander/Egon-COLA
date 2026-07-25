package top.egon.cola.component.gateway.admin.infrastructure.messaging;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.FencedInstanceIdException;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GatewayKafkaCallEventConsumer
        implements SmartLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            GatewayKafkaCallEventConsumer.class
    );

    private final Consumer<String, byte[]> consumer;

    private final GatewayCallEventConsumerHandler handler;

    private final GatewayKafkaConsumerMetrics metrics;

    private final Settings settings;

    private final AtomicBoolean running = new AtomicBoolean();

    private final Map<RecordKey, Integer> attempts = new HashMap<>();

    private Thread worker;

    public GatewayKafkaCallEventConsumer(
            GatewayCallEventConsumerHandler handler,
            GatewayKafkaConsumerMetrics metrics,
            Settings settings) {
        this(
                new KafkaConsumer<>(properties(settings)),
                handler,
                metrics,
                settings
        );
    }

    GatewayKafkaCallEventConsumer(
            Consumer<String, byte[]> consumer,
            GatewayCallEventConsumerHandler handler,
            GatewayKafkaConsumerMetrics metrics,
            Settings settings) {
        this.consumer = Objects.requireNonNull(consumer, "consumer");
        this.handler = Objects.requireNonNull(handler, "handler");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        consumer.subscribe(
                java.util.List.of(settings.topic()),
                new RebalanceListener()
        );
        worker = Thread.ofPlatform()
                .daemon(true)
                .name("gateway-admin-call-event-consumer")
                .start(this::consume);
    }

    @Override
    public void stop() {
        if (!running.getAndSet(false)) {
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
                try {
                    pollAndProject();
                } catch (WakeupException wakeup) {
                    if (running.get()) {
                        recover(wakeup);
                    }
                } catch (RetriableException transientFailure) {
                    recover(transientFailure);
                } catch (RuntimeException failure) {
                    if (fatal(failure)) {
                        LOGGER.error(
                                "Gateway Kafka consumer stopped by a "
                                        + "non-recoverable failure",
                                failure
                        );
                        return;
                    }
                    recover(failure);
                }
            }
        } finally {
            running.set(false);
            consumer.close(settings.closeTimeout());
        }
    }

    private void pollAndProject() {
        ConsumerRecords<String, byte[]> records =
                consumer.poll(settings.pollTimeout());
        Map<TopicPartition, Long> nextOffsets = new HashMap<>();
        for (TopicPartition partition : records.partitions()) {
            nextOffsets.put(
                    partition,
                    records.records(partition).getFirst().offset()
            );
        }
        for (ConsumerRecord<String, byte[]> record : records) {
            if (!running.get()) {
                rewind(nextOffsets);
                return;
            }
            TopicPartition partition = new TopicPartition(
                    record.topic(),
                    record.partition()
            );
            RecordKey key = new RecordKey(
                    record.topic(),
                    record.partition(),
                    record.offset()
            );
            try {
                GatewayCallEventConsumerHandler.Result result =
                        handler.handle(record);
                commit(record, partition);
                nextOffsets.put(partition, record.offset() + 1);
                attempts.remove(key);
                metrics.observed(record.timestamp());
                if (result == GatewayCallEventConsumerHandler
                        .Result.POISON_RECORDED) {
                    metrics.deadLetter();
                }
            } catch (RuntimeException failure) {
                int attempt = attempts.merge(key, 1, Integer::sum);
                if (attempt >= settings.maxRecordAttempts()) {
                    if (deadLetter(record, partition, key, failure)) {
                        nextOffsets.put(partition, record.offset() + 1);
                        continue;
                    }
                    retry(nextOffsets);
                    return;
                }
                retry(nextOffsets);
                return;
            }
        }
    }

    private boolean deadLetter(
            ConsumerRecord<String, byte[]> record,
            TopicPartition partition,
            RecordKey key,
            RuntimeException failure) {
        try {
            handler.deadLetter(record, failure);
            commit(record, partition);
            attempts.remove(key);
            metrics.deadLetter();
            return true;
        } catch (RuntimeException deadLetterFailure) {
            LOGGER.warn(
                    "Gateway Kafka record dead-letter persistence failed",
                    deadLetterFailure
            );
            return false;
        }
    }

    private void rewind(Map<TopicPartition, Long> nextOffsets) {
        for (Map.Entry<TopicPartition, Long> entry
                : nextOffsets.entrySet()) {
            consumer.seek(entry.getKey(), entry.getValue());
        }
    }

    private void retry(Map<TopicPartition, Long> nextOffsets) {
        rewind(nextOffsets);
        consumer.pause(nextOffsets.keySet());
        metrics.retry();
        try {
            backoff();
        } finally {
            consumer.resume(nextOffsets.keySet());
        }
    }

    private void commit(
            ConsumerRecord<String, byte[]> record,
            TopicPartition partition) {
        try {
            consumer.commitSync(
                    Map.of(
                            partition,
                            new OffsetAndMetadata(record.offset() + 1)
                    ),
                    settings.commitTimeout()
            );
        } catch (RuntimeException failure) {
            consumer.seek(partition, record.offset());
            throw failure;
        }
    }

    private void recover(RuntimeException failure) {
        if (!running.get()) {
            return;
        }
        metrics.workerRestart();
        LOGGER.warn(
                "Gateway Kafka consumer recovered from a transient failure",
                failure
        );
        backoff();
    }

    private void backoff() {
        try {
            Thread.sleep(settings.retryBackoff().toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }

    private boolean fatal(RuntimeException failure) {
        return failure instanceof AuthenticationException
                || failure instanceof AuthorizationException
                || failure instanceof FencedInstanceIdException;
    }

    private static Properties properties(Settings settings) {
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

    private static String required(String value, String field) {
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
            Duration retryBackoff,
            int maxRecordAttempts,
            Map<String, Object> additionalProperties
    ) {

        public Settings(
                String bootstrapServers,
                String topic,
                String groupId,
                Duration pollTimeout,
                Duration commitTimeout,
                Duration closeTimeout,
                Map<String, Object> additionalProperties) {
            this(
                    bootstrapServers,
                    topic,
                    groupId,
                    pollTimeout,
                    commitTimeout,
                    closeTimeout,
                    Duration.ofMillis(250),
                    5,
                    additionalProperties
            );
        }

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
            retryBackoff = retryBackoff == null
                    ? Duration.ofMillis(250)
                    : retryBackoff;
            if (retryBackoff.isNegative()
                    || retryBackoff.compareTo(Duration.ofSeconds(30)) > 0) {
                throw new IllegalArgumentException(
                        "retryBackoff must be between PT0S and PT30S"
                );
            }
            if (maxRecordAttempts < 1 || maxRecordAttempts > 100) {
                throw new IllegalArgumentException(
                        "maxRecordAttempts must be between 1 and 100"
                );
            }
            additionalProperties = additionalProperties == null
                    ? Map.of()
                    : Map.copyOf(additionalProperties);
        }
    }

    private record RecordKey(String topic, int partition, long offset) {
    }

    private final class RebalanceListener
            implements ConsumerRebalanceListener {

        @Override
        public void onPartitionsRevoked(
                java.util.Collection<TopicPartition> partitions) {
            attempts.clear();
        }

        @Override
        public void onPartitionsAssigned(
                java.util.Collection<TopicPartition> partitions) {
            LOGGER.info(
                    "Gateway Kafka consumer assigned {} partitions",
                    partitions.size()
            );
        }
    }
}
