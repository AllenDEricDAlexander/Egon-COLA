package top.egon.cola.component.gateway.engine.common.observability.domain;

import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mutable request-local builder that can publish exactly one immutable event.
 * 补充说明 / Supplementary summary: {@code GatewayCallObservation} 是类型，位于当前 Gateway 模块的相关包中，负责网关调用观测相关的职责与边界。
 * English supplement: {@code GatewayCallObservation} is a type in the current Gateway module; it owns the gateway call observation-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayCallObservation {

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 trace 对应的状态、依赖或配置值；字段类型为 {@code GatewayTraceContext}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by trace; its type is {@code GatewayTraceContext}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayTraceContext trace;

    /**
     * 中文说明：保存 遥测 对应的状态、依赖或配置值；字段类型为 {@code GatewayTelemetry.Request}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by telemetry; its type is {@code GatewayTelemetry.Request}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayTelemetry.Request telemetry;

    /**
     * 中文说明：保存 请求Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by request id; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String requestId;

    /**
     * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String protocol;

    /**
     * 中文说明：保存 accessZone 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by access zone; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String accessZone;

    /**
     * 中文说明：保存 引擎NodeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by engine node id; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String engineNodeId;

    /**
     * 中文说明：保存 occurredAt 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by occurred at; its type is {@code long}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final long occurredAt;

    /**
     * 中文说明：保存 startedNanos 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by started nanos; its type is {@code long}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final long startedNanos;

    /**
     * 中文说明：保存 请求Bytes 对应的状态、依赖或配置值；字段类型为 {@code AtomicLong}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by request bytes; its type is {@code AtomicLong}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicLong requestBytes = new AtomicLong();

    /**
     * 中文说明：保存 响应Bytes 对应的状态、依赖或配置值；字段类型为 {@code AtomicLong}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by response bytes; its type is {@code AtomicLong}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicLong responseBytes = new AtomicLong();

    /**
     * 中文说明：保存 completed 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by completed; its type is {@code AtomicBoolean}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicBoolean completed = new AtomicBoolean();

    /**
     * 中文说明：保存 attempts 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayCallEventV1.Attempt>}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by attempts; its type is {@code List<GatewayCallEventV1.Attempt>}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final List<GatewayCallEventV1.Attempt> attempts =
            new ArrayList<>();

    /**
     * 中文说明：保存 normalized方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by normalized method; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile String normalizedMethod;

    /**
     * 中文说明：保存 normalized路由模板 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by normalized route template; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile String normalizedRouteTemplate;

    /**
     * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile String env;

    /**
     * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile String namespace;

    /**
     * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile String gatewayGroupId;

    /**
     * 中文说明：保存 发布Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by release id; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile String releaseId;

    /**
     * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile String operationId;

    /**
     * 中文说明：保存 路由Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by route id; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile String routeId;

    /**
     * 中文说明：保存 提供方身份 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by provider identity; its type is {@code Map<String, Object>}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile Map<String, Object> providerIdentity = Map.of();

    /**
     * 中文说明：保存 terminalStage 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by terminal stage; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile String terminalStage = "RECEIVE";

    /**
     * 中文说明：保存 rateLimitDecision 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rate limit decision; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile String rateLimitDecision = "NOT_APPLIED";

    /**
     * 中文说明：保存 circuitDecision 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by circuit decision; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile String circuitDecision = "NOT_APPLIED";

    /**
     * 中文说明：保存 安全Decision 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayCallObservation} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security decision; its type is {@code String}, and {@code GatewayCallObservation} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallObservation} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallObservation}; do not couple callers to its representation when the owning type exposes an API.
     */
    private volatile String securityDecision = "NOT_APPLIED";

    /**
     * 中文说明：创建 {@code GatewayCallObservation} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCallObservation} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param clock 参数 clock；parameter clock。
     * @param trace 参数 trace；parameter trace。
     * @param requestId 参数 请求Id；parameter request id。
     * @param protocol 参数 protocol；parameter protocol。
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     */
    public GatewayCallObservation(
            Clock clock,
            GatewayTraceContext trace,
            String requestId,
            String protocol,
            String accessZone,
            String engineNodeId) {
        this(
                clock,
                trace,
                requestId,
                protocol,
                accessZone,
                engineNodeId,
                GatewayTelemetry.noop()
        );
    }

    /**
     * 中文说明：创建 {@code GatewayCallObservation} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayCallObservation} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param clock 参数 clock；parameter clock。
     * @param trace 参数 trace；parameter trace。
     * @param requestId 参数 请求Id；parameter request id。
     * @param protocol 参数 protocol；parameter protocol。
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @param gatewayTelemetry 参数 网关遥测；parameter gateway telemetry。
     */
    public GatewayCallObservation(
            Clock clock,
            GatewayTraceContext trace,
            String requestId,
            String protocol,
            String accessZone,
            String engineNodeId,
            GatewayTelemetry gatewayTelemetry) {
        this.clock = Objects.requireNonNull(clock, "clock");
        telemetry = Objects.requireNonNull(
                gatewayTelemetry,
                "gatewayTelemetry"
        ).startRequest(
                Objects.requireNonNull(trace, "trace"),
                protocol,
                accessZone
        );
        this.trace = telemetry.trace();
        this.requestId = required(requestId, "requestId");
        this.protocol = required(protocol, "protocol");
        this.accessZone = required(accessZone, "accessZone");
        this.engineNodeId = required(engineNodeId, "engineNodeId");
        occurredAt = clock.millis();
        startedNanos = System.nanoTime();
    }

    /**
     * 中文说明：执行 start 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.start(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param trace 参数 trace；parameter trace。
     * @param protocol 参数 protocol；parameter protocol。
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @return 返回 start 的处理结果；returns the result of the operation.
     */
    public static GatewayCallObservation start(
            GatewayTraceContext trace,
            String protocol,
            String accessZone,
            String engineNodeId) {
        return start(
                trace,
                protocol,
                accessZone,
                engineNodeId,
                GatewayTelemetry.noop()
        );
    }

    /**
     * 中文说明：执行 start 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the start operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.start(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param trace 参数 trace；parameter trace。
     * @param protocol 参数 protocol；parameter protocol。
     * @param accessZone 参数 accessZone；parameter access zone。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @param telemetry 参数 遥测；parameter telemetry。
     * @return 返回 start 的处理结果；returns the result of the operation.
     */
    public static GatewayCallObservation start(
            GatewayTraceContext trace,
            String protocol,
            String accessZone,
            String engineNodeId,
            GatewayTelemetry telemetry) {
        return new GatewayCallObservation(
                Clock.systemUTC(),
                trace,
                trace.requestId(),
                protocol,
                accessZone,
                engineNodeId,
                telemetry
        );
    }

    /**
     * 中文说明：执行 trace 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the trace operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.trace(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 trace 的处理结果；returns the result of the operation.
     */
    public GatewayTraceContext trace() {
        return trace;
    }

    /**
     * 中文说明：执行 路由 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the route operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.route(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param normalizedMethod 参数 normalized方法；parameter normalized method。
     * @param normalizedRouteTemplate 参数 normalized路由模板；parameter normalized route template。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param releaseId 参数 发布Id；parameter release id。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param routeId 参数 路由Id；parameter route id。
     */
    public void route(
            String normalizedMethod,
            String normalizedRouteTemplate,
            String gatewayGroupId,
            String releaseId,
            String operationId,
            String routeId) {
        this.normalizedMethod = safe(normalizedMethod);
        this.normalizedRouteTemplate = safe(normalizedRouteTemplate);
        this.gatewayGroupId = safe(gatewayGroupId);
        this.releaseId = safe(releaseId);
        this.operationId = safe(operationId);
        this.routeId = safe(routeId);
        terminalStage = "ROUTE";
        telemetry.route(gatewayGroupId, operationId, routeId);
    }

    /**
     * 中文说明：执行 scope 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the scope operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.scope(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     */
    public void scope(String env, String namespace) {
        this.env = safe(env);
        this.namespace = safe(namespace);
    }

    /**
     * Adds passive transport facts without changing the v1 event contract.
     * 补充说明 / Supplementary summary: 执行 传输 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English supplement: Executes the transport operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.transport(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    public void transport(
            String transportMode,
            String commitPoint,
            String terminationReason) {
        telemetry.transport(
                transportMode,
                commitPoint,
                terminationReason
        );
    }

    /**
     * 中文说明：执行 提供方 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the provider operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.provider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param providerInstanceId 参数 提供方InstanceId；parameter provider instance id。
     * @param providerServiceIdentity 参数 提供方服务身份；parameter provider service identity。
     */
    public void provider(
            String providerInstanceId,
            Map<String, Object> providerServiceIdentity) {
        LinkedHashMap<String, Object> safeIdentity = new LinkedHashMap<>();
        if (providerServiceIdentity != null) {
            copyIdentity(providerServiceIdentity, safeIdentity, "serviceKey");
            copyIdentity(providerServiceIdentity, safeIdentity, "protocol");
            copyIdentity(providerServiceIdentity, safeIdentity, "version");
            copyIdentity(providerServiceIdentity, safeIdentity, "group");
        }
        if (providerInstanceId != null) {
            safeIdentity.put("instanceId", providerInstanceId);
        }
        providerIdentity = Map.copyOf(safeIdentity);
        terminalStage = "PROVIDER";
    }

    /**
     * 中文说明：执行 governance 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the governance operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.governance(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param rateLimitDecision 参数 rateLimitDecision；parameter rate limit decision。
     * @param circuitDecision 参数 circuitDecision；parameter circuit decision。
     * @param securityDecision 参数 安全Decision；parameter security decision。
     */
    public void governance(
            String rateLimitDecision,
            String circuitDecision,
            String securityDecision) {
        this.rateLimitDecision = safeDecision(rateLimitDecision);
        this.circuitDecision = safeDecision(circuitDecision);
        this.securityDecision = safeDecision(securityDecision);
        terminalStage = "GOVERNANCE";
    }

    /**
     * 中文说明：执行 add请求Bytes 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the add request bytes operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.addRequestBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param bytes 参数 bytes；parameter bytes。
     */
    public void addRequestBytes(long bytes) {
        if (bytes > 0) {
            requestBytes.addAndGet(bytes);
        }
    }

    /**
     * 中文说明：执行 add响应Bytes 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the add response bytes operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.addResponseBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param bytes 参数 bytes；parameter bytes。
     */
    public void addResponseBytes(long bytes) {
        if (bytes > 0) {
            responseBytes.addAndGet(bytes);
        }
    }

    /**
     * 中文说明：执行 attempt 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the attempt operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.attempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param number 参数 number；parameter number。
     * @param spanId 参数 spanId；parameter span id。
     * @param providerInstanceId 参数 提供方InstanceId；parameter provider instance id。
     * @param startedAt 参数 startedAt；parameter started at。
     * @param durationMs 参数 durationMs；parameter duration ms。
     * @param category 参数 category；parameter category。
     * @param retryReason 参数 重试Reason；parameter retry reason。
     */
    public synchronized void attempt(
            int number,
            String spanId,
            String providerInstanceId,
            long startedAt,
            long durationMs,
            String category,
            String retryReason) {
        telemetry.finishAttempt(
                spanId,
                category,
                retryReason,
                null
        );
        attempts.add(new GatewayCallEventV1.Attempt(
                number,
                spanId,
                providerInstanceId,
                startedAt,
                durationMs,
                safe(category),
                safe(retryReason)
        ));
    }

    /**
     * 中文说明：执行 beginAttempt 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the begin attempt operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.beginAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param number 参数 number；parameter number。
     * @param providerInstanceId 参数 提供方InstanceId；parameter provider instance id。
     * @param providerProtocol 参数 提供方Protocol；parameter provider protocol。
     * @return 返回 beginAttempt 的处理结果；returns the result of the operation.
     */
    public GatewayTelemetry.AttemptTrace beginAttempt(
            int number,
            String providerInstanceId,
            String providerProtocol) {
        return telemetry.startAttempt(
                number,
                providerInstanceId,
                providerProtocol
        );
    }

    /**
     * 中文说明：执行 complete 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the complete operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.complete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param terminalStage 参数 terminalStage；parameter terminal stage。
     * @param category 参数 category；parameter category。
     * @param gatewayErrorCode 参数 网关ErrorCode；parameter gateway error code。
     * @param httpStatus 参数 httpStatus；parameter http status。
     * @param grpcStatus 参数 grpcStatus；parameter grpc status。
     * @return 返回 complete 的处理结果；returns the result of the operation.
     */
    public Optional<GatewayCallEventV1> complete(
            String terminalStage,
            String category,
            String gatewayErrorCode,
            Integer httpStatus,
            String grpcStatus) {
        if (!completed.compareAndSet(false, true)) {
            return Optional.empty();
        }
        telemetry.finish(terminalStage, category, gatewayErrorCode);
        long completedAt = Math.max(clock.millis(), occurredAt);
        long durationMs = Math.max(
                0,
                (System.nanoTime() - startedNanos) / 1_000_000
        );
        List<GatewayCallEventV1.Attempt> attemptSnapshot;
        synchronized (this) {
            attemptSnapshot = List.copyOf(attempts);
        }
        return Optional.of(new GatewayCallEventV1(
                "v1",
                UuidV7.simpleString(),
                occurredAt,
                completedAt,
                new GatewayCallEventV1.Trace(
                        trace.traceId(),
                        trace.engineSpanId(),
                        trace.sampled()
                ),
                new GatewayCallEventV1.Request(
                        requestId,
                        protocol,
                        accessZone,
                        safe(normalizedMethod),
                        safe(normalizedRouteTemplate),
                        requestBytes.get(),
                        "UNSPECIFIED"
                ),
                new GatewayCallEventV1.Routing(
                        safe(env),
                        safe(namespace),
                        safe(gatewayGroupId),
                        engineNodeId,
                        safe(releaseId),
                        safe(operationId),
                        safe(routeId),
                        providerIdentity
                ),
                new GatewayCallEventV1.Governance(
                        safe(terminalStage == null
                                ? this.terminalStage
                                : terminalStage),
                        rateLimitDecision,
                        circuitDecision,
                        securityDecision,
                        Math.max(0, attemptSnapshot.size() - 1)
                ),
                new GatewayCallEventV1.Result(
                        safe(category),
                        safe(gatewayErrorCode),
                        httpStatus,
                        safe(grpcStatus),
                        responseBytes.get(),
                        durationMs
                ),
                attemptSnapshot
        ));
    }

    /**
     * 中文说明：执行 copy身份 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the copy identity operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.copyIdentity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param target 参数 target；parameter target。
     * @param key 参数 键；parameter key。
     */
    private static void copyIdentity(
            Map<String, Object> source,
            Map<String, Object> target,
            String key) {
        Object value = source.get(key);
        if (value instanceof String text && !text.isBlank()) {
            target.put(key, text);
        }
    }

    /**
     * 中文说明：执行 safeDecision 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe decision operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.safeDecision(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 safeDecision 的处理结果；returns the result of the operation.
     */
    private static String safeDecision(String value) {
        return value == null || value.isBlank() ? "NOT_APPLIED" : value;
    }

    /**
     * 中文说明：执行 safe 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.safe(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 safe 的处理结果；returns the result of the operation.
     */
    private static String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code GatewayCallObservation} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code GatewayCallObservation} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallObservation.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
