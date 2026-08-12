package top.egon.cola.component.gateway.engine.http;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.channel.ChannelOption;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import top.egon.cola.component.gateway.engine.http.buffer.GatewayDataBufferOwnership;
import top.egon.cola.component.gateway.engine.http.buffer.GatewayDataBufferPipeline;
import top.egon.cola.component.gateway.engine.transport.GatewayTransportTimeouts;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 中文说明：{@code ReactorNettyHttpUpstreamAdapter} 是适配器，位于当前 Gateway 模块的相关包中，负责ReactorNettyHttpUpstreamAdapter相关的职责与边界。
 * English summary: {@code ReactorNettyHttpUpstreamAdapter} is a reactor netty http upstream adapter adapter in the current Gateway module; it owns the reactor netty http upstream adapter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class ReactorNettyHttpUpstreamAdapter
        implements HttpUpstreamAdapter, AutoCloseable {

    /**
     * 中文说明：表示 缓冲区工厂 这一固定值；它属于 {@code ReactorNettyHttpUpstreamAdapter} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value buffer factory; it is a state, type, or protocol value of {@code ReactorNettyHttpUpstreamAdapter} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyHttpUpstreamAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyHttpUpstreamAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final NettyDataBufferFactory BUFFER_FACTORY =
            new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT);

    /**
     * 中文说明：表示 FORBIDDEN 这一固定值；它属于 {@code ReactorNettyHttpUpstreamAdapter} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value forbidden; it is a state, type, or protocol value of {@code ReactorNettyHttpUpstreamAdapter} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyHttpUpstreamAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyHttpUpstreamAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Set<String> FORBIDDEN = Set.of(
            "host",
            "x-forwarded-for",
            "x-forwarded-host",
            "x-forwarded-proto",
            "x-egon-principal",
            "x-ddc-secret",
            "x-admin-token"
    );

    /**
     * 中文说明：保存 connection提供方 对应的状态、依赖或配置值；字段类型为 {@code ConnectionProvider}，由 {@code ReactorNettyHttpUpstreamAdapter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by connection provider; its type is {@code ConnectionProvider}, and {@code ReactorNettyHttpUpstreamAdapter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyHttpUpstreamAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyHttpUpstreamAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ConnectionProvider connectionProvider;

    /**
     * 中文说明：保存 客户端 对应的状态、依赖或配置值；字段类型为 {@code HttpClient}，由 {@code ReactorNettyHttpUpstreamAdapter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by client; its type is {@code HttpClient}, and {@code ReactorNettyHttpUpstreamAdapter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyHttpUpstreamAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyHttpUpstreamAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final HttpClient client;

    /**
     * 中文说明：保存 header过滤器 对应的状态、依赖或配置值；字段类型为 {@code GatewayHeaderFilter}，由 {@code ReactorNettyHttpUpstreamAdapter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by header filter; its type is {@code GatewayHeaderFilter}, and {@code ReactorNettyHttpUpstreamAdapter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyHttpUpstreamAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyHttpUpstreamAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayHeaderFilter headerFilter =
            new GatewayHeaderFilter();

    /**
     * 中文说明：创建 {@code ReactorNettyHttpUpstreamAdapter} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ReactorNettyHttpUpstreamAdapter} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param maxConnections 参数 maxConnections；parameter max connections。
     * @param pendingAcquireMaxCount 参数 pendingAcquireMaxCount；parameter pending acquire max count。
     * @param idleTimeout 参数 idle超时；parameter idle timeout。
     */
    public ReactorNettyHttpUpstreamAdapter(
            int maxConnections,
            int pendingAcquireMaxCount,
            Duration idleTimeout) {
        connectionProvider = ConnectionProvider.builder("gateway-http-upstream")
                .maxConnections(maxConnections)
                .pendingAcquireMaxCount(pendingAcquireMaxCount)
                .maxIdleTime(idleTimeout)
                .build();
        client = HttpClient.create(connectionProvider);
    }

    /**
     * 中文说明：执行 invoke 操作；该方法是 {@code ReactorNettyHttpUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invoke operation; this method is the invocation entry point on {@code ReactorNettyHttpUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyHttpUpstreamAdapter.invoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 invoke 的处理结果；returns the result of the operation.
     */
    @Override
    public Mono<GatewayOutboundHttpResponse> invoke(
            HttpUpstreamRequest request) {
        String scheme = request.provider().secure() ? "https" : "http";
        long startedNanos = System.nanoTime();
        Mono<GatewayOutboundHttpResponse> result = client
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        timeoutMillis(request.connectTimeout())
                )
                .responseTimeout(request.responseHeaderTimeout())
                .headers(headers -> {
                    headerFilter.requestHeaders(request.headers())
                            .forEach((name, values) -> {
                        if (!FORBIDDEN.contains(name.toLowerCase(Locale.ROOT))) {
                            values.forEach(value -> headers.add(name, value));
                        }
                    });
                    headers.set("host", request.provider().host()
                            + ":" + request.provider().port());
                })
                .request(HttpMethod.valueOf(request.method()))
                .uri(scheme
                        + "://"
                        + request.provider().host()
                        + ":"
                        + request.provider().port()
                        + request.pathAndQuery())
                .send((ignored, outbound) ->
                        outbound.send(
                                GatewayDataBufferPipeline
                                        .releaseOnDiscardOrCancel(
                                                GatewayTransportTimeouts
                                                        .requestIdle(
                                                                request.body(),
                                                                request.streamIdleTimeout()
                                                        )
                                        )
                                        .map(buffer ->
                                                GatewayDataBufferOwnership
                                                        .transferToNetty(
                                                                buffer,
                                                                outbound.alloc()
                                                        ))
                        ))
                .responseConnection((response, connection) -> {
                    AtomicBoolean disposed = new AtomicBoolean();
                    Runnable dispose = () -> {
                        if (disposed.compareAndSet(false, true)) {
                            connection.dispose();
                        }
                    };
                    Flux<DataBuffer> body = GatewayTransportTimeouts
                            .responseIdle(
                                    connection.inbound()
                                            .receive()
                                            .<DataBuffer>map(buffer ->
                                                    GatewayDataBufferOwnership
                                                            .retainAndWrap(
                                                                    BUFFER_FACTORY,
                                                                    buffer
                                                            )),
                                    request.streamIdleTimeout()
                            );
                    body = GatewayTransportTimeouts.total(
                            body,
                            remainingTotal(request, startedNanos)
                    )
                            .doFinally(ignored -> dispose.run());
                    return Flux.just(new GatewayOutboundHttpResponse(
                            response.status().code(),
                            responseHeaders(response.responseHeaders()),
                            body,
                            GatewayHttpFlushMode.STANDARD,
                            dispose
                    ));
                })
                .single();
        return GatewayTransportTimeouts.total(
                result,
                request.totalTimeout()
        );
    }

    /**
     * 中文说明：执行 close 操作；该方法是 {@code ReactorNettyHttpUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the close operation; this method is the invocation entry point on {@code ReactorNettyHttpUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyHttpUpstreamAdapter.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     */
    @Override
    public void close() {
        connectionProvider.disposeLater().block();
    }

    /**
     * 中文说明：执行 响应Headers 操作；该方法是 {@code ReactorNettyHttpUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the response headers operation; this method is the invocation entry point on {@code ReactorNettyHttpUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyHttpUpstreamAdapter.responseHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @return 返回 响应Headers 的处理结果；returns the result of the operation.
     */
    private Map<String, List<String>> responseHeaders(
            io.netty.handler.codec.http.HttpHeaders headers) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        headers.forEach(entry -> {
            String name = entry.getKey().toLowerCase(Locale.ROOT);
            if (!FORBIDDEN.contains(name)) {
                result.computeIfAbsent(
                        name,
                        ignored -> new ArrayList<>()
                ).add(entry.getValue());
            }
        });
        return headerFilter.responseHeaders(result);
    }

    /**
     * 中文说明：执行 超时Millis 操作；该方法是 {@code ReactorNettyHttpUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the timeout millis operation; this method is the invocation entry point on {@code ReactorNettyHttpUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyHttpUpstreamAdapter.timeoutMillis(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 超时Millis 的处理结果；returns the result of the operation.
     */
    private int timeoutMillis(Duration timeout) {
        return Math.toIntExact(Math.min(
                Integer.MAX_VALUE,
                Math.max(1, timeout.toMillis())
        ));
    }

    /**
     * 中文说明：执行 remainingTotal 操作；该方法是 {@code ReactorNettyHttpUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remaining total operation; this method is the invocation entry point on {@code ReactorNettyHttpUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyHttpUpstreamAdapter.remainingTotal(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @param startedNanos 参数 startedNanos；parameter started nanos。
     * @return 返回 remainingTotal 的处理结果；returns the result of the operation.
     */
    private java.util.Optional<Duration> remainingTotal(
            HttpUpstreamRequest request,
            long startedNanos) {
        return request.totalTimeout().map(total -> {
            long elapsed = Math.max(0, System.nanoTime() - startedNanos);
            long remaining = Math.max(1, total.toNanos() - elapsed);
            return Duration.ofNanos(remaining);
        });
    }
}
