package top.egon.cola.component.gateway.engine.http;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.error.GatewayError;
import top.egon.cola.component.gateway.contract.error.GatewayErrorCategory;
import top.egon.cola.component.gateway.contract.error.GatewayResult;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.context.GatewayContext;
import top.egon.cola.component.gateway.core.exchange.EmptyGatewayBody;
import top.egon.cola.component.gateway.core.exchange.GatewayBody;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.exchange.GatewayHeaders;
import top.egon.cola.component.gateway.core.exchange.GatewayRequest;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;
import top.egon.cola.component.gateway.core.exchange.ImmutableGatewayHeaders;
import top.egon.cola.component.gateway.core.filter.GatewayFilterChain;
import top.egon.cola.component.gateway.contract.trace.GatewayTraceContext;
import top.egon.cola.component.gateway.engine.websocket.GatewayWebSocketHandshakeResult;

import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code AbstractGatewayHttpStageExchange} 是类型，位于当前 Gateway 模块的相关包中，负责Abstract网关HttpStageExchange相关的职责与边界。
 * English summary: {@code AbstractGatewayHttpStageExchange} is a type in the current Gateway module; it owns the abstract gateway http stage exchange-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public abstract class AbstractGatewayHttpStageExchange
        implements GatewayExchange {

    /**
     * 中文说明：保存 inbound 对应的状态、依赖或配置值；字段类型为 {@code GatewayInboundHttpRequest}，由 {@code AbstractGatewayHttpStageExchange} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by inbound; its type is {@code GatewayInboundHttpRequest}, and {@code AbstractGatewayHttpStageExchange} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code AbstractGatewayHttpStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AbstractGatewayHttpStageExchange}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayInboundHttpRequest inbound;

    /**
     * 中文说明：保存 context 对应的状态、依赖或配置值；字段类型为 {@code GatewayContext}，由 {@code AbstractGatewayHttpStageExchange} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by context; its type is {@code GatewayContext}, and {@code AbstractGatewayHttpStageExchange} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code AbstractGatewayHttpStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AbstractGatewayHttpStageExchange}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayContext context;

    /**
     * 中文说明：保存 请求 对应的状态、依赖或配置值；字段类型为 {@code GatewayRequest}，由 {@code AbstractGatewayHttpStageExchange} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by request; its type is {@code GatewayRequest}, and {@code AbstractGatewayHttpStageExchange} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code AbstractGatewayHttpStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AbstractGatewayHttpStageExchange}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRequest request;

    /**
     * 中文说明：保存 响应 对应的状态、依赖或配置值；字段类型为 {@code GatewayResponse}，由 {@code AbstractGatewayHttpStageExchange} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by response; its type is {@code GatewayResponse}, and {@code AbstractGatewayHttpStageExchange} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code AbstractGatewayHttpStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AbstractGatewayHttpStageExchange}; do not couple callers to its representation when the owning type exposes an API.
     */
    private GatewayResponse response;

    /**
     * 中文说明：保存 webSocketResult 对应的状态、依赖或配置值；字段类型为 {@code GatewayWebSocketHandshakeResult}，由 {@code AbstractGatewayHttpStageExchange} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by web socket result; its type is {@code GatewayWebSocketHandshakeResult}, and {@code AbstractGatewayHttpStageExchange} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code AbstractGatewayHttpStageExchange} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AbstractGatewayHttpStageExchange}; do not couple callers to its representation when the owning type exposes an API.
     */
    private GatewayWebSocketHandshakeResult webSocketResult;

    /**
     * 中文说明：创建 {@code AbstractGatewayHttpStageExchange} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code AbstractGatewayHttpStageExchange} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param inbound 参数 inbound；parameter inbound。
     * @param context 参数 context；parameter context。
     */
    protected AbstractGatewayHttpStageExchange(
            GatewayInboundHttpRequest inbound,
            GatewayContext context) {
        this.inbound = inbound;
        this.context = context;
        request = request(inbound, context);
    }

    /**
     * 中文说明：执行 cors 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the cors operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.cors(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param chain 参数 chain；parameter chain。
     * @return 返回 cors 的处理结果；returns the result of the operation.
     */
    public abstract Publisher<GatewayResponse> cors(
            GatewayFilterChain chain);

    /**
     * 中文说明：执行 安全 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the security operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.security(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param chain 参数 chain；parameter chain。
     * @return 返回 安全 的处理结果；returns the result of the operation.
     */
    public abstract Publisher<GatewayResponse> security(
            GatewayFilterChain chain);

    /**
     * 中文说明：执行 governance 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the governance operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.governance(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param chain 参数 chain；parameter chain。
     * @return 返回 governance 的处理结果；returns the result of the operation.
     */
    public abstract Publisher<GatewayResponse> governance(
            GatewayFilterChain chain);

    /**
     * 中文说明：执行 invoke 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invoke operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.invoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 invoke 的处理结果；returns the result of the operation.
     */
    public abstract Publisher<GatewayResponse> invoke();

    /**
     * 中文说明：执行 mapFailure 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the map failure operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.mapFailure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param failure 参数 failure；parameter failure。
     * @return 返回 mapFailure 的处理结果；returns the result of the operation.
     */
    public abstract GatewayOutboundHttpResponse mapFailure(
            Throwable failure);

    /**
     * 中文说明：执行 respond 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the respond operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.respond(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param outbound 参数 outbound；parameter outbound。
     * @return 返回 respond 的处理结果；returns the result of the operation.
     */
    protected final Publisher<GatewayResponse> respond(
            GatewayOutboundHttpResponse outbound) {
        response = new GatewayHttpBridgeResponse(
                outbound,
                request.traceId()
        );
        return Mono.just(response);
    }

    /**
     * 中文说明：执行 respondWebSocket 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the respond web socket operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.respondWebSocket(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param result 参数 result；parameter result。
     * @return 返回 respondWebSocket 的处理结果；returns the result of the operation.
     */
    protected final Publisher<GatewayResponse> respondWebSocket(
            GatewayWebSocketHandshakeResult result) {
        webSocketResult = result;
        response = new GatewayWebSocketBridgeResponse(
                result,
                request.traceId()
        );
        return Mono.just(response);
    }

    /**
     * 中文说明：执行 fail 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the fail operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.fail(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param failure 参数 failure；parameter failure。
     * @return 返回 fail 的处理结果；returns the result of the operation.
     */
    final GatewayResponse fail(Throwable failure) {
        response = new GatewayHttpBridgeResponse(
                mapFailure(failure),
                request.traceId()
        );
        return response;
    }

    /**
     * 中文说明：执行 outbound 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the outbound operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.outbound(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 outbound 的处理结果；returns the result of the operation.
     */
    final GatewayOutboundHttpResponse outbound() {
        if (!(response instanceof GatewayHttpBridgeResponse bridge)) {
            throw new IllegalStateException(
                    "gateway HTTP pipeline produced no response"
            );
        }
        return bridge.outbound();
    }

    /**
     * 中文说明：执行 webSocketResult 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the web socket result operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.webSocketResult(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 webSocketResult 的处理结果；returns the result of the operation.
     */
    final GatewayWebSocketHandshakeResult webSocketResult() {
        if (webSocketResult != null) {
            return webSocketResult;
        }
        if (response instanceof GatewayHttpBridgeResponse bridge) {
            return GatewayWebSocketHandshakeResult.rejected(
                    bridge.outbound().status(),
                    "GATEWAY_WEBSOCKET_REQUEST_REJECTED",
                    "gateway WebSocket request rejected before handshake"
            );
        }
        throw new IllegalStateException(
                "gateway WebSocket pipeline produced no handshake result"
        );
    }

    /**
     * 中文说明：执行 请求 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the request operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.request(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 请求 的处理结果；returns the result of the operation.
     */
    @Override
    public final GatewayRequest request() {
        return request;
    }

    /**
     * 中文说明：执行 context 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the context operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.context(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 context 的处理结果；returns the result of the operation.
     */
    @Override
    public final GatewayContext context() {
        return context;
    }

    /**
     * 中文说明：执行 响应 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the response operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.response(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 响应 的处理结果；returns the result of the operation.
     */
    @Override
    public final GatewayResponse response() {
        if (response == null) {
            return new GatewayHttpBridgeResponse(
                    GatewayOutboundHttpResponse.text(
                            500,
                            "gateway pipeline is incomplete"
                    ),
                    request.traceId()
            );
        }
        return response;
    }

    /**
     * 中文说明：执行 inbound 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the inbound operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.inbound(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 inbound 的处理结果；returns the result of the operation.
     */
    protected final GatewayInboundHttpRequest inbound() {
        return inbound;
    }

    /**
     * 中文说明：执行 请求 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the request operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.request(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @param gatewayContext 参数 网关Context；parameter gateway context。
     * @return 返回 请求 的处理结果；returns the result of the operation.
     */
    private GatewayRequest request(
            GatewayInboundHttpRequest source,
            GatewayContext gatewayContext) {
        GatewayTraceContext selected = gatewayContext == null
                ? GatewayTraceContext.fromHeaders(
                header(source.headers(), "traceparent", null),
                header(source.headers(), "tracestate", null),
                header(source.headers(), "x-egon-request-id", null)
        )
                : null;
        String traceId = gatewayContext == null
                ? selected.traceId()
                : gatewayContext.traceId();
        String requestId = gatewayContext == null
                ? selected.requestId()
                : gatewayContext.requestId();
        AccessZone accessZone = gatewayContext == null
                ? AccessZone.INTERNAL
                : gatewayContext.accessZone();
        ImmutableGatewayHeaders headers = new ImmutableGatewayHeaders(
                source.headers()
        );
        return new GatewayRequest() {
            /**
             * 中文说明：执行 请求Id 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the request id operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.requestId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @return 返回 请求Id 的处理结果；returns the result of the operation.
             */
            @Override
            public String requestId() {
                return requestId;
            }

            /**
             * 中文说明：执行 traceId 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the trace id operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.traceId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @return 返回 traceId 的处理结果；returns the result of the operation.
             */
            @Override
            public String traceId() {
                return traceId;
            }

            /**
             * 中文说明：执行 protocol 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the protocol operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.protocol(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @return 返回 protocol 的处理结果；returns the result of the operation.
             */
            @Override
            public GatewayProtocol protocol() {
                return GatewayProtocol.HTTP;
            }

            /**
             * 中文说明：执行 accessZone 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the access zone operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.accessZone(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @return 返回 accessZone 的处理结果；returns the result of the operation.
             */
            @Override
            public AccessZone accessZone() {
                return accessZone;
            }

            /**
             * 中文说明：执行 headers 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the headers operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.headers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @return 返回 headers 的处理结果；returns the result of the operation.
             */
            @Override
            public GatewayHeaders headers() {
                return headers;
            }

            /**
             * 中文说明：执行 body 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
             * English summary: Executes the body operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
             *
             * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.body(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
             * @return 返回 body 的处理结果；returns the result of the operation.
             */
            @Override
            public GatewayBody body() {
                return new GatewayBody() {
                    /**
                     * 中文说明：执行 contentLength 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                     * English summary: Executes the content length operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
                     *
                     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.contentLength(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                     * @return 返回 contentLength 的处理结果；returns the result of the operation.
                     */
                    @Override
                    public long contentLength() {
                        return AbstractGatewayHttpStageExchange.this
                                .contentLength(source.headers());
                    }

                    /**
                     * 中文说明：执行 replayable 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                     * English summary: Executes the replayable operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
                     *
                     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.replayable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                     * @return 返回 replayable 的处理结果；returns the result of the operation.
                     */
                    @Override
                    public boolean replayable() {
                        return false;
                    }
                };
            }
        };
    }

    /**
     * 中文说明：执行 contentLength 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the content length operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.contentLength(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @return 返回 contentLength 的处理结果；returns the result of the operation.
     */
    private long contentLength(Map<String, List<String>> headers) {
        String raw = header(headers, "content-length", null);
        if (raw == null) {
            return -1;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /**
     * 中文说明：执行 header 操作；该方法是 {@code AbstractGatewayHttpStageExchange} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the header operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.header(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @param name 参数 name；parameter name。
     * @param defaultValue 参数 default值；parameter default value。
     * @return 返回 header 的处理结果；returns the result of the operation.
     */
    private String header(
            Map<String, List<String>> headers,
            String name,
            String defaultValue) {
        return headers.entrySet().stream()
                .filter(entry -> name.equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst()
                .orElse(defaultValue);
    }

    /**
     * 中文说明：{@code GatewayHttpBridgeResponse} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关HttpBridge响应相关的职责与边界。
     * English summary: {@code GatewayHttpBridgeResponse} is an immutable data carrier in the current Gateway module; it owns the gateway http bridge response-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param outbound 参数 outbound；parameter outbound。
     * @param traceId 参数 traceId；parameter trace id。
     */
    private record GatewayHttpBridgeResponse(
            /**
             * 中文说明：保存 outbound 对应的状态、依赖或配置值；字段类型为 {@code GatewayOutboundHttpResponse}，由 {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by outbound; its type is {@code GatewayOutboundHttpResponse}, and {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayOutboundHttpResponse outbound,
            /**
             * 中文说明：保存 traceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by trace id; its type is {@code String}, and {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse}; do not couple callers to its representation when the owning type exposes an API.
             */
            String traceId
    ) implements GatewayResponse {

        /**
         * 中文说明：执行 result 操作；该方法是 {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the result operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse.result(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 result 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayResult result() {
            if (outbound.status() < 400) {
                return GatewayResult.success();
            }
            return GatewayResult.failure(new GatewayError(
                    "GATEWAY_HTTP_" + outbound.status(),
                    GatewayErrorCategory.UPSTREAM_FAILURE,
                    "Gateway HTTP request failed",
                    traceId,
                    outbound.status() >= 500,
                    Map.of()
            ));
        }

        /**
         * 中文说明：执行 headers 操作；该方法是 {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the headers operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse.headers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 headers 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayHeaders headers() {
            return new ImmutableGatewayHeaders(outbound.headers());
        }

        /**
         * 中文说明：执行 body 操作；该方法是 {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the body operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.GatewayHttpBridgeResponse.body(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 body 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayBody body() {
            return EmptyGatewayBody.INSTANCE;
        }
    }

    /**
     * 中文说明：{@code GatewayWebSocketBridgeResponse} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关WebSocketBridge响应相关的职责与边界。
     * English summary: {@code GatewayWebSocketBridgeResponse} is an immutable data carrier in the current Gateway module; it owns the gateway web socket bridge response-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param handshake 参数 handshake；parameter handshake。
     * @param traceId 参数 traceId；parameter trace id。
     */
    private record GatewayWebSocketBridgeResponse(
            /**
             * 中文说明：保存 handshake 对应的状态、依赖或配置值；字段类型为 {@code GatewayWebSocketHandshakeResult}，由 {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by handshake; its type is {@code GatewayWebSocketHandshakeResult}, and {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse}; do not couple callers to its representation when the owning type exposes an API.
             */
            GatewayWebSocketHandshakeResult handshake,
            /**
             * 中文说明：保存 traceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by trace id; its type is {@code String}, and {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse}; do not couple callers to its representation when the owning type exposes an API.
             */
            String traceId
    ) implements GatewayResponse {

        /**
         * 中文说明：执行 result 操作；该方法是 {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the result operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse.result(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 result 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayResult result() {
            if (handshake instanceof GatewayWebSocketHandshakeResult.Accepted) {
                return GatewayResult.success();
            }
            GatewayWebSocketHandshakeResult.Rejected rejected =
                    (GatewayWebSocketHandshakeResult.Rejected) handshake;
            return GatewayResult.failure(new GatewayError(
                    rejected.errorCode(),
                    GatewayErrorCategory.UPSTREAM_FAILURE,
                    rejected.message(),
                    traceId,
                    rejected.httpStatus() >= 500,
                    Map.of()
            ));
        }

        /**
         * 中文说明：执行 headers 操作；该方法是 {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the headers operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse.headers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 headers 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayHeaders headers() {
            return new ImmutableGatewayHeaders(Map.of());
        }

        /**
         * 中文说明：执行 body 操作；该方法是 {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the body operation; this method is the invocation entry point on {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code AbstractGatewayHttpStageExchange.GatewayWebSocketBridgeResponse.body(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 body 的处理结果；returns the result of the operation.
         */
        @Override
        public GatewayBody body() {
            return EmptyGatewayBody.INSTANCE;
        }
    }
}
