package top.egon.cola.component.gateway.engine.operation;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.http.NormalizedHttpRequest;
import top.egon.cola.component.gateway.core.operation.GatewayInvocationResult;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;
import top.egon.cola.component.gateway.core.route.HttpRouteMatch;
import top.egon.cola.component.gateway.core.route.RuntimeHttpRoute;
import top.egon.cola.component.gateway.core.transport.EffectiveGatewayTransportPolicy;
import top.egon.cola.component.gateway.engine.http.GatewayOutboundHttpResponse;
import top.egon.cola.component.gateway.engine.http.HttpUpstreamAdapter;
import top.egon.cola.component.gateway.engine.http.HttpUpstreamRequest;
import top.egon.cola.component.gateway.engine.rpc.HttpRpcUpstreamAdapter;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Adapts direct operation requests to the existing HTTP and HTTP-to-RPC
 * upstream adapters.
 * 补充说明 / Supplementary summary: {@code DefaultGatewayOperationTransport} 是类型，位于当前 Gateway 模块的相关包中，负责Default网关操作传输相关的职责与边界。
 * English supplement: {@code DefaultGatewayOperationTransport} is a type in the current Gateway module; it owns the default gateway operation transport-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class DefaultGatewayOperationTransport
        implements EngineGatewayOperationInvoker.OperationTransport {

    /**
     * 中文说明：表示 缓冲区工厂 这一固定值；它属于 {@code DefaultGatewayOperationTransport} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value buffer factory; it is a state, type, or protocol value of {@code DefaultGatewayOperationTransport} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayOperationTransport} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayOperationTransport}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final DefaultDataBufferFactory BUFFER_FACTORY =
            DefaultDataBufferFactory.sharedInstance;

    /**
     * 中文说明：保存 http 对应的状态、依赖或配置值；字段类型为 {@code HttpUpstreamAdapter}，由 {@code DefaultGatewayOperationTransport} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by http; its type is {@code HttpUpstreamAdapter}, and {@code DefaultGatewayOperationTransport} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayOperationTransport} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayOperationTransport}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final HttpUpstreamAdapter http;

    /**
     * 中文说明：保存 rpc 对应的状态、依赖或配置值；字段类型为 {@code HttpRpcUpstreamAdapter}，由 {@code DefaultGatewayOperationTransport} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rpc; its type is {@code HttpRpcUpstreamAdapter}, and {@code DefaultGatewayOperationTransport} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayOperationTransport} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayOperationTransport}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final HttpRpcUpstreamAdapter rpc;

    /**
     * 中文说明：保存 maximum响应Bytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code DefaultGatewayOperationTransport} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by maximum response bytes; its type is {@code long}, and {@code DefaultGatewayOperationTransport} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code DefaultGatewayOperationTransport} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code DefaultGatewayOperationTransport}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final long maximumResponseBytes;

    /**
     * 中文说明：创建 {@code DefaultGatewayOperationTransport} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code DefaultGatewayOperationTransport} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param http 参数 http；parameter http。
     * @param rpc 参数 rpc；parameter rpc。
     * @param maximumResponseBytes 参数 maximum响应Bytes；parameter maximum response bytes。
     */
    public DefaultGatewayOperationTransport(
            HttpUpstreamAdapter http,
            HttpRpcUpstreamAdapter rpc,
            long maximumResponseBytes) {
        this.http = Objects.requireNonNull(http, "http");
        this.rpc = Objects.requireNonNull(rpc, "rpc");
        if (maximumResponseBytes <= 0) {
            throw new IllegalArgumentException(
                    "maximumResponseBytes must be positive"
            );
        }
        this.maximumResponseBytes = maximumResponseBytes;
    }

    /**
     * 中文说明：执行 invoke 操作；该方法是 {@code DefaultGatewayOperationTransport} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invoke operation; this method is the invocation entry point on {@code DefaultGatewayOperationTransport} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayOperationTransport.invoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @param request 参数 请求；parameter request。
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 invoke 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<GatewayInvocationResult> invoke(
            ProviderInstance provider,
            EngineGatewayOperationInvoker.PreparedRequest request,
            Duration timeout) {
        Mono<GatewayOutboundHttpResponse> response =
                provider.serviceKey().protocolType()
                        == ProviderProtocolType.RPC
                        ? rpc(provider, request, timeout)
                        : http(provider, request, timeout);
        return response.flatMap(this::aggregate);
    }

    /**
     * 中文说明：执行 http 操作；该方法是 {@code DefaultGatewayOperationTransport} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the http operation; this method is the invocation entry point on {@code DefaultGatewayOperationTransport} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayOperationTransport.http(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @param request 参数 请求；parameter request。
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 http 的处理结果；returns the result of the operation.
     */
    private Mono<GatewayOutboundHttpResponse> http(
            ProviderInstance provider,
            EngineGatewayOperationInvoker.PreparedRequest request,
            Duration timeout) {
        return http.invoke(new HttpUpstreamRequest(
                provider,
                request.method(),
                request.pathAndQuery(),
                request.headers(),
                Flux.defer(() -> request.body().length == 0
                        ? Flux.empty()
                        : Flux.just(BUFFER_FACTORY.wrap(request.body()))),
                timeout,
                true
        ));
    }

    /**
     * 中文说明：执行 rpc 操作；该方法是 {@code DefaultGatewayOperationTransport} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rpc operation; this method is the invocation entry point on {@code DefaultGatewayOperationTransport} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayOperationTransport.rpc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @param request 参数 请求；parameter request。
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 rpc 的处理结果；returns the result of the operation.
     */
    private Mono<GatewayOutboundHttpResponse> rpc(
            ProviderInstance provider,
            EngineGatewayOperationInvoker.PreparedRequest request,
            Duration timeout) {
        String rawPath = rawPath(request.pathAndQuery());
        String rawQuery = rawQuery(request.pathAndQuery());
        RuntimeHttpRoute route = syntheticRoute(request, provider, rawPath);
        return rpc.invoke(
                new HttpRouteMatch(route, request.pathVariables()),
                provider,
                new NormalizedHttpRequest(
                        request.method(),
                        "mcp.local",
                        rawPath,
                        rawPath,
                        rawQuery,
                        request.headers()
                ),
                request.body(),
                request.headers(),
                timeout
        );
    }

    /**
     * 中文说明：执行 synthetic路由 操作；该方法是 {@code DefaultGatewayOperationTransport} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the synthetic route operation; this method is the invocation entry point on {@code DefaultGatewayOperationTransport} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayOperationTransport.syntheticRoute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param provider 参数 提供方；parameter provider。
     * @param rawPath 参数 rawPath；parameter raw path。
     * @return 返回 synthetic路由 的处理结果；returns the result of the operation.
     */
    private RuntimeHttpRoute syntheticRoute(
            EngineGatewayOperationInvoker.PreparedRequest request,
            ProviderInstance provider,
            String rawPath) {
        Map<String, String> metadata = new LinkedHashMap<>(
                request.operation().attributes()
        );
        metadata.put("methodIdentity", request.operation().methodIdentity());
        if (request.operation().requestSchema() != null) {
            metadata.put(
                    "requestSchema",
                    request.operation().requestSchema()
            );
        }
        if (request.operation().responseSchema() != null) {
            metadata.put(
                    "responseSchema",
                    request.operation().responseSchema()
            );
        }
        return new RuntimeHttpRoute(
                "mcp:" + request.operation().operationId(),
                request.operation().operationId(),
                "mcp-local",
                Set.of(AccessZone.INTERNAL),
                "*",
                Set.of(request.method()),
                rawPath,
                false,
                provider.serviceKey(),
                request.operation().policyRefs(),
                0,
                responseMode(request.operation().responseMode()),
                Map.copyOf(metadata),
                EffectiveGatewayTransportPolicy.legacy()
        );
    }

    /**
     * 中文说明：执行 响应Mode 操作；该方法是 {@code DefaultGatewayOperationTransport} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the response mode operation; this method is the invocation entry point on {@code DefaultGatewayOperationTransport} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayOperationTransport.responseMode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 响应Mode 的处理结果；returns the result of the operation.
     */
    private GatewayResponseMode responseMode(String value) {
        try {
            return GatewayResponseMode.valueOf(value.toUpperCase(
                    Locale.ROOT
            ));
        } catch (IllegalArgumentException ignored) {
            return GatewayResponseMode.TRANSPARENT;
        }
    }

    /**
     * 中文说明：执行 aggregate 操作；该方法是 {@code DefaultGatewayOperationTransport} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the aggregate operation; this method is the invocation entry point on {@code DefaultGatewayOperationTransport} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayOperationTransport.aggregate(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param response 参数 响应；parameter response。
     * @return 返回 aggregate 的处理结果；returns the result of the operation.
     */
    private Mono<GatewayInvocationResult> aggregate(
            GatewayOutboundHttpResponse response) {
        int limit = (int) Math.min(Integer.MAX_VALUE, maximumResponseBytes);
        Mono<byte[]> body = DataBufferUtils.join(response.body(), limit)
                .map(this::bytes)
                .switchIfEmpty(Mono.just(new byte[0]));
        return body.map(bytes -> new GatewayInvocationResult(
                response.status(),
                response.headers(),
                bytes
        ));
    }

    /**
     * 中文说明：执行 bytes 操作；该方法是 {@code DefaultGatewayOperationTransport} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bytes operation; this method is the invocation entry point on {@code DefaultGatewayOperationTransport} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayOperationTransport.bytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param buffer 参数 缓冲区；parameter buffer。
     * @return 返回 bytes 的处理结果；returns the result of the operation.
     */
    private byte[] bytes(DataBuffer buffer) {
        try {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            return bytes;
        } finally {
            DataBufferUtils.release(buffer);
        }
    }

    /**
     * 中文说明：执行 rawPath 操作；该方法是 {@code DefaultGatewayOperationTransport} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the raw path operation; this method is the invocation entry point on {@code DefaultGatewayOperationTransport} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayOperationTransport.rawPath(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param pathAndQuery 参数 pathAndQuery；parameter path and query。
     * @return 返回 rawPath 的处理结果；returns the result of the operation.
     */
    private String rawPath(String pathAndQuery) {
        int query = pathAndQuery.indexOf('?');
        return query < 0 ? pathAndQuery : pathAndQuery.substring(0, query);
    }

    /**
     * 中文说明：执行 rawQuery 操作；该方法是 {@code DefaultGatewayOperationTransport} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the raw query operation; this method is the invocation entry point on {@code DefaultGatewayOperationTransport} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code DefaultGatewayOperationTransport.rawQuery(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param pathAndQuery 参数 pathAndQuery；parameter path and query。
     * @return 返回 rawQuery 的处理结果；returns the result of the operation.
     */
    private String rawQuery(String pathAndQuery) {
        int query = pathAndQuery.indexOf('?');
        return query < 0 ? "" : pathAndQuery.substring(query + 1);
    }
}
