package top.egon.cola.component.gateway.engine.mcp.adapter.remote;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelOption;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteMcpClient;
import top.egon.cola.component.gateway.mcp.remote.domain.McpRemoteEndpointValidator;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bounded Streamable HTTP client for Stable, RC and fixed legacy endpoints.
 * 补充说明 / Supplementary summary: {@code ReactorNettyRemoteMcpClient} 是客户端，位于当前 Gateway 模块的相关包中，负责ReactorNetty远程MCP客户端相关的职责与边界。
 * English supplement: {@code ReactorNettyRemoteMcpClient} is a reactor netty remote mcp client client in the current Gateway module; it owns the reactor netty remote mcp client-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class ReactorNettyRemoteMcpClient
        implements RemoteMcpClient {

    /**
     * 中文说明：表示 MAX响应BYTES 这一固定值；它属于 {@code ReactorNettyRemoteMcpClient} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value max response bytes; it is a state, type, or protocol value of {@code ReactorNettyRemoteMcpClient} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyRemoteMcpClient} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyRemoteMcpClient}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final int MAX_RESPONSE_BYTES = 16 * 1024 * 1024;

    /**
     * 中文说明：表示 OBJECTMAP 这一固定值；它属于 {@code ReactorNettyRemoteMcpClient} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value object map; it is a state, type, or protocol value of {@code ReactorNettyRemoteMcpClient} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyRemoteMcpClient} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyRemoteMcpClient}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final TypeReference<Map<String, Object>> OBJECT_MAP =
            new TypeReference<>() {
            };

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code ReactorNettyRemoteMcpClient} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code ReactorNettyRemoteMcpClient} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyRemoteMcpClient} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyRemoteMcpClient}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存 clients 对应的状态、依赖或配置值；字段类型为 {@code TlsClientProvider}，由 {@code ReactorNettyRemoteMcpClient} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clients; its type is {@code TlsClientProvider}, and {@code ReactorNettyRemoteMcpClient} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyRemoteMcpClient} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyRemoteMcpClient}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final TlsClientProvider clients;

    /**
     * 中文说明：保存 stable会话Id 对应的状态、依赖或配置值；字段类型为 {@code AtomicReference<String>}，由 {@code ReactorNettyRemoteMcpClient} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by stable session id; its type is {@code AtomicReference<String>}, and {@code ReactorNettyRemoteMcpClient} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ReactorNettyRemoteMcpClient} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyRemoteMcpClient}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicReference<String> stableSessionId =
            new AtomicReference<>();

    /**
     * 中文说明：创建 {@code ReactorNettyRemoteMcpClient} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ReactorNettyRemoteMcpClient} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public ReactorNettyRemoteMcpClient(ObjectMapper objectMapper) {
        this(objectMapper, reference -> {
            if (reference != null) {
                throw new IllegalStateException(
                        "remote MCP mTLS client is not configured"
                );
            }
            return HttpClient.create();
        });
    }

    /**
     * 中文说明：创建 {@code ReactorNettyRemoteMcpClient} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ReactorNettyRemoteMcpClient} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param objectMapper 参数 object映射器；parameter object mapper。
     * @param clients 参数 clients；parameter clients。
     */
    public ReactorNettyRemoteMcpClient(
            ObjectMapper objectMapper,
            TlsClientProvider clients) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        ).copy();
        this.clients = Objects.requireNonNull(clients, "clients");
    }

    /**
     * 中文说明：执行 exchange 操作；该方法是 {@code ReactorNettyRemoteMcpClient} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the exchange operation; this method is the invocation entry point on {@code ReactorNettyRemoteMcpClient} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyRemoteMcpClient.exchange(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 exchange 的处理结果；returns the result of the operation.
     */
    @Override
    public Publisher<ExchangeResponse> exchange(ExchangeRequest request) {
        validateProvider(request);
        if (request.provider().dialect()
                != McpProtocolDialect.STABLE_2025_11_25
                || "initialize".equals(request.method())
                || stableSessionId.get() != null) {
            return perform(request);
        }
        return perform(initialize(request))
                .flatMap(ignored -> perform(request));
    }

    /**
     * 中文说明：执行 perform 操作；该方法是 {@code ReactorNettyRemoteMcpClient} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the perform operation; this method is the invocation entry point on {@code ReactorNettyRemoteMcpClient} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyRemoteMcpClient.perform(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 perform 的处理结果；returns the result of the operation.
     */
    private Mono<ExchangeResponse> perform(ExchangeRequest request) {
        HttpClient client = clients.client(request.tlsProfileReference())
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        timeoutMillis(request.timeout())
                )
                .responseTimeout(request.timeout())
                .headers(headers -> {
                    headers.set("content-type", "application/json");
                    headers.set("accept", "application/json, text/event-stream");
                    request.headers().forEach(headers::set);
                    String session = stableSessionId.get();
                    if (session != null) {
                        headers.set("mcp-session-id", session);
                    }
                });
        String body = encode(request);
        return client.post()
                .uri(request.provider().endpointReference())
                .send((ignored, outbound) -> outbound.sendString(
                        Mono.just(body)
                ))
                .response((response, content) -> {
                    int status = response.status().code();
                    String session = response.responseHeaders().get(
                            "mcp-session-id"
                    );
                    if (session != null && !session.isBlank()) {
                        stableSessionId.set(session.trim());
                    }
                    Map<String, String> headers = responseHeaders(
                            response.responseHeaders()
                    );
                    return content.collect(
                                    BoundedResponseBody::new,
                                    BoundedResponseBody::append
                            )
                            .map(BoundedResponseBody::bytes)
                            .map(bytes -> decode(status, bytes, headers));
                })
                .single()
                .timeout(request.timeout());
    }

    /**
     * 中文说明：执行 initialize 操作；该方法是 {@code ReactorNettyRemoteMcpClient} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the initialize operation; this method is the invocation entry point on {@code ReactorNettyRemoteMcpClient} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyRemoteMcpClient.initialize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param original 参数 original；parameter original。
     * @return 返回 initialize 的处理结果；returns the result of the operation.
     */
    private ExchangeRequest initialize(ExchangeRequest original) {
        return new ExchangeRequest(
                original.provider(),
                "init-" + original.id(),
                "initialize",
                Map.of(
                        "protocolVersion",
                        original.provider().dialect().protocolVersion(),
                        "capabilities",
                        Map.of(),
                        "clientInfo",
                        Map.of(
                                "name", "egon-cola-gateway",
                                "version", "5.3.2"
                        )
                ),
                Map.of(),
                original.headers(),
                original.tlsProfileReference(),
                original.timeout()
        );
    }

    /**
     * 中文说明：执行 encode 操作；该方法是 {@code ReactorNettyRemoteMcpClient} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the encode operation; this method is the invocation entry point on {@code ReactorNettyRemoteMcpClient} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyRemoteMcpClient.encode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     * @return 返回 encode 的处理结果；returns the result of the operation.
     */
    private String encode(ExchangeRequest request) {
        try {
            LinkedHashMap<String, Object> params = new LinkedHashMap<>(
                    request.params()
            );
            if (!request.meta().isEmpty()) {
                params.put("_meta", request.meta());
            }
            return objectMapper.writeValueAsString(Map.of(
                    "jsonrpc", "2.0",
                    "id", request.id(),
                    "method", request.method(),
                    "params", params
            ));
        } catch (Exception failure) {
            throw new IllegalStateException(
                    "remote MCP request encoding failed",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 decode 操作；该方法是 {@code ReactorNettyRemoteMcpClient} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the decode operation; this method is the invocation entry point on {@code ReactorNettyRemoteMcpClient} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyRemoteMcpClient.decode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @param bytes 参数 bytes；parameter bytes。
     * @param headers 参数 headers；parameter headers。
     * @return 返回 decode 的处理结果；returns the result of the operation.
     */
    private ExchangeResponse decode(
            int status,
            byte[] bytes,
            Map<String, String> headers) {
        if (status < 200 || status >= 300) {
            return ExchangeResponse.failure(
                    -32030,
                    "remote MCP HTTP status " + status,
                    Map.of("status", status),
                    headers
            );
        }
        try {
            JsonNode root = objectMapper.readTree(bytes);
            if (root == null || !root.isObject()) {
                throw new IllegalStateException(
                        "remote MCP response must be an object"
                );
            }
            JsonNode error = root.get("error");
            if (error != null && error.isObject()) {
                int code = error.path("code").asInt(-32030);
                String message = error.path("message").asText(
                        "remote MCP request failed"
                );
                Map<String, Object> data = error.has("data")
                        && error.get("data").isObject()
                        ? objectMapper.convertValue(
                                error.get("data"),
                                OBJECT_MAP
                        )
                        : Map.of();
                return ExchangeResponse.failure(
                        code,
                        message,
                        data,
                        headers
                );
            }
            JsonNode result = root.get("result");
            if (result == null || !result.isObject()) {
                throw new IllegalStateException(
                        "remote MCP result must be an object"
                );
            }
            return ExchangeResponse.success(
                    objectMapper.convertValue(result, OBJECT_MAP),
                    headers
            );
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException(
                    "remote MCP response decoding failed",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 validate提供方 操作；该方法是 {@code ReactorNettyRemoteMcpClient} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the validate provider operation; this method is the invocation entry point on {@code ReactorNettyRemoteMcpClient} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyRemoteMcpClient.validateProvider(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param request 参数 请求；parameter request。
     */
    private void validateProvider(ExchangeRequest request) {
        McpRemoteEndpointValidator.requireSafe(
                request.provider().endpointReference()
        );
        String transport = request.provider().transportType()
                .toUpperCase(Locale.ROOT);
        if (!transport.equals("STREAMABLE_HTTP")
                && !transport.equals("LEGACY_SSE")) {
            throw new IllegalArgumentException(
                    "remote MCP transport is not supported by the HTTP client"
            );
        }
    }

    /**
     * 中文说明：执行 响应Headers 操作；该方法是 {@code ReactorNettyRemoteMcpClient} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the response headers operation; this method is the invocation entry point on {@code ReactorNettyRemoteMcpClient} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyRemoteMcpClient.responseHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param source 参数 source；parameter source。
     * @return 返回 响应Headers 的处理结果；returns the result of the operation.
     */
    private Map<String, String> responseHeaders(
            io.netty.handler.codec.http.HttpHeaders source) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        source.forEach(entry -> result.putIfAbsent(
                entry.getKey().toLowerCase(Locale.ROOT),
                entry.getValue()
        ));
        return Map.copyOf(result);
    }

    /**
     * 中文说明：执行 超时Millis 操作；该方法是 {@code ReactorNettyRemoteMcpClient} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the timeout millis operation; this method is the invocation entry point on {@code ReactorNettyRemoteMcpClient} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyRemoteMcpClient.timeoutMillis(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 超时Millis 的处理结果；returns the result of the operation.
     */
    private int timeoutMillis(Duration timeout) {
        return Math.toIntExact(Math.min(
                Integer.MAX_VALUE,
                Math.max(1L, timeout.toMillis())
        ));
    }

    /**
     * 中文说明：{@code TlsClientProvider} 是接口契约，位于当前 Gateway 模块的相关包中，负责Tls客户端提供方相关的职责与边界。
     * English summary: {@code TlsClientProvider} is an interface contract in the current Gateway module; it owns the tls client provider-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    @FunctionalInterface
    public interface TlsClientProvider {

        /**
         * 中文说明：执行 客户端 操作；该方法是 {@code ReactorNettyRemoteMcpClient.TlsClientProvider} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the client operation; this method is the invocation entry point on {@code ReactorNettyRemoteMcpClient.TlsClientProvider} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyRemoteMcpClient.TlsClientProvider.client(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param tlsProfileReference 参数 tlsProfileReference；parameter tls profile reference。
         * @return 返回 客户端 的处理结果；returns the result of the operation.
         */
        HttpClient client(String tlsProfileReference);
    }

    /**
     * 中文说明：{@code BoundedResponseBody} 是类型，位于当前 Gateway 模块的相关包中，负责Bounded响应Body相关的职责与边界。
     * English summary: {@code BoundedResponseBody} is a type in the current Gateway module; it owns the bounded response body-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class BoundedResponseBody {

        /**
         * 中文说明：保存 output 对应的状态、依赖或配置值；字段类型为 {@code ByteArrayOutputStream}，由 {@code ReactorNettyRemoteMcpClient.BoundedResponseBody} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by output; its type is {@code ByteArrayOutputStream}, and {@code ReactorNettyRemoteMcpClient.BoundedResponseBody} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ReactorNettyRemoteMcpClient.BoundedResponseBody} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ReactorNettyRemoteMcpClient.BoundedResponseBody}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        /**
         * 中文说明：执行 append 操作；该方法是 {@code ReactorNettyRemoteMcpClient.BoundedResponseBody} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the append operation; this method is the invocation entry point on {@code ReactorNettyRemoteMcpClient.BoundedResponseBody} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyRemoteMcpClient.BoundedResponseBody.append(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param buffer 参数 缓冲区；parameter buffer。
         */
        private void append(ByteBuf buffer) {
            int readableBytes = buffer.readableBytes();
            if ((long) output.size() + readableBytes > MAX_RESPONSE_BYTES) {
                throw new IllegalStateException(
                        "remote MCP response exceeds its maximum size"
                );
            }
            byte[] chunk = new byte[readableBytes];
            buffer.getBytes(buffer.readerIndex(), chunk);
            output.writeBytes(chunk);
        }

        /**
         * 中文说明：执行 bytes 操作；该方法是 {@code ReactorNettyRemoteMcpClient.BoundedResponseBody} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the bytes operation; this method is the invocation entry point on {@code ReactorNettyRemoteMcpClient.BoundedResponseBody} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ReactorNettyRemoteMcpClient.BoundedResponseBody.bytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 bytes 的处理结果；returns the result of the operation.
         */
        private byte[] bytes() {
            return output.toByteArray();
        }
    }
}
