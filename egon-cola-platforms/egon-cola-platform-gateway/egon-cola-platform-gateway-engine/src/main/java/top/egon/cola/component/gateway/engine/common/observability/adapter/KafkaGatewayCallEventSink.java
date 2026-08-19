package top.egon.cola.component.gateway.engine.common.observability.adapter;

import top.egon.cola.component.gateway.engine.common.observability.domain.GatewayCallEventSink;
import top.egon.cola.component.gateway.engine.common.observability.domain.GatewayTelemetry;

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

/**
 * 中文说明：{@code KafkaGatewayCallEventSink} 是类型，位于当前 Gateway 模块的相关包中，负责Kafka网关调用事件Sink相关的职责与边界。
 * English summary: {@code KafkaGatewayCallEventSink} is a type in the current Gateway module; it owns the kafka gateway call event sink-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class KafkaGatewayCallEventSink
        implements GatewayCallEventSink {

    /**
     * 中文说明：保存 producer 对应的状态、依赖或配置值；字段类型为 {@code Producer<String, byte[]>}，由 {@code KafkaGatewayCallEventSink} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by producer; its type is {@code Producer<String, byte[]>}, and {@code KafkaGatewayCallEventSink} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code KafkaGatewayCallEventSink} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code KafkaGatewayCallEventSink}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Producer<String, byte[]> producer;

    /**
     * 中文说明：保存 topic 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code KafkaGatewayCallEventSink} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by topic; its type is {@code String}, and {@code KafkaGatewayCallEventSink} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code KafkaGatewayCallEventSink} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code KafkaGatewayCallEventSink}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String topic;

    /**
     * 中文说明：保存 close超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code KafkaGatewayCallEventSink} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by close timeout; its type is {@code Duration}, and {@code KafkaGatewayCallEventSink} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code KafkaGatewayCallEventSink} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code KafkaGatewayCallEventSink}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration closeTimeout;

    /**
     * 中文说明：保存 遥测 对应的状态、依赖或配置值；字段类型为 {@code GatewayTelemetry}，由 {@code KafkaGatewayCallEventSink} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by telemetry; its type is {@code GatewayTelemetry}, and {@code KafkaGatewayCallEventSink} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code KafkaGatewayCallEventSink} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code KafkaGatewayCallEventSink}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayTelemetry telemetry;

    /**
     * 中文说明：保存 acknowledged 对应的状态、依赖或配置值；字段类型为 {@code AtomicLong}，由 {@code KafkaGatewayCallEventSink} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by acknowledged; its type is {@code AtomicLong}, and {@code KafkaGatewayCallEventSink} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code KafkaGatewayCallEventSink} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code KafkaGatewayCallEventSink}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicLong acknowledged = new AtomicLong();

    /**
     * 中文说明：保存 failed 对应的状态、依赖或配置值；字段类型为 {@code AtomicLong}，由 {@code KafkaGatewayCallEventSink} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by failed; its type is {@code AtomicLong}, and {@code KafkaGatewayCallEventSink} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code KafkaGatewayCallEventSink} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code KafkaGatewayCallEventSink}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicLong failed = new AtomicLong();

    /**
     * 中文说明：创建 {@code KafkaGatewayCallEventSink} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code KafkaGatewayCallEventSink} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param settings 参数 settings；parameter settings。
     */
    public KafkaGatewayCallEventSink(Settings settings) {
        this(settings, createProducer(settings), GatewayTelemetry.noop());
    }

    /**
     * 中文说明：创建 {@code KafkaGatewayCallEventSink} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code KafkaGatewayCallEventSink} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param settings 参数 settings；parameter settings。
     * @param telemetry 参数 遥测；parameter telemetry。
     */
    public KafkaGatewayCallEventSink(
            Settings settings,
            GatewayTelemetry telemetry) {
        this(settings, createProducer(settings), telemetry);
    }

    /**
     * 中文说明：创建 {@code KafkaGatewayCallEventSink} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code KafkaGatewayCallEventSink} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param settings 参数 settings；parameter settings。
     * @param producer 参数 producer；parameter producer。
     */
    KafkaGatewayCallEventSink(
            Settings settings,
            Producer<String, byte[]> producer) {
        this(settings, producer, GatewayTelemetry.noop());
    }

    /**
     * 中文说明：创建 {@code KafkaGatewayCallEventSink} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code KafkaGatewayCallEventSink} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param settings 参数 settings；parameter settings。
     * @param producer 参数 producer；parameter producer。
     * @param telemetry 参数 遥测；parameter telemetry。
     */
    KafkaGatewayCallEventSink(
            Settings settings,
            Producer<String, byte[]> producer,
            GatewayTelemetry telemetry) {
        this.producer = Objects.requireNonNull(producer, "producer");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
        topic = required(settings.topic(), "topic");
        closeTimeout = Objects.requireNonNull(
                settings.closeTimeout(),
                "closeTimeout"
        );
    }

    /**
     * 中文说明：执行 send 操作；该方法是 {@code KafkaGatewayCallEventSink} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the send operation; this method is the invocation entry point on {@code KafkaGatewayCallEventSink} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code KafkaGatewayCallEventSink.send(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     * @param payload 参数 payload；parameter payload。
     */
    @Override
    public void send(GatewayCallEventV1 event, byte[] payload) {
        GatewayTelemetry.Operation operation = telemetry.startKafkaSend(
                event.eventId(),
                event.trace().traceId()
        );
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
        try {
            producer.send(record, (metadata, error) -> {
                if (error == null) {
                    acknowledged.incrementAndGet();
                    operation.success();
                } else {
                    failed.incrementAndGet();
                    operation.failure(error);
                }
            });
        } catch (RuntimeException failure) {
            operation.failure(failure);
            throw failure;
        }
    }

    /**
     * 中文说明：执行 delivery 操作；该方法是 {@code KafkaGatewayCallEventSink} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delivery operation; this method is the invocation entry point on {@code KafkaGatewayCallEventSink} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code KafkaGatewayCallEventSink.delivery(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 delivery 的处理结果；returns the result of the operation.
     */
    public Delivery delivery() {
        return new Delivery(acknowledged.get(), failed.get());
    }

    /**
     * 中文说明：执行 close 操作；该方法是 {@code KafkaGatewayCallEventSink} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code KafkaGatewayCallEventSink} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code KafkaGatewayCallEventSink.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public void close() {
        producer.close(closeTimeout);
    }

    /**
     * 中文说明：执行 createProducer 操作；该方法是 {@code KafkaGatewayCallEventSink} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the create producer operation; this method is the invocation entry point on {@code KafkaGatewayCallEventSink} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code KafkaGatewayCallEventSink.createProducer(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param settings 参数 settings；parameter settings。
     * @return 返回 createProducer 的处理结果；returns the result of the operation.
     */
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
        int deliveryTimeoutMillis = Math.toIntExact(
                settings.deliveryTimeout().toMillis()
        );
        properties.put(
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                deliveryTimeoutMillis
        );
        properties.put(
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
                deliveryTimeoutMillis
        );
        properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        properties.putAll(settings.additionalProperties());
        return new KafkaProducer<>(properties);
    }

    /**
     * 中文说明：执行 header 操作；该方法是 {@code KafkaGatewayCallEventSink} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the header operation; this method is the invocation entry point on {@code KafkaGatewayCallEventSink} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code KafkaGatewayCallEventSink.header(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param name 参数 name；parameter name。
     * @param value 参数 值；parameter value。
     * @return 返回 header 的处理结果；returns the result of the operation.
     */
    private static RecordHeader header(String name, String value) {
        return new RecordHeader(
                name,
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code KafkaGatewayCallEventSink} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code KafkaGatewayCallEventSink} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code KafkaGatewayCallEventSink.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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

    /**
     * 中文说明：{@code Settings} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Settings相关的职责与边界。
     * English summary: {@code Settings} is an immutable data carrier in the current Gateway module; it owns the settings-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param bootstrapServers 参数 bootstrapServers；parameter bootstrap servers。
     * @param topic 参数 topic；parameter topic。
     * @param deliveryTimeout 参数 delivery超时；parameter delivery timeout。
     * @param closeTimeout 参数 close超时；parameter close timeout。
     * @param additionalProperties 参数 additionalProperties；parameter additional properties。
     */
    public record Settings(
            /**
             * 中文说明：保存 bootstrapServers 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code KafkaGatewayCallEventSink.Settings} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by bootstrap servers; its type is {@code String}, and {@code KafkaGatewayCallEventSink.Settings} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code KafkaGatewayCallEventSink.Settings} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code KafkaGatewayCallEventSink.Settings}; do not couple callers to its representation when the owning type exposes an API.
             */
            String bootstrapServers,
            /**
             * 中文说明：保存 topic 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code KafkaGatewayCallEventSink.Settings} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by topic; its type is {@code String}, and {@code KafkaGatewayCallEventSink.Settings} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code KafkaGatewayCallEventSink.Settings} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code KafkaGatewayCallEventSink.Settings}; do not couple callers to its representation when the owning type exposes an API.
             */
            String topic,
            /**
             * 中文说明：保存 delivery超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code KafkaGatewayCallEventSink.Settings} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by delivery timeout; its type is {@code Duration}, and {@code KafkaGatewayCallEventSink.Settings} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code KafkaGatewayCallEventSink.Settings} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code KafkaGatewayCallEventSink.Settings}; do not couple callers to its representation when the owning type exposes an API.
             */
            Duration deliveryTimeout,
            /**
             * 中文说明：保存 close超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code KafkaGatewayCallEventSink.Settings} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by close timeout; its type is {@code Duration}, and {@code KafkaGatewayCallEventSink.Settings} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code KafkaGatewayCallEventSink.Settings} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code KafkaGatewayCallEventSink.Settings}; do not couple callers to its representation when the owning type exposes an API.
             */
            Duration closeTimeout,
            /**
             * 中文说明：保存 additionalProperties 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code KafkaGatewayCallEventSink.Settings} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by additional properties; its type is {@code Map<String, Object>}, and {@code KafkaGatewayCallEventSink.Settings} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code KafkaGatewayCallEventSink.Settings} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code KafkaGatewayCallEventSink.Settings}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> additionalProperties
    ) {

        /**
         * 中文说明：创建 {@code KafkaGatewayCallEventSink.Settings} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code KafkaGatewayCallEventSink.Settings} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param bootstrapServers 参数 bootstrapServers；parameter bootstrap servers。
         * @param topic 参数 topic；parameter topic。
         * @param deliveryTimeout 参数 delivery超时；parameter delivery timeout。
         * @param closeTimeout 参数 close超时；parameter close timeout。
         * @param additionalProperties 参数 additionalProperties；parameter additional properties。
         */
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

    /**
     * 中文说明：{@code Delivery} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Delivery相关的职责与边界。
     * English summary: {@code Delivery} is an immutable data carrier in the current Gateway module; it owns the delivery-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param acknowledged 参数 acknowledged；parameter acknowledged。
     * @param failed 参数 failed；parameter failed。
     */
    public record Delivery(
    /**
     * 中文说明：保存 acknowledged 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code KafkaGatewayCallEventSink.Delivery} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by acknowledged; its type is {@code long}, and {@code KafkaGatewayCallEventSink.Delivery} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code KafkaGatewayCallEventSink.Delivery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code KafkaGatewayCallEventSink.Delivery}; do not couple callers to its representation when the owning type exposes an API.
     */
    long acknowledged,
    /**
     * 中文说明：保存 failed 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code KafkaGatewayCallEventSink.Delivery} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by failed; its type is {@code long}, and {@code KafkaGatewayCallEventSink.Delivery} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code KafkaGatewayCallEventSink.Delivery} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code KafkaGatewayCallEventSink.Delivery}; do not couple callers to its representation when the owning type exposes an API.
     */
    long failed) {
    }
}
