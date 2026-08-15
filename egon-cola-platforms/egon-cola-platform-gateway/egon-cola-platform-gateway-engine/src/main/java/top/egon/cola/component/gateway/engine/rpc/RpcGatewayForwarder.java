package top.egon.cola.component.gateway.engine.rpc;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import top.egon.cola.component.common.id.uuid.UuidV7;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.engine.balance.ProviderSelectionHandle;
import top.egon.cola.component.gateway.engine.discovery.ProviderCallOutcome;
import top.egon.cola.component.gateway.engine.discovery.ProviderCallOutcomeRecorder;
import top.egon.cola.component.gateway.engine.http.ProviderSelector;
import top.egon.cola.component.gateway.engine.observability.GatewayCallCompletionListener;
import top.egon.cola.component.gateway.engine.observability.GatewayCallObservation;
import top.egon.cola.component.gateway.engine.observability.GatewayTelemetry;
import top.egon.cola.component.gateway.engine.security.GatewaySecurityException;
import top.egon.cola.component.gateway.engine.security.TrustedIdentitySanitizer;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficContext;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficGovernance;
import top.egon.cola.component.gateway.engine.traffic.GatewayTrafficRejectedException;
import top.egon.cola.component.gateway.engine.traffic.ProviderCallClassification;
import top.egon.cola.component.rpc.context.invocation.RpcFailureStage;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 中文说明：{@code RpcGatewayForwarder} 是类型，位于当前 Gateway 模块的相关包中，负责Rpc网关转发器相关的职责与边界。
 * English summary: {@code RpcGatewayForwarder} is a type in the current Gateway module; it owns the rpc gateway forwarder-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class RpcGatewayForwarder {

    /**
     * 中文说明：保存 提供方Selector 对应的状态、依赖或配置值；字段类型为 {@code ProviderSelector}，由 {@code RpcGatewayForwarder} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by provider selector; its type is {@code ProviderSelector}, and {@code RpcGatewayForwarder} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ProviderSelector providerSelector;

    /**
     * 中文说明：保存 channels 对应的状态、依赖或配置值；字段类型为 {@code RpcProviderChannelCache}，由 {@code RpcGatewayForwarder} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by channels; its type is {@code RpcProviderChannelCache}, and {@code RpcGatewayForwarder} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RpcProviderChannelCache channels;

    /**
     * 中文说明：保存 maximum超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code RpcGatewayForwarder} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by maximum timeout; its type is {@code Duration}, and {@code RpcGatewayForwarder} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Duration maximumTimeout;

    /**
     * 中文说明：保存 maxInbound消息Bytes 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code RpcGatewayForwarder} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by max inbound message bytes; its type is {@code int}, and {@code RpcGatewayForwarder} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final int maxInboundMessageBytes;

    /**
     * 中文说明：保存 安全Processor 对应的状态、依赖或配置值；字段类型为 {@code GatewayRpcSecurityProcessor}，由 {@code RpcGatewayForwarder} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security processor; its type is {@code GatewayRpcSecurityProcessor}, and {@code RpcGatewayForwarder} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRpcSecurityProcessor securityProcessor;

    /**
     * 中文说明：保存 补全监听器 对应的状态、依赖或配置值；字段类型为 {@code GatewayCallCompletionListener}，由 {@code RpcGatewayForwarder} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by completion listener; its type is {@code GatewayCallCompletionListener}, and {@code RpcGatewayForwarder} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayCallCompletionListener completionListener;

    /**
     * 中文说明：保存 遥测 对应的状态、依赖或配置值；字段类型为 {@code GatewayTelemetry}，由 {@code RpcGatewayForwarder} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by telemetry; its type is {@code GatewayTelemetry}, and {@code RpcGatewayForwarder} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayTelemetry telemetry;

    /**
     * 中文说明：保存 引擎NodeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RpcGatewayForwarder} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by engine node id; its type is {@code String}, and {@code RpcGatewayForwarder} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final String engineNodeId;

    /**
     * 中文说明：保存 流量Governance 对应的状态、依赖或配置值；字段类型为 {@code GatewayTrafficGovernance}，由 {@code RpcGatewayForwarder} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by traffic governance; its type is {@code GatewayTrafficGovernance}, and {@code RpcGatewayForwarder} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayTrafficGovernance trafficGovernance;

    /**
     * 中文说明：保存 outcomeRecorder 对应的状态、依赖或配置值；字段类型为 {@code ProviderCallOutcomeRecorder}，由 {@code RpcGatewayForwarder} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by outcome recorder; its type is {@code ProviderCallOutcomeRecorder}, and {@code RpcGatewayForwarder} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ProviderCallOutcomeRecorder outcomeRecorder;

    /**
     * 中文说明：保存 身份Sanitizer 对应的状态、依赖或配置值；字段类型为 {@code TrustedIdentitySanitizer}，由 {@code RpcGatewayForwarder} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by identity sanitizer; its type is {@code TrustedIdentitySanitizer}, and {@code RpcGatewayForwarder} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final TrustedIdentitySanitizer identitySanitizer =
            new TrustedIdentitySanitizer();

    /**
     * 中文说明：创建 {@code RpcGatewayForwarder} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcGatewayForwarder} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param channels 参数 channels；parameter channels。
     * @param maximumTimeout 参数 maximum超时；parameter maximum timeout。
     * @param maxInboundMessageBytes 参数 maxInbound消息Bytes；parameter max inbound message bytes。
     */
    public RpcGatewayForwarder(
            ProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            Duration maximumTimeout,
            int maxInboundMessageBytes) {
        this(
                providerSelector,
                channels,
                maximumTimeout,
                maxInboundMessageBytes,
                (route, metadata, traceId, deadline) ->
                        reactor.core.publisher.Mono.just(
                                GatewayRpcSecurityProcessor.Outcome.anonymous()
                ),
                GatewayCallCompletionListener.noop(),
                "unknown-engine",
                GatewayTrafficGovernance.noop()
        );
    }

    /**
     * 中文说明：创建 {@code RpcGatewayForwarder} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcGatewayForwarder} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param channels 参数 channels；parameter channels。
     * @param maximumTimeout 参数 maximum超时；parameter maximum timeout。
     * @param maxInboundMessageBytes 参数 maxInbound消息Bytes；parameter max inbound message bytes。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     */
    public RpcGatewayForwarder(
            ProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            Duration maximumTimeout,
            int maxInboundMessageBytes,
            GatewayRpcSecurityProcessor securityProcessor) {
        this(
                providerSelector,
                channels,
                maximumTimeout,
                maxInboundMessageBytes,
                securityProcessor,
                GatewayCallCompletionListener.noop(),
                "unknown-engine",
                GatewayTrafficGovernance.noop()
        );
    }

    /**
     * 中文说明：创建 {@code RpcGatewayForwarder} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcGatewayForwarder} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param channels 参数 channels；parameter channels。
     * @param maximumTimeout 参数 maximum超时；parameter maximum timeout。
     * @param maxInboundMessageBytes 参数 maxInbound消息Bytes；parameter max inbound message bytes。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     */
    public RpcGatewayForwarder(
            ProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            Duration maximumTimeout,
            int maxInboundMessageBytes,
            GatewayRpcSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId) {
        this(
                providerSelector,
                channels,
                maximumTimeout,
                maxInboundMessageBytes,
                securityProcessor,
                completionListener,
                engineNodeId,
                GatewayTrafficGovernance.noop()
        );
    }

    /**
     * 中文说明：创建 {@code RpcGatewayForwarder} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcGatewayForwarder} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param channels 参数 channels；parameter channels。
     * @param maximumTimeout 参数 maximum超时；parameter maximum timeout。
     * @param maxInboundMessageBytes 参数 maxInbound消息Bytes；parameter max inbound message bytes。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     */
    public RpcGatewayForwarder(
            ProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            Duration maximumTimeout,
            int maxInboundMessageBytes,
            GatewayRpcSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance) {
        this(
                providerSelector,
                channels,
                maximumTimeout,
                maxInboundMessageBytes,
                securityProcessor,
                completionListener,
                engineNodeId,
                trafficGovernance,
                ProviderCallOutcomeRecorder.noop()
        );
    }

    /**
     * 中文说明：创建 {@code RpcGatewayForwarder} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcGatewayForwarder} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param channels 参数 channels；parameter channels。
     * @param maximumTimeout 参数 maximum超时；parameter maximum timeout。
     * @param maxInboundMessageBytes 参数 maxInbound消息Bytes；parameter max inbound message bytes。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     * @param outcomeRecorder 参数 outcomeRecorder；parameter outcome recorder。
     */
    public RpcGatewayForwarder(
            ProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            Duration maximumTimeout,
            int maxInboundMessageBytes,
            GatewayRpcSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance,
            ProviderCallOutcomeRecorder outcomeRecorder) {
        this(
                providerSelector,
                channels,
                maximumTimeout,
                maxInboundMessageBytes,
                securityProcessor,
                completionListener,
                engineNodeId,
                trafficGovernance,
                outcomeRecorder,
                GatewayTelemetry.noop()
        );
    }

    /**
     * 中文说明：创建 {@code RpcGatewayForwarder} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RpcGatewayForwarder} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param providerSelector 参数 提供方Selector；parameter provider selector。
     * @param channels 参数 channels；parameter channels。
     * @param maximumTimeout 参数 maximum超时；parameter maximum timeout。
     * @param maxInboundMessageBytes 参数 maxInbound消息Bytes；parameter max inbound message bytes。
     * @param securityProcessor 参数 安全Processor；parameter security processor。
     * @param completionListener 参数 补全监听器；parameter completion listener。
     * @param engineNodeId 参数 引擎NodeId；parameter engine node id。
     * @param trafficGovernance 参数 流量Governance；parameter traffic governance。
     * @param outcomeRecorder 参数 outcomeRecorder；parameter outcome recorder。
     * @param telemetry 参数 遥测；parameter telemetry。
     */
    public RpcGatewayForwarder(
            ProviderSelector providerSelector,
            RpcProviderChannelCache channels,
            Duration maximumTimeout,
            int maxInboundMessageBytes,
            GatewayRpcSecurityProcessor securityProcessor,
            GatewayCallCompletionListener completionListener,
            String engineNodeId,
            GatewayTrafficGovernance trafficGovernance,
            ProviderCallOutcomeRecorder outcomeRecorder,
            GatewayTelemetry telemetry) {
        this.providerSelector = Objects.requireNonNull(
                providerSelector,
                "providerSelector"
        );
        this.channels = Objects.requireNonNull(channels, "channels");
        this.maximumTimeout = Objects.requireNonNull(
                maximumTimeout,
                "maximumTimeout"
        );
        this.maxInboundMessageBytes = maxInboundMessageBytes;
        this.securityProcessor = Objects.requireNonNull(
                securityProcessor,
                "securityProcessor"
        );
        this.completionListener = Objects.requireNonNull(
                completionListener,
                "completionListener"
        );
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
        this.engineNodeId = Objects.requireNonNull(
                engineNodeId,
                "engineNodeId"
        );
        this.trafficGovernance = Objects.requireNonNull(
                trafficGovernance,
                "trafficGovernance"
        );
        this.outcomeRecorder = Objects.requireNonNull(
                outcomeRecorder,
                "outcomeRecorder"
        );
    }

    /**
     * 中文说明：执行 处理器 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the handler operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.handler(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param route 参数 路由；parameter route。
     * @return 返回 处理器 的处理结果；returns the result of the operation.
     */
    public ServerCallHandler<byte[], byte[]> handler(RuntimeRpcRoute route) {
        MethodDescriptor<byte[], byte[]> method =
                RawByteMarshaller.INSTANCE.descriptor(route.fullMethodName());
        return (serverCall, inboundHeaders) -> {
            GatewayTraceContext selectedTrace = traceContext(
                    inboundHeaders
            );
            GatewayCallObservation observation = GatewayCallObservation.start(
                    selectedTrace,
                    "RPC",
                    "INTERNAL",
                    engineNodeId,
                    telemetry
            );
            GatewayTraceContext trace = observation.trace();
            observation.route(
                    route.fullMethodName(),
                    route.fullMethodName(),
                    route.targetService().group(),
                    null,
                    route.operationId(),
                    route.routeId()
            );
            observation.scope(
                    route.targetService().env(),
                    route.targetService().namespace()
            );
            if (!metadataMatches(route, inboundHeaders)) {
                serverCall.close(
                        Status.INVALID_ARGUMENT.withDescription(
                                "RPC method metadata conflicts with route"
                        ),
                        gatewayTrailers()
                );
                publish(
                        observation,
                        "ROUTE",
                        Status.INVALID_ARGUMENT,
                        "GATEWAY_RPC_METADATA_MISMATCH"
                );
                return new ServerCall.Listener<>() {
                };
            }
            PendingCall pending = new PendingCall(
                    route,
                    method,
                    serverCall,
                    inboundHeaders,
                    trace,
                    Context.current().getDeadline(),
                    observation
            );
            securityProcessor.authorize(
                            route,
                            inboundHeaders,
                            trace.traceId(),
                            Context.current().getDeadline()
                    )
                    .subscribe(pending::authorized, pending::securityFailed);
            serverCall.request(1);
            return pending;
        };
    }

    /**
     * 中文说明：{@code PendingCall} 是类型，位于当前 Gateway 模块的相关包中，负责Pending调用相关的职责与边界。
     * English summary: {@code PendingCall} is a type in the current Gateway module; it owns the pending call-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private final class PendingCall extends ServerCall.Listener<byte[]> {

        /**
         * 中文说明：保存 路由 对应的状态、依赖或配置值；字段类型为 {@code RuntimeRpcRoute}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by route; its type is {@code RuntimeRpcRoute}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final RuntimeRpcRoute route;

        /**
         * 中文说明：保存 方法 对应的状态、依赖或配置值；字段类型为 {@code MethodDescriptor<byte[], byte[]>}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by method; its type is {@code MethodDescriptor<byte[], byte[]>}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final MethodDescriptor<byte[], byte[]> method;

        /**
         * 中文说明：保存 服务器调用 对应的状态、依赖或配置值；字段类型为 {@code ServerCall<byte[], byte[]>}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by server call; its type is {@code ServerCall<byte[], byte[]>}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final ServerCall<byte[], byte[]> serverCall;

        /**
         * 中文说明：保存 inboundHeaders 对应的状态、依赖或配置值；字段类型为 {@code Metadata}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by inbound headers; its type is {@code Metadata}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Metadata inboundHeaders;

        /**
         * 中文说明：保存 trace 对应的状态、依赖或配置值；字段类型为 {@code GatewayTraceContext}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by trace; its type is {@code GatewayTraceContext}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final GatewayTraceContext trace;

        /**
         * 中文说明：保存 inboundDeadline 对应的状态、依赖或配置值；字段类型为 {@code Deadline}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by inbound deadline; its type is {@code Deadline}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Deadline inboundDeadline;

        /**
         * 中文说明：保存 观测 对应的状态、依赖或配置值；字段类型为 {@code GatewayCallObservation}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by observation; its type is {@code GatewayCallObservation}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final GatewayCallObservation observation;

        /**
         * 中文说明：保存 released 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by released; its type is {@code AtomicBoolean}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicBoolean released = new AtomicBoolean();

        /**
         * 中文说明：保存 请求 对应的状态、依赖或配置值；字段类型为 {@code byte[]}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by request; its type is {@code byte[]}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private byte[] request;

        /**
         * 中文说明：保存 halfClosed 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by half closed; its type is {@code boolean}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean halfClosed;

        /**
         * 中文说明：保存 cancelled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by cancelled; its type is {@code boolean}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean cancelled;

        /**
         * 中文说明：保存 started 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by started; its type is {@code boolean}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean started;

        /**
         * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code GatewayRpcSecurityProcessor.Outcome}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code GatewayRpcSecurityProcessor.Outcome}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private GatewayRpcSecurityProcessor.Outcome security;

        /**
         * 中文说明：保存 流量Permit 对应的状态、依赖或配置值；字段类型为 {@code GatewayTrafficGovernance.RequestPermit}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by traffic permit; its type is {@code GatewayTrafficGovernance.RequestPermit}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private GatewayTrafficGovernance.RequestPermit trafficPermit;

        /**
         * 中文说明：保存 attemptPermit 对应的状态、依赖或配置值；字段类型为 {@code GatewayTrafficGovernance.AttemptPermit}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempt permit; its type is {@code GatewayTrafficGovernance.AttemptPermit}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private GatewayTrafficGovernance.AttemptPermit attemptPermit;

        /**
         * 中文说明：保存 selection 对应的状态、依赖或配置值；字段类型为 {@code ProviderSelectionHandle}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by selection; its type is {@code ProviderSelectionHandle}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private ProviderSelectionHandle selection;

        /**
         * 中文说明：保存 通道Handle 对应的状态、依赖或配置值；字段类型为 {@code RpcProviderChannelCache.ChannelHandle}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by channel handle; its type is {@code RpcProviderChannelCache.ChannelHandle}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private RpcProviderChannelCache.ChannelHandle channelHandle;

        /**
         * 中文说明：保存 客户端调用 对应的状态、依赖或配置值；字段类型为 {@code ClientCall<byte[], byte[]>}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by client call; its type is {@code ClientCall<byte[], byte[]>}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private ClientCall<byte[], byte[]> clientCall;

        /**
         * 中文说明：保存 attemptStartedAt 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempt started at; its type is {@code long}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private long attemptStartedAt;

        /**
         * 中文说明：保存 attemptStartedNanos 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempt started nanos; its type is {@code long}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private long attemptStartedNanos;

        /**
         * 中文说明：保存 attemptSpanId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempt span id; its type is {@code String}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String attemptSpanId;

        /**
         * 中文说明：保存 attemptNumber 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by attempt number; its type is {@code int}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int attemptNumber;

        /**
         * 中文说明：保存 重试DeadlineNanos 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by retry deadline nanos; its type is {@code long}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private long retryDeadlineNanos;

        /**
         * 中文说明：保存 响应Started 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by response started; its type is {@code boolean}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean responseStarted;

        /**
         * 中文说明：保存 failedProviders 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code RpcGatewayForwarder.PendingCall} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by failed providers; its type is {@code Set<String>}, and {@code RpcGatewayForwarder.PendingCall} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RpcGatewayForwarder.PendingCall} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RpcGatewayForwarder.PendingCall}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Set<String> failedProviders = new LinkedHashSet<>();

        /**
         * 中文说明：创建 {@code RpcGatewayForwarder.PendingCall} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code RpcGatewayForwarder.PendingCall} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param route 参数 路由；parameter route。
         * @param method 参数 方法；parameter method。
         * @param serverCall 参数 服务器调用；parameter server call。
         * @param inboundHeaders 参数 inboundHeaders；parameter inbound headers。
         * @param trace 参数 trace；parameter trace。
         * @param inboundDeadline 参数 inboundDeadline；parameter inbound deadline。
         * @param observation 参数 观测；parameter observation。
         */
        private PendingCall(
                RuntimeRpcRoute route,
                MethodDescriptor<byte[], byte[]> method,
                ServerCall<byte[], byte[]> serverCall,
                Metadata inboundHeaders,
                GatewayTraceContext trace,
                Deadline inboundDeadline,
                GatewayCallObservation observation) {
            this.route = route;
            this.method = method;
            this.serverCall = serverCall;
            this.inboundHeaders = inboundHeaders;
            this.trace = trace;
            this.inboundDeadline = inboundDeadline;
            this.observation = observation;
        }

        /**
         * 中文说明：执行 on消息 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the on message operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.onMessage(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param message 参数 消息；parameter message。
         */
        @Override
        public synchronized void onMessage(byte[] message) {
            if (message.length > maxInboundMessageBytes) {
                close(
                        Status.RESOURCE_EXHAUSTED,
                        "GATEWAY_RPC_MESSAGE_TOO_LARGE"
                );
                return;
            }
            if (request != null) {
                close(
                        Status.INVALID_ARGUMENT,
                        "GATEWAY_RPC_MULTIPLE_MESSAGES"
                );
                return;
            }
            request = message;
            observation.addRequestBytes(message.length);
            startIfReady();
        }

        /**
         * 中文说明：执行 onHalfClose 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the on half close operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.onHalfClose(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        @Override
        public synchronized void onHalfClose() {
            halfClosed = true;
            if (request == null) {
                close(
                        Status.INVALID_ARGUMENT,
                        "GATEWAY_RPC_REQUEST_MISSING"
                );
                return;
            }
            startIfReady();
        }

        /**
         * 中文说明：执行 onCancel 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the on cancel operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.onCancel(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        @Override
        public synchronized void onCancel() {
            cancelled = true;
            if (clientCall != null) {
                clientCall.cancel("consumer cancelled", null);
            }
            publish(
                    observation,
                    "CLIENT",
                    Status.CANCELLED,
                    "GATEWAY_RPC_CANCELLED"
            );
            release();
        }

        /**
         * 中文说明：执行 authorized 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the authorized operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.authorized(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param outcome 参数 outcome；parameter outcome。
         */
        private synchronized void authorized(
                GatewayRpcSecurityProcessor.Outcome outcome) {
            if (cancelled || released.get()) {
                return;
            }
            security = Objects.requireNonNull(outcome, "security outcome");
            trafficGovernance.acquire(
                            route.policyRefs(),
                            trafficContext(route, inboundHeaders, security),
                            route.timeout()
                    )
                    .subscribe(
                            permit -> {
                                synchronized (PendingCall.this) {
                                    if (cancelled || released.get()) {
                                        permit.close();
                                        return;
                                    }
                                    trafficPermit = permit;
                                    observation.governance(
                                            "APPLIED",
                                            permit.retryPolicy().enabled()
                                                    ? "RETRY_ENABLED"
                                                    : "RETRY_DISABLED",
                                            "ALLOW"
                                    );
                                    startIfReady();
                                }
                            },
                            this::trafficFailed
                    );
        }

        /**
         * 中文说明：执行 安全Failed 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the security failed operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.securityFailed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param failure 参数 failure；parameter failure。
         */
        private synchronized void securityFailed(Throwable failure) {
            if (failure instanceof GatewaySecurityException securityFailure) {
                close(
                        rpcStatus(securityFailure.rpcStatus()),
                        securityFailure.code()
                );
                return;
            }
            close(
                    Status.UNAVAILABLE,
                    "GATEWAY_SECURITY_PROVIDER_ERROR"
            );
        }

        /**
         * 中文说明：执行 流量Failed 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the traffic failed operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.trafficFailed(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param failure 参数 failure；parameter failure。
         */
        private synchronized void trafficFailed(Throwable failure) {
            if (failure instanceof GatewayTrafficRejectedException rejected) {
                observation.governance(
                        "APPLIED",
                        rejected.code(),
                        "REJECT"
                );
                close(
                        rpcStatus(rejected.rpcStatus()),
                        rejected.code()
                );
                return;
            }
            close(
                    Status.UNAVAILABLE,
                    "GATEWAY_GOVERNANCE_UNAVAILABLE"
            );
        }

        /**
         * 中文说明：执行 startIfReady 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the start if ready operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.startIfReady(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        private void startIfReady() {
            if (started
                    || cancelled
                    || security == null
                    || trafficPermit == null
                    || request == null
                    || !halfClosed) {
                return;
            }
            started = true;
            retryDeadlineNanos = System.nanoTime()
                    + totalBudgetNanos(
                    trafficPermit.timeout(),
                    inboundDeadline
            );
            startAttempt();
        }

        /**
         * 中文说明：执行 startAttempt 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the start attempt operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.startAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        private synchronized void startAttempt() {
            if (cancelled || released.get()) {
                return;
            }
            attemptNumber++;
            responseStarted = false;
            try {
                selection = providerSelector.select(
                        route.targetService(),
                        route.policyRefs(),
                        failedProviders
                );
                ProviderInstance provider = selection.instance();
                attemptPermit = trafficPermit.acquireAttempt(provider);
                observation.provider(provider.instanceId(), Map.of(
                        "serviceKey",
                        provider.serviceKey().serviceName(),
                        "protocol",
                        provider.serviceKey().protocolType().name(),
                        "version",
                        provider.serviceKey().version(),
                        "group",
                        provider.serviceKey().group()
                ));
                attemptStartedAt = System.currentTimeMillis();
                attemptStartedNanos = System.nanoTime();
                GatewayTelemetry.AttemptTrace attemptTrace =
                        observation.beginAttempt(
                                attemptNumber,
                                provider.instanceId(),
                                provider.serviceKey()
                                        .protocolType()
                                        .name()
                        );
                attemptSpanId = attemptTrace.spanId();
                channelHandle = channels.acquire(provider);
                clientCall = channelHandle.channel().newCall(
                        method,
                        callOptions(
                                remainingBudget(),
                                inboundDeadline
                        )
                );
                ClientCall<byte[], byte[]> activeCall = clientCall;
                activeCall.start(new ClientCall.Listener<>() {
                    /**
                     * 中文说明：执行 onHeaders 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                     * English summary: Executes the on headers operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
                     *
                     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.onHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                     * @param headers 参数 headers；parameter headers。
                     */
                    @Override
                    public void onHeaders(Metadata headers) {
                        synchronized (PendingCall.this) {
                            if (activeCall != clientCall
                                    || released.get()) {
                                return;
                            }
                            responseStarted = true;
                            serverCall.sendHeaders(safeMetadata());
                        }
                    }

                    /**
                     * 中文说明：执行 on消息 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                     * English summary: Executes the on message operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
                     *
                     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.onMessage(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                     * @param message 参数 消息；parameter message。
                     */
                    @Override
                    public void onMessage(byte[] message) {
                        synchronized (PendingCall.this) {
                            if (activeCall != clientCall
                                    || released.get()) {
                                return;
                            }
                            responseStarted = true;
                            observation.addResponseBytes(message.length);
                            serverCall.sendMessage(message);
                            activeCall.request(1);
                        }
                    }

                    /**
                     * 中文说明：执行 onClose 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                     * English summary: Executes the on close operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
                     *
                     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.onClose(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                     * @param status 参数 status；parameter status。
                     * @param trailers 参数 trailers；parameter trailers。
                     */
                    @Override
                    public void onClose(
                            Status status,
                            Metadata trailers) {
                        synchronized (PendingCall.this) {
                            if (activeCall != clientCall
                                    || released.get()) {
                                return;
                            }
                            boolean retry = shouldRetry(status);
                            recordAttempt(
                                    status,
                                    retry
                                            ? "RETRYABLE_RPC_STATUS"
                                            : null
                            );
                            attemptPermit.complete(
                                    classification(status)
                            );
                            attemptPermit = null;
                            outcomeRecorder.record(
                                    selection.instance().runtimeIdentity(),
                                    healthOutcome(status)
                            );
                            if (retry) {
                                failedProviders.add(
                                        selection.instance()
                                                .runtimeIdentity()
                                );
                                closeAttemptHandles();
                                scheduleRetry();
                                return;
                            }
                            serverCall.close(
                                    status,
                                    providerTrailers(status)
                            );
                            publish(
                                    observation,
                                    "COMPLETE",
                                    status,
                                    status.isOk()
                                            ? null
                                            : "GATEWAY_RPC_UPSTREAM_STATUS"
                            );
                            release();
                        }
                    }
                }, outboundHeaders(
                        route,
                        inboundHeaders,
                        trace,
                        attemptTrace,
                        security
                ));
                activeCall.request(1);
                activeCall.sendMessage(request);
                activeCall.halfClose();
            } catch (RuntimeException unavailable) {
                if (selection != null) {
                    outcomeRecorder.record(
                            selection.instance().runtimeIdentity(),
                            ProviderCallOutcome.RETRYABLE_FAILURE
                    );
                }
                recordAttempt(
                        Status.UNAVAILABLE,
                        "GATEWAY_PROVIDER_UNAVAILABLE"
                );
                close(
                        Status.UNAVAILABLE,
                        "GATEWAY_PROVIDER_UNAVAILABLE"
                );
            }
        }

        /**
         * 中文说明：执行 should重试 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the should retry operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.shouldRetry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param status 参数 status；parameter status。
         * @return 返回 should重试 的处理结果；returns the result of the operation.
         */
        private boolean shouldRetry(Status status) {
            var policy = trafficPermit.retryPolicy();
            if (!policy.enabled()
                    || !route.idempotent()
                    || responseStarted
                    || attemptNumber >= policy.maxAttempts()
                    || !policy.retryableRpcStatus(
                    status.getCode().name()
            )) {
                return false;
            }
            long required = policy.backoff(attemptNumber).toNanos()
                    + policy.minimumAttemptBudget().toNanos();
            return retryDeadlineNanos - System.nanoTime() >= required;
        }

        /**
         * 中文说明：执行 schedule重试 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the schedule retry operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.scheduleRetry(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        private void scheduleRetry() {
            Duration backoff = trafficPermit.retryPolicy().backoff(
                    attemptNumber
            );
            reactor.core.publisher.Mono.delay(backoff).subscribe(ignored -> {
                synchronized (PendingCall.this) {
                    startAttempt();
                }
            });
        }

        /**
         * 中文说明：执行 remainingBudget 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the remaining budget operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.remainingBudget(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 remainingBudget 的处理结果；returns the result of the operation.
         */
        private Duration remainingBudget() {
            return Duration.ofNanos(Math.max(
                    1,
                    retryDeadlineNanos - System.nanoTime()
            ));
        }

        /**
         * 中文说明：执行 close 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the close operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param status 参数 status；parameter status。
         * @param code 参数 code；parameter code。
         */
        private void close(Status status, String code) {
            if (released.compareAndSet(false, true)) {
                if (clientCall != null) {
                    clientCall.cancel(code, null);
                }
                serverCall.close(
                        status.withDescription(code),
                        gatewayTrailers()
                );
                publish(observation, "GATEWAY", status, code);
                closeHandles();
            }
        }

        /**
         * 中文说明：执行 发布 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the release operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.release(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        private void release() {
            if (released.compareAndSet(false, true)) {
                closeHandles();
            }
        }

        /**
         * 中文说明：执行 closeHandles 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the close handles operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.closeHandles(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        private void closeHandles() {
            closeAttemptHandles();
            if (trafficPermit != null) {
                trafficPermit.close();
                trafficPermit = null;
            }
        }

        /**
         * 中文说明：执行 closeAttemptHandles 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the close attempt handles operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.closeAttemptHandles(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        private void closeAttemptHandles() {
            if (channelHandle != null) {
                channelHandle.close();
                channelHandle = null;
            }
            if (selection != null) {
                selection.close();
                selection = null;
            }
            if (attemptPermit != null) {
                attemptPermit.close();
                attemptPermit = null;
            }
            clientCall = null;
        }

        /**
         * 中文说明：执行 recordAttempt 操作；该方法是 {@code RpcGatewayForwarder.PendingCall} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the record attempt operation; this method is the invocation entry point on {@code RpcGatewayForwarder.PendingCall} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.PendingCall.recordAttempt(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param status 参数 status；parameter status。
         * @param retryReason 参数 重试Reason；parameter retry reason。
         */
        private void recordAttempt(Status status, String retryReason) {
            if (attemptSpanId == null) {
                return;
            }
            observation.attempt(
                    attemptNumber,
                    attemptSpanId,
                    selection == null
                            ? null
                            : selection.instance().instanceId(),
                    attemptStartedAt,
                    Math.max(
                            0,
                            (System.nanoTime() - attemptStartedNanos)
                                    / 1_000_000
                    ),
                    status.isOk() ? "SUCCESS" : "ERROR",
                    retryReason
            );
            attemptSpanId = null;
        }
    }

    /**
     * 中文说明：执行 调用Options 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the call options operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.callOptions(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param routeTimeout 参数 路由超时；parameter route timeout。
     * @param inboundDeadline 参数 inboundDeadline；parameter inbound deadline。
     * @return 返回 调用Options 的处理结果；returns the result of the operation.
     */
    private CallOptions callOptions(
            Duration routeTimeout,
            Deadline inboundDeadline) {
        long remainingNanos = maximumTimeout.toNanos();
        if (inboundDeadline != null) {
            remainingNanos = Math.min(
                    remainingNanos,
                    inboundDeadline.timeRemaining(TimeUnit.NANOSECONDS)
            );
        }
        remainingNanos = Math.min(remainingNanos, routeTimeout.toNanos());
        return CallOptions.DEFAULT.withDeadlineAfter(
                Math.max(1, remainingNanos),
                TimeUnit.NANOSECONDS
        );
    }

    /**
     * 中文说明：执行 totalBudgetNanos 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the total budget nanos operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.totalBudgetNanos(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param routeTimeout 参数 路由超时；parameter route timeout。
     * @param inboundDeadline 参数 inboundDeadline；parameter inbound deadline。
     * @return 返回 totalBudgetNanos 的处理结果；returns the result of the operation.
     */
    private long totalBudgetNanos(
            Duration routeTimeout,
            Deadline inboundDeadline) {
        long result = Math.min(
                maximumTimeout.toNanos(),
                routeTimeout.toNanos()
        );
        if (inboundDeadline != null) {
            result = Math.min(
                    result,
                    inboundDeadline.timeRemaining(TimeUnit.NANOSECONDS)
            );
        }
        return Math.max(1, result);
    }

    /**
     * 中文说明：执行 元数据Matches 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the metadata matches operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.metadataMatches(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param route 参数 路由；parameter route。
     * @param metadata 参数 元数据；parameter metadata。
     * @return 返回 元数据Matches 的处理结果；returns the result of the operation.
     */
    private boolean metadataMatches(
            RuntimeRpcRoute route,
            Metadata metadata) {
        return matches(
                metadata.get(RpcMetadataKeys.SERVICE),
                route.targetService().serviceName()
        ) && matches(
                metadata.get(RpcMetadataKeys.GROUP),
                route.targetService().group()
        ) && matches(
                metadata.get(RpcMetadataKeys.VERSION),
                route.targetService().version()
        );
    }

    /**
     * 中文说明：执行 matches 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the matches operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.matches(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param supplied 参数 supplied；parameter supplied。
     * @param expected 参数 expected；parameter expected。
     * @return 返回 matches 的处理结果；returns the result of the operation.
     */
    private boolean matches(String supplied, String expected) {
        return supplied == null || supplied.equals(expected);
    }

    /**
     * 中文说明：执行 outboundHeaders 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the outbound headers operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.outboundHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param route 参数 路由；parameter route。
     * @param inbound 参数 inbound；parameter inbound。
     * @param trace 参数 trace；parameter trace。
     * @param attemptTrace 参数 attemptTrace；parameter attempt trace。
     * @param security 参数 安全；parameter security。
     * @return 返回 outboundHeaders 的处理结果；returns the result of the operation.
     */
    private Metadata outboundHeaders(
            RuntimeRpcRoute route,
            Metadata inbound,
            GatewayTraceContext trace,
            GatewayTelemetry.AttemptTrace attemptTrace,
            GatewayRpcSecurityProcessor.Outcome security) {
        Metadata result = new Metadata();
        result.put(
                RpcMetadataKeys.SERVICE,
                route.targetService().serviceName()
        );
        result.put(RpcMetadataKeys.GROUP, route.targetService().group());
        result.put(RpcMetadataKeys.VERSION, route.targetService().version());
        result.put(
                RpcMetadataKeys.INVOCATION_ID,
                valueOrGenerated(inbound.get(RpcMetadataKeys.INVOCATION_ID))
        );
        result.put(
                RpcMetadataKeys.TRACEPARENT,
                attemptTrace.traceparent()
        );
        result.put(RpcMetadataKeys.REQUEST_ID, trace.requestId());
        if (attemptTrace.tracestate() != null) {
            result.put(
                    RpcMetadataKeys.TRACESTATE,
                    attemptTrace.tracestate()
            );
        }
        copy(inbound, result, RpcMetadataKeys.SOURCE_APP);
        copy(inbound, result, RpcMetadataKeys.SOURCE_INSTANCE);
        Map<String, String> trusted = identitySanitizer.sanitizeRpc(
                Map.of(),
                security.fieldsToRemove(),
                security.trustedIdentity()
        );
        trusted.forEach((name, value) -> result.put(
                Metadata.Key.of(
                        name,
                        Metadata.ASCII_STRING_MARSHALLER
                ),
                value
        ));
        if (security.forwardingCredential() != null) {
            result.put(
                    RpcMetadataKeys.AUTHORIZATION,
                    "Bearer " + security.forwardingCredential()
                            .tokenReference()
            );
        }
        return result;
    }

    /**
     * 中文说明：执行 copy 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the copy operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.copy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param target 参数 target；parameter target。
     * @param key 参数 键；parameter key。
     */
    private void copy(
            Metadata source,
            Metadata target,
            Metadata.Key<String> key) {
        String value = source.get(key);
        if (value != null) {
            target.put(key, value);
        }
    }

    /**
     * 中文说明：执行 safe元数据 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe metadata operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.safeMetadata(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 safe元数据 的处理结果；returns the result of the operation.
     */
    private Metadata safeMetadata() {
        return new Metadata();
    }

    /**
     * 中文说明：执行 提供方Trailers 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the provider trailers operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.providerTrailers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @return 返回 提供方Trailers 的处理结果；returns the result of the operation.
     */
    private Metadata providerTrailers(Status status) {
        Metadata trailers = safeMetadata();
        if (!status.isOk()) {
            RpcFailureStage.PROVIDER.put(trailers);
        }
        return trailers;
    }

    /**
     * 中文说明：执行 网关Trailers 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the gateway trailers operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.gatewayTrailers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 网关Trailers 的处理结果；returns the result of the operation.
     */
    private Metadata gatewayTrailers() {
        Metadata trailers = safeMetadata();
        RpcFailureStage.GATEWAY.put(trailers);
        return trailers;
    }

    /**
     * 中文说明：执行 traceContext 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the trace context operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.traceContext(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param metadata 参数 元数据；parameter metadata。
     * @return 返回 traceContext 的处理结果；returns the result of the operation.
     */
    private GatewayTraceContext traceContext(Metadata metadata) {
        return GatewayTraceContext.fromHeaders(
                metadata.get(RpcMetadataKeys.TRACEPARENT),
                metadata.get(RpcMetadataKeys.TRACESTATE),
                metadata.get(RpcMetadataKeys.REQUEST_ID)
        );
    }

    /**
     * 中文说明：执行 值OrGenerated 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the value or generated operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.valueOrGenerated(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 值OrGenerated 的处理结果；returns the result of the operation.
     */
    private String valueOrGenerated(String value) {
        return value == null || value.isBlank()
                ? UuidV7.simpleString()
                : value;
    }

    /**
     * 中文说明：执行 rpcStatus 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rpc status operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.rpcStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 rpcStatus 的处理结果；returns the result of the operation.
     */
    private Status rpcStatus(String value) {
        return switch (value) {
            case "UNAUTHENTICATED" -> Status.UNAUTHENTICATED;
            case "PERMISSION_DENIED" -> Status.PERMISSION_DENIED;
            case "INTERNAL" -> Status.INTERNAL;
            case "RESOURCE_EXHAUSTED" -> Status.RESOURCE_EXHAUSTED;
            case "DEADLINE_EXCEEDED" -> Status.DEADLINE_EXCEEDED;
            default -> Status.UNAVAILABLE;
        };
    }

    /**
     * 中文说明：执行 classification 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the classification operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.classification(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @return 返回 classification 的处理结果；returns the result of the operation.
     */
    private ProviderCallClassification classification(Status status) {
        if (status.isOk()) {
            return ProviderCallClassification.SUCCESS;
        }
        return switch (status.getCode()) {
            case INVALID_ARGUMENT, NOT_FOUND, ALREADY_EXISTS,
                    FAILED_PRECONDITION, UNAUTHENTICATED,
                    PERMISSION_DENIED ->
                    ProviderCallClassification.BUSINESS_FAILURE;
            case CANCELLED -> ProviderCallClassification.CANCELLED;
            default -> ProviderCallClassification.RETRYABLE_FAILURE;
        };
    }

    /**
     * 中文说明：执行 健康Outcome 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the health outcome operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.healthOutcome(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @return 返回 健康Outcome 的处理结果；returns the result of the operation.
     */
    private ProviderCallOutcome healthOutcome(Status status) {
        return switch (classification(status)) {
            case SUCCESS -> ProviderCallOutcome.SUCCESS;
            case RETRYABLE_FAILURE ->
                    ProviderCallOutcome.RETRYABLE_FAILURE;
            case BUSINESS_FAILURE ->
                    ProviderCallOutcome.BUSINESS_REJECTION;
            case CANCELLED -> ProviderCallOutcome.CANCELLED;
        };
    }

    /**
     * 中文说明：执行 流量Context 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the traffic context operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.trafficContext(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param route 参数 路由；parameter route。
     * @param metadata 参数 元数据；parameter metadata。
     * @param security 参数 安全；parameter security。
     * @return 返回 流量Context 的处理结果；returns the result of the operation.
     */
    private GatewayTrafficContext trafficContext(
            RuntimeRpcRoute route,
            Metadata metadata,
            GatewayRpcSecurityProcessor.Outcome security) {
        return new GatewayTrafficContext(
                route.operationId(),
                route.routeId(),
                valueOrGenerated(metadata.get(RpcMetadataKeys.SOURCE_APP)),
                security.trustedIdentity().rpcMetadata().get(
                        "egon-gateway-principal-id"
                ),
                null,
                route.targetService().serviceName(),
                null,
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

    /**
     * 中文说明：执行 publish 操作；该方法是 {@code RpcGatewayForwarder} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the publish operation; this method is the invocation entry point on {@code RpcGatewayForwarder} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RpcGatewayForwarder.publish(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param observation 参数 观测；parameter observation。
     * @param stage 参数 stage；parameter stage。
     * @param status 参数 status；parameter status。
     * @param code 参数 code；parameter code。
     */
    private void publish(
            GatewayCallObservation observation,
            String stage,
            Status status,
            String code) {
        observation.complete(
                stage,
                status.isOk()
                        ? "SUCCESS"
                        : status.getCode() == Status.Code.CANCELLED
                        ? "CANCELLED"
                        : status.getCode() == Status.Code.DEADLINE_EXCEEDED
                        ? "TIMEOUT"
                        : status.getCode() == Status.Code.PERMISSION_DENIED
                        || status.getCode() == Status.Code.UNAUTHENTICATED
                        || status.getCode() == Status.Code.INVALID_ARGUMENT
                        ? "REJECTED"
                        : "ERROR",
                code,
                null,
                status.getCode().name()
        ).ifPresent(completionListener::onComplete);
    }
}
