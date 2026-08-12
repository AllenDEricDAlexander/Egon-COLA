package top.egon.cola.component.gateway.admin.infrastructure.messaging;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 中文说明：{@code GatewayKafkaConsumerMetrics} 是类型，位于当前 Gateway 模块的相关包中，负责网关Kafka消费者Metrics相关的职责与边界。
 * English summary: {@code GatewayKafkaConsumerMetrics} is a type in the current Gateway module; it owns the gateway kafka consumer metrics-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayKafkaConsumerMetrics {

    /**
     * 中文说明：保存 retries 对应的状态、依赖或配置值；字段类型为 {@code Counter}，由 {@code GatewayKafkaConsumerMetrics} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by retries; its type is {@code Counter}, and {@code GatewayKafkaConsumerMetrics} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayKafkaConsumerMetrics} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayKafkaConsumerMetrics}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Counter retries;

    /**
     * 中文说明：保存 deadLetters 对应的状态、依赖或配置值；字段类型为 {@code Counter}，由 {@code GatewayKafkaConsumerMetrics} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by dead letters; its type is {@code Counter}, and {@code GatewayKafkaConsumerMetrics} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayKafkaConsumerMetrics} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayKafkaConsumerMetrics}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Counter deadLetters;

    /**
     * 中文说明：保存 workerRestarts 对应的状态、依赖或配置值；字段类型为 {@code Counter}，由 {@code GatewayKafkaConsumerMetrics} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by worker restarts; its type is {@code Counter}, and {@code GatewayKafkaConsumerMetrics} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayKafkaConsumerMetrics} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayKafkaConsumerMetrics}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Counter workerRestarts;

    /**
     * 中文说明：保存 事件LagMs 对应的状态、依赖或配置值；字段类型为 {@code AtomicLong}，由 {@code GatewayKafkaConsumerMetrics} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by event lag ms; its type is {@code AtomicLong}, and {@code GatewayKafkaConsumerMetrics} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayKafkaConsumerMetrics} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayKafkaConsumerMetrics}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicLong eventLagMs = new AtomicLong();

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayKafkaConsumerMetrics} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayKafkaConsumerMetrics} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayKafkaConsumerMetrics} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayKafkaConsumerMetrics}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：创建 {@code GatewayKafkaConsumerMetrics} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayKafkaConsumerMetrics} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param registry 参数 注册表；parameter registry。
     * @param clock 参数 clock；parameter clock。
     */
    public GatewayKafkaConsumerMetrics(
            MeterRegistry registry,
            Clock clock) {
        this.clock = clock;
        retries = registry.counter(
                "gateway.admin.kafka.consumer.retries"
        );
        deadLetters = registry.counter(
                "gateway.admin.kafka.consumer.dead.letters"
        );
        workerRestarts = registry.counter(
                "gateway.admin.kafka.consumer.worker.restarts"
        );
        Gauge.builder(
                        "gateway.admin.kafka.consumer.event.lag",
                        eventLagMs,
                        AtomicLong::get
                )
                .baseUnit("milliseconds")
                .register(registry);
    }

    /**
     * 中文说明：执行 重试 操作；该方法是 {@code GatewayKafkaConsumerMetrics} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the retry operation; this method is the invocation entry point on {@code GatewayKafkaConsumerMetrics} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaConsumerMetrics.retry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    void retry() {
        retries.increment();
    }

    /**
     * 中文说明：执行 deadLetter 操作；该方法是 {@code GatewayKafkaConsumerMetrics} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dead letter operation; this method is the invocation entry point on {@code GatewayKafkaConsumerMetrics} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaConsumerMetrics.deadLetter(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    void deadLetter() {
        deadLetters.increment();
    }

    /**
     * 中文说明：执行 workerRestart 操作；该方法是 {@code GatewayKafkaConsumerMetrics} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the worker restart operation; this method is the invocation entry point on {@code GatewayKafkaConsumerMetrics} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaConsumerMetrics.workerRestart(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    void workerRestart() {
        workerRestarts.increment();
    }

    /**
     * 中文说明：执行 observed 操作；该方法是 {@code GatewayKafkaConsumerMetrics} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the observed operation; this method is the invocation entry point on {@code GatewayKafkaConsumerMetrics} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayKafkaConsumerMetrics.observed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param recordTimestamp 参数 recordTimestamp；parameter record timestamp。
     */
    void observed(long recordTimestamp) {
        if (recordTimestamp < 0) {
            return;
        }
        eventLagMs.set(Math.max(
                0,
                clock.millis() - recordTimestamp
        ));
    }
}
