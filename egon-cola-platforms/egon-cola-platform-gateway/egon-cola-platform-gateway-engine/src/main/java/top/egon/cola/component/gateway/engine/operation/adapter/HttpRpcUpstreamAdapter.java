package top.egon.cola.component.gateway.engine.operation.adapter;

import top.egon.cola.component.gateway.engine.http.service.GatewayOutboundHttpResponse;

import top.egon.cola.component.gateway.engine.rpc.adapter.ProtobufDescriptorRegistry;
import top.egon.cola.component.gateway.engine.rpc.adapter.RpcProviderChannelCache;
import top.egon.cola.component.gateway.engine.rpc.domain.RawByteMarshaller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.contract.rule.GatewayRpcDescriptor;
import top.egon.cola.component.gateway.core.http.NormalizedHttpRequest;
import top.egon.cola.component.gateway.core.provider.ProviderInstance;
import top.egon.cola.component.gateway.core.route.HttpRouteMatch;
import top.egon.cola.component.gateway.engine.rule.domain.CompiledGatewayRules;
import top.egon.cola.component.rpc.context.invocation.RpcMetadataKeys;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 中文说明：{@code HttpRpcUpstreamAdapter} 是适配器，位于当前 Gateway 模块的相关包中，负责HttpRpcUpstreamAdapter相关的职责与边界。
 * English summary: {@code HttpRpcUpstreamAdapter} is a http rpc upstream adapter adapter in the current Gateway module; it owns the http rpc upstream adapter-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class HttpRpcUpstreamAdapter {

    /**
     * 中文说明：表示 授权 这一固定值；它属于 {@code HttpRpcUpstreamAdapter} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value authorization; it is a state, type, or protocol value of {@code HttpRpcUpstreamAdapter} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code HttpRpcUpstreamAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpRpcUpstreamAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of(
            "authorization",
            Metadata.ASCII_STRING_MARSHALLER
    );

    /**
     * 中文说明：保存 rules 对应的状态、依赖或配置值；字段类型为 {@code Supplier<CompiledGatewayRules>}，由 {@code HttpRpcUpstreamAdapter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rules; its type is {@code Supplier<CompiledGatewayRules>}, and {@code HttpRpcUpstreamAdapter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code HttpRpcUpstreamAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpRpcUpstreamAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Supplier<CompiledGatewayRules> rules;

    /**
     * 中文说明：保存 channels 对应的状态、依赖或配置值；字段类型为 {@code RpcProviderChannelCache}，由 {@code HttpRpcUpstreamAdapter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by channels; its type is {@code RpcProviderChannelCache}, and {@code HttpRpcUpstreamAdapter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code HttpRpcUpstreamAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpRpcUpstreamAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final RpcProviderChannelCache channels;

    /**
     * 中文说明：保存 object映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code HttpRpcUpstreamAdapter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by object mapper; its type is {@code ObjectMapper}, and {@code HttpRpcUpstreamAdapter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code HttpRpcUpstreamAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpRpcUpstreamAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：保存 bridges 对应的状态、依赖或配置值；字段类型为 {@code Map<String, HttpRpcDynamicMessageBridge>}，由 {@code HttpRpcUpstreamAdapter} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by bridges; its type is {@code Map<String, HttpRpcDynamicMessageBridge>}, and {@code HttpRpcUpstreamAdapter} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code HttpRpcUpstreamAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpRpcUpstreamAdapter}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, HttpRpcDynamicMessageBridge> bridges =
            new ConcurrentHashMap<>();

    /**
     * 中文说明：创建 {@code HttpRpcUpstreamAdapter} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code HttpRpcUpstreamAdapter} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param rules 参数 rules；parameter rules。
     * @param channels 参数 channels；parameter channels。
     * @param objectMapper 参数 object映射器；parameter object mapper。
     */
    public HttpRpcUpstreamAdapter(
            Supplier<CompiledGatewayRules> rules,
            RpcProviderChannelCache channels,
            ObjectMapper objectMapper) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.channels = Objects.requireNonNull(channels, "channels");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
    }

    /**
     * 中文说明：执行 invoke 操作；该方法是 {@code HttpRpcUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the invoke operation; this method is the invocation entry point on {@code HttpRpcUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpRpcUpstreamAdapter.invoke(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param match 参数 match；parameter match。
     * @param provider 参数 提供方；parameter provider。
     * @param request 参数 请求；parameter request。
     * @param body 参数 body；parameter body。
     * @param headers 参数 headers；parameter headers。
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 invoke 的处理结果；returns the result of the operation.
     */
    public Mono<GatewayOutboundHttpResponse> invoke(
            HttpRouteMatch match,
            ProviderInstance provider,
            NormalizedHttpRequest request,
            byte[] body,
            Map<String, List<String>> headers,
            Duration timeout) {
        Map<String, String> metadata = match.route().metadata();
        String fullMethod = required(metadata, "methodIdentity");
        String requestType = required(metadata, "requestSchema");
        String responseType = required(metadata, "responseSchema");
        HttpRpcDynamicMessageBridge bridge = bridge(
                required(metadata, "descriptorSha256")
        );
        byte[] protobufRequest = bridge.requestBytes(
                requestType,
                requestJson(match, request, body)
        );
        return unary(
                provider,
                fullMethod,
                protobufRequest,
                headers,
                timeout
        ).map(response -> {
            String json = bridge.responseJson(responseType, response);
            return new GatewayOutboundHttpResponse(
                    200,
                    Map.of(
                            "content-type",
                            List.of("application/json; charset=UTF-8")
                    ),
                    reactor.core.publisher.Flux.just(
                            DefaultDataBufferFactory.sharedInstance.wrap(
                                    json.getBytes(StandardCharsets.UTF_8)
                            )
                    )
            );
        }).onErrorMap(
                StatusRuntimeException.class,
                failure -> new HttpRpcUpstreamException(
                        failure.getStatus()
                )
        );
    }

    /**
     * 中文说明：执行 unary 操作；该方法是 {@code HttpRpcUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the unary operation; this method is the invocation entry point on {@code HttpRpcUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpRpcUpstreamAdapter.unary(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param provider 参数 提供方；parameter provider。
     * @param fullMethod 参数 full方法；parameter full method。
     * @param request 参数 请求；parameter request。
     * @param headers 参数 headers；parameter headers。
     * @param timeout 参数 超时；parameter timeout。
     * @return 返回 unary 的处理结果；returns the result of the operation.
     */
    private Mono<byte[]> unary(
            ProviderInstance provider,
            String fullMethod,
            byte[] request,
            Map<String, List<String>> headers,
            Duration timeout) {
        return Mono.create(sink -> {
            RpcProviderChannelCache.ChannelHandle channel =
                    channels.acquire(provider);
            ClientCall<byte[], byte[]> call = channel.channel().newCall(
                    RawByteMarshaller.INSTANCE.descriptor(fullMethod),
                    CallOptions.DEFAULT.withDeadlineAfter(
                            Math.max(1, timeout.toNanos()),
                            TimeUnit.NANOSECONDS
                    )
            );
            sink.onCancel(() -> {
                call.cancel("HTTP client cancelled", null);
                channel.close();
            });
            call.start(new ClientCall.Listener<>() {

                /**
                 * 中文说明：保存 响应 对应的状态、依赖或配置值；字段类型为 {@code byte[]}，由 {@code HttpRpcUpstreamAdapter} 在其生命周期内读取或更新。
                 * English summary: Holds the state, dependency, or configuration represented by response; its type is {@code byte[]}, and {@code HttpRpcUpstreamAdapter} reads or updates it during its lifecycle.
                 *
                 * 用法 / Usage: 该字段通过 {@code HttpRpcUpstreamAdapter} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpRpcUpstreamAdapter}; do not couple callers to its representation when the owning type exposes an API.
                 */
                private byte[] response;

                /**
                 * 中文说明：执行 on消息 操作；该方法是 {@code HttpRpcUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the on message operation; this method is the invocation entry point on {@code HttpRpcUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code HttpRpcUpstreamAdapter.onMessage(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @param message 参数 消息；parameter message。
                 */
                @Override
                public void onMessage(byte[] message) {
                    response = message;
                    call.request(1);
                }

                /**
                 * 中文说明：执行 onClose 操作；该方法是 {@code HttpRpcUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
                 * English summary: Executes the on close operation; this method is the invocation entry point on {@code HttpRpcUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
                 *
                 * 用法 / Usage: 调用方式 / Usage: {@code HttpRpcUpstreamAdapter.onClose(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
                 * @param status 参数 status；parameter status。
                 * @param trailers 参数 trailers；parameter trailers。
                 */
                @Override
                public void onClose(Status status, Metadata trailers) {
                    try {
                        if (status.isOk() && response != null) {
                            sink.success(response);
                        } else {
                            sink.error(status.asRuntimeException(trailers));
                        }
                    } finally {
                        channel.close();
                    }
                }
            }, rpcHeaders(headers));
            call.request(1);
            call.sendMessage(request);
            call.halfClose();
        });
    }

    /**
     * 中文说明：执行 rpcHeaders 操作；该方法是 {@code HttpRpcUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rpc headers operation; this method is the invocation entry point on {@code HttpRpcUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpRpcUpstreamAdapter.rpcHeaders(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @return 返回 rpcHeaders 的处理结果；returns the result of the operation.
     */
    private Metadata rpcHeaders(Map<String, List<String>> headers) {
        Metadata metadata = new Metadata();
        copy(headers, metadata, "traceparent", RpcMetadataKeys.TRACEPARENT);
        copy(headers, metadata, "tracestate", RpcMetadataKeys.TRACESTATE);
        copy(headers, metadata, "x-egon-request-id", RpcMetadataKeys.REQUEST_ID);
        copy(headers, metadata, "authorization", AUTHORIZATION);
        return metadata;
    }

    /**
     * 中文说明：执行 copy 操作；该方法是 {@code HttpRpcUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the copy operation; this method is the invocation entry point on {@code HttpRpcUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpRpcUpstreamAdapter.copy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param headers 参数 headers；parameter headers。
     * @param target 参数 target；parameter target。
     * @param source 参数 source；parameter source。
     * @param key 参数 键；parameter key。
     */
    private void copy(
            Map<String, List<String>> headers,
            Metadata target,
            String source,
            Metadata.Key<String> key) {
        headers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(source))
                .map(Map.Entry::getValue)
                .filter(values -> values != null && !values.isEmpty())
                .map(List::getFirst)
                .findFirst()
                .ifPresent(value -> target.put(key, value));
    }

    /**
     * 中文说明：执行 请求Json 操作；该方法是 {@code HttpRpcUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the request json operation; this method is the invocation entry point on {@code HttpRpcUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpRpcUpstreamAdapter.requestJson(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param match 参数 match；parameter match。
     * @param request 参数 请求；parameter request。
     * @param body 参数 body；parameter body。
     * @return 返回 请求Json 的处理结果；returns the result of the operation.
     */
    private String requestJson(
            HttpRouteMatch match,
            NormalizedHttpRequest request,
            byte[] body) {
        try {
            Map<String, Object> values = body.length == 0
                    ? new LinkedHashMap<>()
                    : objectMapper.readValue(
                    body,
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    }
            );
            match.pathVariables().forEach(values::putIfAbsent);
            query(request.rawQuery()).forEach(values::putIfAbsent);
            return objectMapper.writeValueAsString(values);
        } catch (Exception failure) {
            throw new IllegalArgumentException(
                    "HTTP request body must be a JSON object",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 query 操作；该方法是 {@code HttpRpcUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the query operation; this method is the invocation entry point on {@code HttpRpcUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpRpcUpstreamAdapter.query(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param rawQuery 参数 rawQuery；parameter raw query。
     * @return 返回 query 的处理结果；returns the result of the operation.
     */
    private Map<String, Object> query(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (String parameter : rawQuery.split("&")) {
            int separator = parameter.indexOf('=');
            String name = decode(separator < 0
                    ? parameter
                    : parameter.substring(0, separator));
            String value = decode(separator < 0
                    ? ""
                    : parameter.substring(separator + 1));
            values.putIfAbsent(name, value);
        }
        return Map.copyOf(values);
    }

    /**
     * 中文说明：执行 decode 操作；该方法是 {@code HttpRpcUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the decode operation; this method is the invocation entry point on {@code HttpRpcUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpRpcUpstreamAdapter.decode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 decode 的处理结果；returns the result of the operation.
     */
    private String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    /**
     * 中文说明：执行 bridge 操作；该方法是 {@code HttpRpcUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the bridge operation; this method is the invocation entry point on {@code HttpRpcUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpRpcUpstreamAdapter.bridge(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param descriptorSha 参数 descriptorSha；parameter descriptor sha。
     * @return 返回 bridge 的处理结果；returns the result of the operation.
     */
    private HttpRpcDynamicMessageBridge bridge(String descriptorSha) {
        return bridges.computeIfAbsent(descriptorSha, ignored -> {
            CompiledGatewayRules active = rules.get();
            if (active == null) {
                throw new IllegalStateException(
                        "GATEWAY_RULES_NOT_READY"
                );
            }
            GatewayRpcDescriptor descriptor = active.snapshot()
                    .content()
                    .rpcDescriptors()
                    .stream()
                    .filter(value -> value.sha256().equals(descriptorSha))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "GATEWAY_RPC_DESCRIPTOR_NOT_FOUND"
                    ));
            return new HttpRpcDynamicMessageBridge(
                    new ProtobufDescriptorRegistry(
                            Base64.getDecoder().decode(
                                    descriptor.base64DescriptorSet()
                            )
                    )
            );
        });
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code HttpRpcUpstreamAdapter} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code HttpRpcUpstreamAdapter} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpRpcUpstreamAdapter.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param values 参数 values；parameter values。
     * @param key 参数 键；parameter key。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "HTTP to RPC route is missing " + key
            );
        }
        return value;
    }

    /**
     * 中文说明：{@code HttpRpcUpstreamException} 是异常类型，位于当前 Gateway 模块的相关包中，负责HttpRpcUpstreamException相关的职责与边界。
     * English summary: {@code HttpRpcUpstreamException} is a http rpc upstream exception exception in the current Gateway module; it owns the http rpc upstream exception-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static final class HttpRpcUpstreamException
            extends RuntimeException {

        /**
         * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code Status}，由 {@code HttpRpcUpstreamAdapter.HttpRpcUpstreamException} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code Status}, and {@code HttpRpcUpstreamAdapter.HttpRpcUpstreamException} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code HttpRpcUpstreamAdapter.HttpRpcUpstreamException} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpRpcUpstreamAdapter.HttpRpcUpstreamException}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Status status;

        /**
         * 中文说明：创建 {@code HttpRpcUpstreamAdapter.HttpRpcUpstreamException} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code HttpRpcUpstreamAdapter.HttpRpcUpstreamException} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param status 参数 status；parameter status。
         */
        private HttpRpcUpstreamException(Status status) {
            super(status.toString());
            this.status = status;
        }

        /**
         * 中文说明：执行 status 操作；该方法是 {@code HttpRpcUpstreamAdapter.HttpRpcUpstreamException} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the status operation; this method is the invocation entry point on {@code HttpRpcUpstreamAdapter.HttpRpcUpstreamException} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code HttpRpcUpstreamAdapter.HttpRpcUpstreamException.status(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 status 的处理结果；returns the result of the operation.
         */
        public Status status() {
            return status;
        }
    }
}
