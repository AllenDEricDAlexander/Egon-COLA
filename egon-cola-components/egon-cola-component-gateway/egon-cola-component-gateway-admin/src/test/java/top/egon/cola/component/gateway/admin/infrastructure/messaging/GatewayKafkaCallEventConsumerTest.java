package top.egon.cola.component.gateway.admin.infrastructure.messaging;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayKafkaCallEventConsumerTest {

    @Test
    void keepsPollingAfterTransientConsumerFailure() throws Exception {
        Consumer<String, byte[]> consumer = consumer();
        CountDownLatch recovered = new CountDownLatch(1);
        when(consumer.poll(any(Duration.class)))
                .thenThrow(new TimeoutException("temporary"))
                .thenAnswer(invocation -> {
                    recovered.countDown();
                    return ConsumerRecords.empty();
                });
        GatewayKafkaCallEventConsumer gatewayConsumer = consumer(
                consumer,
                mock(GatewayCallEventConsumerHandler.class),
                3
        );

        gatewayConsumer.start();

        assertTrue(recovered.await(2, TimeUnit.SECONDS));
        assertTrue(gatewayConsumer.isRunning());
        gatewayConsumer.stop();
        verify(consumer, atLeast(2)).poll(any(Duration.class));
    }

    @Test
    void rewindsAllUnprocessedPartitionsBeforeRetry() throws Exception {
        Consumer<String, byte[]> consumer = consumer();
        TopicPartition first = new TopicPartition("calls", 0);
        TopicPartition second = new TopicPartition("calls", 1);
        ConsumerRecord<String, byte[]> failed =
                new ConsumerRecord<>("calls", 0, 10, "a", new byte[]{1});
        ConsumerRecord<String, byte[]> pending =
                new ConsumerRecord<>("calls", 1, 20, "b", new byte[]{2});
        ConsumerRecords<String, byte[]> batch = new ConsumerRecords<>(
                Map.of(
                        first,
                        List.of(failed),
                        second,
                        List.of(pending)
                )
        );
        CountDownLatch retried = new CountDownLatch(1);
        AtomicInteger polls = new AtomicInteger();
        when(consumer.poll(any(Duration.class))).thenAnswer(invocation -> {
            if (polls.getAndIncrement() == 0) {
                return batch;
            }
            retried.countDown();
            return ConsumerRecords.empty();
        });
        GatewayCallEventConsumerHandler handler =
                mock(GatewayCallEventConsumerHandler.class);
        when(handler.handle(any())).thenThrow(
                new IllegalStateException("database unavailable")
        );
        GatewayKafkaCallEventConsumer gatewayConsumer =
                consumer(consumer, handler, 3);

        gatewayConsumer.start();

        assertTrue(retried.await(2, TimeUnit.SECONDS));
        gatewayConsumer.stop();
        verify(consumer).seek(first, 10);
        verify(consumer).seek(second, 20);
    }

    @Test
    void persistsDeadLetterBeforeCommittingPoisonOffset()
            throws Exception {
        Consumer<String, byte[]> consumer = consumer();
        TopicPartition partition = new TopicPartition("calls", 0);
        ConsumerRecord<String, byte[]> record =
                new ConsumerRecord<>("calls", 0, 10, "a", new byte[]{1});
        ConsumerRecords<String, byte[]> batch = new ConsumerRecords<>(
                Map.of(partition, List.of(record))
        );
        CountDownLatch committed = new CountDownLatch(1);
        AtomicInteger polls = new AtomicInteger();
        when(consumer.poll(any(Duration.class))).thenAnswer(invocation ->
                polls.getAndIncrement() < 2
                        ? batch
                        : ConsumerRecords.empty()
        );
        doAnswer(invocation -> {
            committed.countDown();
            return null;
        }).when(consumer).commitSync(
                any(Map.class),
                any(Duration.class)
        );
        GatewayCallEventConsumerHandler handler =
                mock(GatewayCallEventConsumerHandler.class);
        when(handler.handle(any())).thenThrow(
                new IllegalStateException("projection failed")
        );
        GatewayKafkaCallEventConsumer gatewayConsumer =
                consumer(consumer, handler, 2);

        gatewayConsumer.start();

        assertTrue(committed.await(2, TimeUnit.SECONDS));
        gatewayConsumer.stop();
        verify(handler, times(2)).handle(any());
        verify(handler).deadLetter(eq(record), any());
        verify(consumer).commitSync(
                eq(Map.of(
                        partition,
                        new OffsetAndMetadata(record.offset() + 1)
                )),
                any(Duration.class)
        );
    }

    @SuppressWarnings("unchecked")
    private Consumer<String, byte[]> consumer() {
        return mock(Consumer.class);
    }

    private GatewayKafkaCallEventConsumer consumer(
            Consumer<String, byte[]> kafkaConsumer,
            GatewayCallEventConsumerHandler handler,
            int maxRecordAttempts) {
        return new GatewayKafkaCallEventConsumer(
                kafkaConsumer,
                handler,
                new GatewayKafkaConsumerMetrics(
                        new SimpleMeterRegistry(),
                        Clock.systemUTC()
                ),
                new GatewayKafkaCallEventConsumer.Settings(
                        "localhost:9092",
                        "calls",
                        "gateway-admin",
                        Duration.ofMillis(1),
                        Duration.ofMillis(10),
                        Duration.ofMillis(100),
                        Duration.ofMillis(1),
                        maxRecordAttempts,
                        Map.of()
                )
        );
    }
}
