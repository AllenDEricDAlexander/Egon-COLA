package top.egon.cola.component.gateway.engine.rpc;

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
import top.egon.cola.component.gateway.engine.http.GatewayOutboundHttpResponse;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;
import top.egon.cola.component.rpc.context.RpcMetadataKeys;

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

public final class HttpRpcUpstreamAdapter {

    private static final Metadata.Key<String> AUTHORIZATION = Metadata.Key.of(
            "authorization",
            Metadata.ASCII_STRING_MARSHALLER
    );

    private final Supplier<CompiledGatewayRules> rules;

    private final RpcProviderChannelCache channels;

    private final ObjectMapper objectMapper;

    private final Map<String, HttpRpcDynamicMessageBridge> bridges =
            new ConcurrentHashMap<>();

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

                private byte[] response;

                @Override
                public void onMessage(byte[] message) {
                    response = message;
                    call.request(1);
                }

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

    private Metadata rpcHeaders(Map<String, List<String>> headers) {
        Metadata metadata = new Metadata();
        copy(headers, metadata, "traceparent", RpcMetadataKeys.TRACEPARENT);
        copy(headers, metadata, "tracestate", RpcMetadataKeys.TRACESTATE);
        copy(headers, metadata, "x-egon-request-id", RpcMetadataKeys.REQUEST_ID);
        copy(headers, metadata, "authorization", AUTHORIZATION);
        return metadata;
    }

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

    private String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

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

    private String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "HTTP to RPC route is missing " + key
            );
        }
        return value;
    }

    public static final class HttpRpcUpstreamException
            extends RuntimeException {

        private final Status status;

        private HttpRpcUpstreamException(Status status) {
            super(status.toString());
            this.status = status;
        }

        public Status status() {
            return status;
        }
    }
}
