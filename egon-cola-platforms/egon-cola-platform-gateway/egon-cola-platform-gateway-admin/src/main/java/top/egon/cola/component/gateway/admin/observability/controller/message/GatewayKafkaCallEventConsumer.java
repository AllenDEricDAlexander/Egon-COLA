package top.egon.cola.component.gateway.admin.observability.controller.message;


import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
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
import top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayKafkaConsumerSettingsDTO;
import top.egon.cola.component.gateway.admin.observability.domain.enums.GatewayCallEventConsumeResultEnum;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 中文说明：{@code GatewayKafkaCallEventConsumer} 是类型，位于当前 Gateway 模块的相关包中，负责网关Kafka调用事件消费者相关的职责与边界。
 * English summary: {@code GatewayKafkaCallEventConsumer} is a type in the current Gateway module; it owns the gateway kafka call event consumer-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayKafkaCallEventConsumer
        implements SmartLifecycle {

    /**
     * 中文说明：表示 LOGGER 这一固定值；它属于 {@code GatewayKafkaCallEventConsumer} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value logger; it is a state, type, or protocol value of {@code GatewayKafkaCallEventConsumer} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayKafkaCallEventConsumer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayKafkaCallEventConsumer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            GatewayKafkaCallEventConsumer.class
    );

    /**
     * 中文说明：保存 消费者 对应的状态、依赖或配置值；字段类型为 {@code Consumer<String, byte[]>}，由 {@code GatewayKafkaCallEventConsumer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by consumer; its type is {@code Consumer<String, byte[]>}, and {@code GatewayKafkaCallEventConsumer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayKafkaCallEventConsumer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayKafkaCallEventConsumer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Consumer<String, byte[]> consumer;

    /**
     * 中文说明：保存 处理器 对应的状态、依赖或配置值；字段类型为 {@code GatewayCallEventConsumerHandler}，由 {@code GatewayKafkaCallEventConsumer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by handler; its type is {@code GatewayCallEventConsumerHandler}, and {@code GatewayKafkaCallEventConsumer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayKafkaCallEventConsumer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayKafkaCallEventConsumer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCallEventConsumerHandler handler;

    /**
     * 中文说明：保存 metrics 对应的状态、依赖或配置值；字段类型为 {@code GatewayKafkaConsumerMetrics}，由 {@code GatewayKafkaCallEventConsumer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by metrics; its type is {@code GatewayKafkaConsumerMetrics}, and {@code GatewayKafkaCallEventConsumer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayKafkaCallEventConsumer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayKafkaCallEventConsumer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayKafkaConsumerMetrics metrics;

    /**
     * 中文说明：保存 settings 对应的状态、依赖或配置值；字段类型为 {@code GatewayKafkaConsumerSettingsDTO}，由 {@code GatewayKafkaCallEventConsumer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by settings; its type is {@code GatewayKafkaConsumerSettingsDTO}, and {@code GatewayKafkaCallEventConsumer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayKafkaCallEventConsumer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayKafkaCallEventConsumer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayKafkaConsumerSettingsDTO settings;

    /**
     * 中文说明：保存 running 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code GatewayKafkaCallEventConsumer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by running; its type is {@code AtomicBoolean}, and {@code GatewayKafkaCallEventConsumer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayKafkaCallEventConsumer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayKafkaCallEventConsumer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicBoolean running = new AtomicBoolean();

    /**
     * 中文说明：保存 attempts 对应的状态、依赖或配置值；字段类型为 {@code Map<GatewayKafkaRecordKey, Integer>}，由 {@code GatewayKafkaCallEventConsumer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by attempts; its type is {@code Map<GatewayKafkaRecordKey, Integer>}, and {@code GatewayKafkaCallEventConsumer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayKafkaCallEventConsumer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayKafkaCallEventConsumer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<GatewayKafkaRecordKey, Integer> attempts = new HashMap<>();

    /**
     * 中文说明：保存 worker 对应的状态、依赖或配置值；字段类型为 {@code Thread}，由 {@code GatewayKafkaCallEventConsumer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by worker; its type is {@code Thread}, and {@code GatewayKafkaCallEventConsumer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayKafkaCallEventConsumer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayKafkaCallEventConsumer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private Thread worker;

    /**
     * 中文说明：创建 {@code GatewayKafkaCallEventConsumer} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayKafkaCallEventConsumer} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param handler 参数 处理器；parameter handler。
     * @param metrics 参数 metrics；parameter metrics。
     * @param settings 参数 settings；parameter settings。
     */
    public GatewayKafkaCallEventConsumer(
            GatewayCallEventConsumerHandler handler,
            GatewayKafkaConsumerMetrics metrics,
            GatewayKafkaConsumerSettingsDTO settings) {
        this(
                new KafkaConsumer<>(properties(settings)),
                handler,
                metrics,
                settings
        );
    }

    /**
     * 中文说明：创建 {@code GatewayKafkaCallEventConsumer} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayKafkaCallEventConsumer} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param consumer 参数 消费者；parameter consumer。
     * @param handler 参数 处理器；parameter handler。
     * @param metrics 参数 metrics；parameter metrics。
     * @param settings 参数 settings；parameter settings。
     */
    GatewayKafkaCallEventConsumer(
            Consumer<String, byte[]> consumer,
            GatewayCallEventConsumerHandler handler,
            GatewayKafkaConsumerMetrics metrics,
            GatewayKafkaConsumerSettingsDTO settings) {
        this.consumer = Objects.requireNonNull(consumer, "consumer");
        this.handler = Objects.requireNonNull(handler, "handler");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    /**
     * 中文说明：执行 start 操作；该方法是 {@code GatewayKafkaCallEventConsumer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start operation; this method is the invocation entry point on {@code GatewayKafkaCallEventConsumer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaCallEventConsumer.start(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        consumer.subscribe(
                java.util.List.of(settings.topic()),
                new GatewayKafkaRebalanceListener(attempts)
        );
        worker = Thread.ofPlatform()
                .daemon(true)
                .name("gateway-admin-call-event-consumer")
                .start(this::consume);
    }

    /**
     * 中文说明：执行 stop 操作；该方法是 {@code GatewayKafkaCallEventConsumer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the stop operation; this method is the invocation entry point on {@code GatewayKafkaCallEventConsumer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaCallEventConsumer.stop(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
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

    /**
     * 中文说明：执行 isRunning 操作；该方法是 {@code GatewayKafkaCallEventConsumer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the is running operation; this method is the invocation entry point on {@code GatewayKafkaCallEventConsumer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaCallEventConsumer.isRunning(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 isRunning 的处理结果；returns the result of the operation.
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 中文说明：执行 getPhase 操作；该方法是 {@code GatewayKafkaCallEventConsumer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get phase operation; this method is the invocation entry point on {@code GatewayKafkaCallEventConsumer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaCallEventConsumer.getPhase(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getPhase 的处理结果；returns the result of the operation.
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    /**
     * 中文说明：执行 consume 操作；该方法是 {@code GatewayKafkaCallEventConsumer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the consume operation; this method is the invocation entry point on {@code GatewayKafkaCallEventConsumer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaCallEventConsumer.consume(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
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

    /**
     * 中文说明：执行 pollAndProject 操作；该方法是 {@code GatewayKafkaCallEventConsumer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the poll and project operation; this method is the invocation entry point on {@code GatewayKafkaCallEventConsumer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaCallEventConsumer.pollAndProject(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
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
            GatewayKafkaRecordKey key = new GatewayKafkaRecordKey(
                    record.topic(),
                    record.partition(),
                    record.offset()
            );
            try {
                top.egon.cola.component.gateway.admin.observability.domain.enums.GatewayCallEventConsumeResultEnum result =
                        handler.handle(record);
                commit(record, partition);
                nextOffsets.put(partition, record.offset() + 1);
                attempts.remove(key);
                metrics.observed(record.timestamp());
                if (result == GatewayCallEventConsumeResultEnum.POISON_RECORDED) {
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

    /**
     * 中文说明：执行 deadLetter 操作；该方法是 {@code GatewayKafkaCallEventConsumer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dead letter operation; this method is the invocation entry point on {@code GatewayKafkaCallEventConsumer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaCallEventConsumer.deadLetter(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param record 参数 record；parameter record。
     * @param partition 参数 partition；parameter partition。
     * @param key 参数 键；parameter key。
     * @param failure 参数 failure；parameter failure。
     * @return 返回 deadLetter 的处理结果；returns the result of the operation.
     */
    private boolean deadLetter(
            ConsumerRecord<String, byte[]> record,
            TopicPartition partition,
            GatewayKafkaRecordKey key,
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

    /**
     * 中文说明：执行 rewind 操作；该方法是 {@code GatewayKafkaCallEventConsumer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rewind operation; this method is the invocation entry point on {@code GatewayKafkaCallEventConsumer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaCallEventConsumer.rewind(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param nextOffsets 参数 nextOffsets；parameter next offsets。
     */
    private void rewind(Map<TopicPartition, Long> nextOffsets) {
        for (Map.Entry<TopicPartition, Long> entry
                : nextOffsets.entrySet()) {
            consumer.seek(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 中文说明：执行 重试 操作；该方法是 {@code GatewayKafkaCallEventConsumer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the retry operation; this method is the invocation entry point on {@code GatewayKafkaCallEventConsumer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaCallEventConsumer.retry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param nextOffsets 参数 nextOffsets；parameter next offsets。
     */
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

    /**
     * 中文说明：执行 commit 操作；该方法是 {@code GatewayKafkaCallEventConsumer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the commit operation; this method is the invocation entry point on {@code GatewayKafkaCallEventConsumer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaCallEventConsumer.commit(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param record 参数 record；parameter record。
     * @param partition 参数 partition；parameter partition。
     */
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

    /**
     * 中文说明：执行 recover 操作；该方法是 {@code GatewayKafkaCallEventConsumer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the recover operation; this method is the invocation entry point on {@code GatewayKafkaCallEventConsumer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaCallEventConsumer.recover(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param failure 参数 failure；parameter failure。
     */
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

    /**
     * 中文说明：执行 backoff 操作；该方法是 {@code GatewayKafkaCallEventConsumer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the backoff operation; this method is the invocation entry point on {@code GatewayKafkaCallEventConsumer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaCallEventConsumer.backoff(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    private void backoff() {
        try {
            Thread.sleep(settings.retryBackoff().toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }

    /**
     * 中文说明：执行 fatal 操作；该方法是 {@code GatewayKafkaCallEventConsumer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the fatal operation; this method is the invocation entry point on {@code GatewayKafkaCallEventConsumer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaCallEventConsumer.fatal(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param failure 参数 failure；parameter failure。
     * @return 返回 fatal 的处理结果；returns the result of the operation.
     */
    private boolean fatal(RuntimeException failure) {
        return failure instanceof AuthenticationException
                || failure instanceof AuthorizationException
                || failure instanceof FencedInstanceIdException;
    }

    /**
     * 中文说明：执行 properties 操作；该方法是 {@code GatewayKafkaCallEventConsumer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the properties operation; this method is the invocation entry point on {@code GatewayKafkaCallEventConsumer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaCallEventConsumer.properties(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param settings 参数 settings；parameter settings。
     * @return 返回 properties 的处理结果；returns the result of the operation.
     */
    private static Properties properties(GatewayKafkaConsumerSettingsDTO settings) {
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

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayKafkaCallEventConsumer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayKafkaCallEventConsumer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaCallEventConsumer.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }






}
