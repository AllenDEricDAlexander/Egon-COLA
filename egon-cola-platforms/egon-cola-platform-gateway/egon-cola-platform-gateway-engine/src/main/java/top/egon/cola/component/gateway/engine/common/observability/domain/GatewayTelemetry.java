package top.egon.cola.component.gateway.engine.common.observability.domain;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.transport.Kind;
import io.micrometer.observation.transport.ReceiverContext;
import io.micrometer.observation.transport.SenderContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.handler.TracingObservationHandler;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.DoubleSupplier;

/**
 * Observation facade for the data-plane telemetry boundaries.
 * 补充说明 / Supplementary summary: {@code GatewayTelemetry} 是类型，位于当前 Gateway 模块的相关包中，负责网关遥测相关的职责与边界。
 * English supplement: {@code GatewayTelemetry} is a type in the current Gateway module; it owns the gateway telemetry-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayTelemetry {

    /**
     * 中文说明：表示 请求 这一固定值；它属于 {@code GatewayTelemetry} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value request; it is a state, type, or protocol value of {@code GatewayTelemetry} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTelemetry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String REQUEST = "gateway.engine.request";

    /**
     * 中文说明：表示 ATTEMPT 这一固定值；它属于 {@code GatewayTelemetry} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value attempt; it is a state, type, or protocol value of {@code GatewayTelemetry} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTelemetry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String ATTEMPT =
            "gateway.engine.provider.attempt";

    /**
     * 中文说明：表示 DDCAPPLY 这一固定值；它属于 {@code GatewayTelemetry} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value ddc apply; it is a state, type, or protocol value of {@code GatewayTelemetry} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTelemetry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String DDC_APPLY = "gateway.engine.ddc.apply";

    /**
     * 中文说明：表示 KAFKASEND 这一固定值；它属于 {@code GatewayTelemetry} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value kafka send; it is a state, type, or protocol value of {@code GatewayTelemetry} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTelemetry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final String KAFKA_SEND = "gateway.engine.kafka.send";

    /**
     * 中文说明：保存 注册表 对应的状态、依赖或配置值；字段类型为 {@code ObservationRegistry}，由 {@code GatewayTelemetry} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by registry; its type is {@code ObservationRegistry}, and {@code GatewayTelemetry} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTelemetry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObservationRegistry registry;

    /**
     * 中文说明：保存 samplingProbability 对应的状态、依赖或配置值；字段类型为 {@code double}，由 {@code GatewayTelemetry} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by sampling probability; its type is {@code double}, and {@code GatewayTelemetry} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTelemetry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final double samplingProbability;

    /**
     * 中文说明：保存 random 对应的状态、依赖或配置值；字段类型为 {@code DoubleSupplier}，由 {@code GatewayTelemetry} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by random; its type is {@code DoubleSupplier}, and {@code GatewayTelemetry} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayTelemetry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final DoubleSupplier random;

    /**
     * 中文说明：创建 {@code GatewayTelemetry} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayTelemetry} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param registry 参数 注册表；parameter registry。
     */
    public GatewayTelemetry(ObservationRegistry registry) {
        this(registry, 0.1);
    }

    /**
     * 中文说明：创建 {@code GatewayTelemetry} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayTelemetry} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param registry 参数 注册表；parameter registry。
     * @param samplingProbability 参数 samplingProbability；parameter sampling probability。
     */
    public GatewayTelemetry(
            ObservationRegistry registry,
            double samplingProbability) {
        this(
                registry,
                samplingProbability,
                () -> ThreadLocalRandom.current().nextDouble()
        );
    }

    /**
     * 中文说明：创建 {@code GatewayTelemetry} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayTelemetry} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param registry 参数 注册表；parameter registry。
     * @param samplingProbability 参数 samplingProbability；parameter sampling probability。
     * @param random 参数 random；parameter random。
     */
    GatewayTelemetry(
            ObservationRegistry registry,
            double samplingProbability,
            DoubleSupplier random) {
        this.registry = Objects.requireNonNull(registry, "registry");
        if (samplingProbability < 0 || samplingProbability > 1) {
            throw new IllegalArgumentException(
                    "samplingProbability must be between 0 and 1"
            );
        }
        this.samplingProbability = samplingProbability;
        this.random = Objects.requireNonNull(random, "random");
    }

    /**
     * 中文说明：执行 noop 操作；该方法是 {@code GatewayTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the noop operation; this method is the invocation entry point on {@code GatewayTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.noop(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 noop 的处理结果；returns the result of the operation.
     */
    public static GatewayTelemetry noop() {
        return new GatewayTelemetry(
                ObservationRegistry.NOOP,
                0,
                () -> 1
        );
    }

    /**
     * 中文说明：执行 start请求 操作；该方法是 {@code GatewayTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start request operation; this method is the invocation entry point on {@code GatewayTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.startRequest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param selectedTrace 参数 selectedTrace；parameter selected trace。
     * @param protocol 参数 protocol；parameter protocol。
     * @param accessZone 参数 accessZone；parameter access zone。
     * @return 返回 start请求 的处理结果；returns the result of the operation.
     */
    public Request startRequest(
            GatewayTraceContext selectedTrace,
            String protocol,
            String accessZone) {
        Map<String, String> carrier = inboundCarrier(selectedTrace);
        ReceiverContext<Map<String, String>> context =
                new ReceiverContext<>(
                        (source, name) -> source.get(name),
                        Kind.SERVER
                );
        context.setCarrier(carrier);
        Observation observation = Observation.createNotStarted(
                        REQUEST,
                        () -> context,
                        registry
                )
                .contextualName("gateway " + protocol.toLowerCase())
                .lowCardinalityKeyValue("gateway.protocol", protocol)
                .lowCardinalityKeyValue(
                        "gateway.access.zone",
                        accessZone
                )
                .highCardinalityKeyValue(
                        "gateway.trace.id",
                        selectedTrace.traceId()
                )
                .start();
        return new Request(
                observation,
                effectiveTrace(context, selectedTrace),
                protocol
        );
    }

    /**
     * 中文说明：执行 startDdcApply 操作；该方法是 {@code GatewayTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start ddc apply operation; this method is the invocation entry point on {@code GatewayTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.startDdcApply(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param key 参数 键；parameter key。
     * @param version 参数 version；parameter version。
     * @return 返回 startDdcApply 的处理结果；returns the result of the operation.
     */
    public Operation startDdcApply(String key, long version) {
        return operation(DDC_APPLY)
                .low("gateway.operation", "ddc.apply")
                .high("ddc.config.key", key)
                .high("ddc.config.version", Long.toString(version))
                .start();
    }

    /**
     * 中文说明：执行 startKafkaSend 操作；该方法是 {@code GatewayTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start kafka send operation; this method is the invocation entry point on {@code GatewayTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.startKafkaSend(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param eventId 参数 事件Id；parameter event id。
     * @param traceId 参数 traceId；parameter trace id。
     * @return 返回 startKafkaSend 的处理结果；returns the result of the operation.
     */
    public Operation startKafkaSend(
            String eventId,
            String traceId) {
        return operation(KAFKA_SEND)
                .low("gateway.operation", "kafka.send")
                .low("messaging.system", "kafka")
                .high("messaging.message.id", eventId)
                .high("gateway.trace.id", traceId)
                .start();
    }

    /**
     * 中文说明：执行 操作 操作；该方法是 {@code GatewayTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the operation operation; this method is the invocation entry point on {@code GatewayTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.operation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param name 参数 name；parameter name。
     * @return 返回 操作 的处理结果；returns the result of the operation.
     */
    private Operation operation(String name) {
        return new Operation(
                Observation.createNotStarted(name, registry)
        );
    }

    /**
     * 中文说明：执行 inboundCarrier 操作；该方法是 {@code GatewayTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the inbound carrier operation; this method is the invocation entry point on {@code GatewayTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.inboundCarrier(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param trace 参数 trace；parameter trace。
     * @return 返回 inboundCarrier 的处理结果；returns the result of the operation.
     */
    private Map<String, String> inboundCarrier(
            GatewayTraceContext trace) {
        String parentId = trace.parentSpanId() == null
                ? trace.engineSpanId()
                : trace.parentSpanId();
        String flags = trace.source()
                == GatewayTraceContext.Source.TRACEPARENT
                ? trace.traceFlags()
                : random.getAsDouble() < samplingProbability
                ? "01"
                : "00";
        Map<String, String> carrier = new LinkedHashMap<>();
        carrier.put(
                "traceparent",
                "00-"
                        + trace.traceId()
                        + "-"
                        + parentId
                        + "-"
                        + flags
        );
        if (trace.tracestate() != null) {
            carrier.put("tracestate", trace.tracestate());
        }
        return carrier;
    }

    /**
     * 中文说明：执行 effectiveTrace 操作；该方法是 {@code GatewayTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the effective trace operation; this method is the invocation entry point on {@code GatewayTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.effectiveTrace(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @param fallback 参数 fallback；parameter fallback。
     * @return 返回 effectiveTrace 的处理结果；returns the result of the operation.
     */
    private static GatewayTraceContext effectiveTrace(
            Observation.Context context,
            GatewayTraceContext fallback) {
        TraceContext tracing = tracingContext(context);
        if (tracing == null
                || tracing.traceId() == null
                || tracing.spanId() == null) {
            return fallback;
        }
        return new GatewayTraceContext(
                tracing.traceId(),
                fallback.requestId(),
                tracing.parentId(),
                tracing.spanId(),
                Boolean.TRUE.equals(tracing.sampled()) ? "01" : "00",
                fallback.tracestate(),
                fallback.source(),
                fallback.headerConflict()
        );
    }

    /**
     * 中文说明：执行 tracingContext 操作；该方法是 {@code GatewayTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the tracing context operation; this method is the invocation entry point on {@code GatewayTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.tracingContext(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param context 参数 context；parameter context。
     * @return 返回 tracingContext 的处理结果；returns the result of the operation.
     */
    private static TraceContext tracingContext(
            Observation.Context context) {
        TracingObservationHandler.TracingContext tracing = context.get(
                TracingObservationHandler.TracingContext.class
        );
        Span span = tracing == null ? null : tracing.getSpan();
        return span == null || span.isNoop() ? null : span.context();
    }

    /**
     * 中文说明：{@code Request} 是类型，位于当前 Gateway 模块的相关包中，负责请求相关的职责与边界。
     * English summary: {@code Request} is a type in the current Gateway module; it owns the request-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public final class Request {

        /**
         * 中文说明：保存 观测 对应的状态、依赖或配置值；字段类型为 {@code Observation}，由 {@code GatewayTelemetry.Request} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by observation; its type is {@code Observation}, and {@code GatewayTelemetry.Request} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTelemetry.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry.Request}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Observation observation;

        /**
         * 中文说明：保存 trace 对应的状态、依赖或配置值；字段类型为 {@code GatewayTraceContext}，由 {@code GatewayTelemetry.Request} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by trace; its type is {@code GatewayTraceContext}, and {@code GatewayTelemetry.Request} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTelemetry.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry.Request}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final GatewayTraceContext trace;

        /**
         * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTelemetry.Request} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code String}, and {@code GatewayTelemetry.Request} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTelemetry.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry.Request}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final String protocol;

        /**
         * 中文说明：保存 attempts 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Operation>}，由 {@code GatewayTelemetry.Request} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempts; its type is {@code Map<String, Operation>}, and {@code GatewayTelemetry.Request} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTelemetry.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry.Request}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Map<String, Operation> attempts =
                new ConcurrentHashMap<>();

        /**
         * 中文说明：保存 stopped 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code GatewayTelemetry.Request} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by stopped; its type is {@code AtomicBoolean}, and {@code GatewayTelemetry.Request} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTelemetry.Request} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry.Request}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicBoolean stopped = new AtomicBoolean();

        /**
         * 中文说明：创建 {@code GatewayTelemetry.Request} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayTelemetry.Request} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param observation 参数 观测；parameter observation。
         * @param trace 参数 trace；parameter trace。
         * @param protocol 参数 protocol；parameter protocol。
         */
        private Request(
                Observation observation,
                GatewayTraceContext trace,
                String protocol) {
            this.observation = observation;
            this.trace = trace;
            this.protocol = protocol;
        }

        /**
         * 中文说明：执行 trace 操作；该方法是 {@code GatewayTelemetry.Request} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the trace operation; this method is the invocation entry point on {@code GatewayTelemetry.Request} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.Request.trace(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 trace 的处理结果；returns the result of the operation.
         */
        public GatewayTraceContext trace() {
            return trace;
        }

        /**
         * 中文说明：执行 路由 操作；该方法是 {@code GatewayTelemetry.Request} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the route operation; this method is the invocation entry point on {@code GatewayTelemetry.Request} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.Request.route(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
         * @param operationId 参数 操作Id；parameter operation id。
         * @param routeId 参数 路由Id；parameter route id。
         */
        public void route(
                String gatewayGroupId,
                String operationId,
                String routeId) {
            observation.highCardinalityKeyValue(
                    "gateway.group.id",
                    safe(gatewayGroupId)
            );
            observation.highCardinalityKeyValue(
                    "gateway.operation.id",
                    safe(operationId)
            );
            observation.highCardinalityKeyValue(
                    "gateway.route.id",
                    safe(routeId)
            );
        }

        /**
         * 中文说明：执行 传输 操作；该方法是 {@code GatewayTelemetry.Request} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the transport operation; this method is the invocation entry point on {@code GatewayTelemetry.Request} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.Request.transport(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param transportMode 参数 传输Mode；parameter transport mode。
         * @param commitPoint 参数 commitPoint；parameter commit point。
         * @param terminationReason 参数 terminationReason；parameter termination reason。
         */
        public void transport(
                String transportMode,
                String commitPoint,
                String terminationReason) {
            observation.lowCardinalityKeyValue(
                    "gateway.transport.mode",
                    safe(transportMode)
            );
            observation.lowCardinalityKeyValue(
                    "gateway.commit.point",
                    safe(commitPoint)
            );
            observation.lowCardinalityKeyValue(
                    "gateway.termination.reason",
                    safe(terminationReason)
            );
        }

        /**
         * 中文说明：执行 startAttempt 操作；该方法是 {@code GatewayTelemetry.Request} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the start attempt operation; this method is the invocation entry point on {@code GatewayTelemetry.Request} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.Request.startAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param number 参数 number；parameter number。
         * @param providerInstanceId 参数 提供方InstanceId；parameter provider instance id。
         * @param providerProtocol 参数 提供方Protocol；parameter provider protocol。
         * @return 返回 startAttempt 的处理结果；returns the result of the operation.
         */
        public AttemptTrace startAttempt(
                int number,
                String providerInstanceId,
                String providerProtocol) {
            Map<String, String> carrier = new LinkedHashMap<>();
            SenderContext<Map<String, String>> context =
                    new SenderContext<>(
                            Map::put,
                            Kind.CLIENT
                    );
            context.setCarrier(carrier);
            context.setRemoteServiceName(providerInstanceId);
            Observation child = Observation.createNotStarted(
                            ATTEMPT,
                            () -> context,
                            registry
                    )
                    .parentObservation(observation)
                    .contextualName("gateway provider "
                            + providerProtocol.toLowerCase())
                    .lowCardinalityKeyValue(
                            "gateway.protocol",
                            protocol
                    )
                    .lowCardinalityKeyValue(
                            "gateway.provider.protocol",
                            providerProtocol
                    )
                    .highCardinalityKeyValue(
                            "gateway.provider.instance.id",
                            safe(providerInstanceId)
                    )
                    .highCardinalityKeyValue(
                            "gateway.attempt.number",
                            Integer.toString(number)
                    )
                    .start();
            TraceContext tracing = tracingContext(context);
            String spanId = tracing == null
                    ? trace.newChildSpanId()
                    : tracing.spanId();
            String traceparent = carrier.get("traceparent");
            if (traceparent == null) {
                traceparent = trace.childTraceparent(spanId);
            }
            String tracestate = carrier.getOrDefault(
                    "tracestate",
                    trace.tracestate()
            );
            attempts.put(spanId, new Operation(child));
            return new AttemptTrace(spanId, traceparent, tracestate);
        }

        /**
         * 中文说明：执行 finishAttempt 操作；该方法是 {@code GatewayTelemetry.Request} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the finish attempt operation; this method is the invocation entry point on {@code GatewayTelemetry.Request} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.Request.finishAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param spanId 参数 spanId；parameter span id。
         * @param outcome 参数 outcome；parameter outcome。
         * @param retryReason 参数 重试Reason；parameter retry reason。
         * @param failure 参数 failure；parameter failure。
         */
        public void finishAttempt(
                String spanId,
                String outcome,
                String retryReason,
                Throwable failure) {
            Operation attempt = attempts.remove(spanId);
            if (attempt == null) {
                return;
            }
            if (retryReason != null && !retryReason.isBlank()) {
                attempt.high("gateway.retry.reason", retryReason);
            }
            if (failure == null && "ERROR".equals(outcome)) {
                failure = new TelemetryFailure("provider attempt failed");
            }
            attempt.finish(outcome, failure);
        }

        /**
         * 中文说明：执行 finish 操作；该方法是 {@code GatewayTelemetry.Request} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the finish operation; this method is the invocation entry point on {@code GatewayTelemetry.Request} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.Request.finish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param terminalStage 参数 terminalStage；parameter terminal stage。
         * @param outcome 参数 outcome；parameter outcome。
         * @param errorCode 参数 errorCode；parameter error code。
         */
        public void finish(
                String terminalStage,
                String outcome,
                String errorCode) {
            if (!stopped.compareAndSet(false, true)) {
                return;
            }
            attempts.values().forEach(attempt ->
                    attempt.finish("CANCELLED", null)
            );
            attempts.clear();
            observation.lowCardinalityKeyValue(
                    "gateway.outcome",
                    safe(outcome)
            );
            observation.lowCardinalityKeyValue(
                    "gateway.terminal.stage",
                    safe(terminalStage)
            );
            if (errorCode != null && !errorCode.isBlank()) {
                observation.highCardinalityKeyValue(
                        "gateway.error.code",
                        errorCode
                );
                observation.error(new TelemetryFailure(errorCode));
            }
            observation.stop();
        }
    }

    /**
     * 中文说明：{@code Operation} 是类型，位于当前 Gateway 模块的相关包中，负责操作相关的职责与边界。
     * English summary: {@code Operation} is a type in the current Gateway module; it owns the operation-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static final class Operation {

        /**
         * 中文说明：保存 观测 对应的状态、依赖或配置值；字段类型为 {@code Observation}，由 {@code GatewayTelemetry.Operation} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by observation; its type is {@code Observation}, and {@code GatewayTelemetry.Operation} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTelemetry.Operation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry.Operation}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Observation observation;

        /**
         * 中文说明：保存 stopped 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code GatewayTelemetry.Operation} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by stopped; its type is {@code AtomicBoolean}, and {@code GatewayTelemetry.Operation} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTelemetry.Operation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry.Operation}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicBoolean stopped = new AtomicBoolean();

        /**
         * 中文说明：创建 {@code GatewayTelemetry.Operation} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayTelemetry.Operation} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param observation 参数 观测；parameter observation。
         */
        private Operation(Observation observation) {
            this.observation = observation;
        }

        /**
         * 中文说明：执行 low 操作；该方法是 {@code GatewayTelemetry.Operation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the low operation; this method is the invocation entry point on {@code GatewayTelemetry.Operation} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.Operation.low(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param name 参数 name；parameter name。
         * @param value 参数 值；parameter value。
         * @return 返回 low 的处理结果；returns the result of the operation.
         */
        private Operation low(String name, String value) {
            observation.lowCardinalityKeyValue(name, safe(value));
            return this;
        }

        /**
         * 中文说明：执行 high 操作；该方法是 {@code GatewayTelemetry.Operation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the high operation; this method is the invocation entry point on {@code GatewayTelemetry.Operation} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.Operation.high(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param name 参数 name；parameter name。
         * @param value 参数 值；parameter value。
         * @return 返回 high 的处理结果；returns the result of the operation.
         */
        private Operation high(String name, String value) {
            observation.highCardinalityKeyValue(name, safe(value));
            return this;
        }

        /**
         * 中文说明：执行 start 操作；该方法是 {@code GatewayTelemetry.Operation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the start operation; this method is the invocation entry point on {@code GatewayTelemetry.Operation} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.Operation.start(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 start 的处理结果；returns the result of the operation.
         */
        private Operation start() {
            observation.start();
            return this;
        }

        /**
         * 中文说明：执行 success 操作；该方法是 {@code GatewayTelemetry.Operation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the success operation; this method is the invocation entry point on {@code GatewayTelemetry.Operation} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.Operation.success(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        public void success() {
            finish("SUCCESS", null);
        }

        /**
         * 中文说明：执行 ignored 操作；该方法是 {@code GatewayTelemetry.Operation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the ignored operation; this method is the invocation entry point on {@code GatewayTelemetry.Operation} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.Operation.ignored(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        public void ignored() {
            finish("IGNORED", null);
        }

        /**
         * 中文说明：执行 failure 操作；该方法是 {@code GatewayTelemetry.Operation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the failure operation; this method is the invocation entry point on {@code GatewayTelemetry.Operation} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.Operation.failure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param failure 参数 failure；parameter failure。
         */
        public void failure(Throwable failure) {
            finish("ERROR", failure);
        }

        /**
         * 中文说明：执行 finish 操作；该方法是 {@code GatewayTelemetry.Operation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the finish operation; this method is the invocation entry point on {@code GatewayTelemetry.Operation} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.Operation.finish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param outcome 参数 outcome；parameter outcome。
         * @param failure 参数 failure；parameter failure。
         */
        private void finish(String outcome, Throwable failure) {
            if (!stopped.compareAndSet(false, true)) {
                return;
            }
            observation.lowCardinalityKeyValue(
                    "gateway.outcome",
                    outcome
            );
            if (failure != null) {
                observation.error(failure);
            }
            observation.stop();
        }
    }

    /**
     * 中文说明：{@code AttemptTrace} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责AttemptTrace相关的职责与边界。
     * English summary: {@code AttemptTrace} is an immutable data carrier in the current Gateway module; it owns the attempt trace-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param spanId 参数 spanId；parameter span id。
     * @param traceparent 参数 traceparent；parameter traceparent。
     * @param tracestate 参数 tracestate；parameter tracestate。
     */
    public record AttemptTrace(
            /**
             * 中文说明：保存 spanId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTelemetry.AttemptTrace} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by span id; its type is {@code String}, and {@code GatewayTelemetry.AttemptTrace} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayTelemetry.AttemptTrace} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry.AttemptTrace}; do not couple callers to its representation when the owning type exposes an API.
             */
            String spanId,
            /**
             * 中文说明：保存 traceparent 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTelemetry.AttemptTrace} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by traceparent; its type is {@code String}, and {@code GatewayTelemetry.AttemptTrace} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayTelemetry.AttemptTrace} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry.AttemptTrace}; do not couple callers to its representation when the owning type exposes an API.
             */
            String traceparent,
            /**
             * 中文说明：保存 tracestate 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTelemetry.AttemptTrace} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by tracestate; its type is {@code String}, and {@code GatewayTelemetry.AttemptTrace} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayTelemetry.AttemptTrace} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTelemetry.AttemptTrace}; do not couple callers to its representation when the owning type exposes an API.
             */
            String tracestate) {
    }

    /**
     * 中文说明：执行 safe 操作；该方法是 {@code GatewayTelemetry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe operation; this method is the invocation entry point on {@code GatewayTelemetry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTelemetry.safe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 safe 的处理结果；returns the result of the operation.
     */
    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    /**
     * 中文说明：{@code TelemetryFailure} 是类型，位于当前 Gateway 模块的相关包中，负责遥测Failure相关的职责与边界。
     * English summary: {@code TelemetryFailure} is a type in the current Gateway module; it owns the telemetry failure-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class TelemetryFailure
            extends RuntimeException {

        /**
         * 中文说明：创建 {@code GatewayTelemetry.TelemetryFailure} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayTelemetry.TelemetryFailure} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param code 参数 code；parameter code。
         */
        private TelemetryFailure(String code) {
            super(code, null, false, false);
        }
    }
}
