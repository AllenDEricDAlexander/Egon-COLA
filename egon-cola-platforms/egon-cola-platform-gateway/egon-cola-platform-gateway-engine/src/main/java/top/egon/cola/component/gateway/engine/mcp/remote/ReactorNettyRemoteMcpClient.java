package top.egon.cola.component.gateway.engine.mcp.remote;

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
import top.egon.cola.component.gateway.mcp.remote.McpRemoteEndpointValidator;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bounded Streamable HTTP client for Stable, RC and fixed legacy endpoints.
 */
public final class ReactorNettyRemoteMcpClient
        implements RemoteMcpClient {

    private static final int MAX_RESPONSE_BYTES = 16 * 1024 * 1024;

    private static final TypeReference<Map<String, Object>> OBJECT_MAP =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;

    private final TlsClientProvider clients;

    private final AtomicReference<String> stableSessionId =
            new AtomicReference<>();

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

    public ReactorNettyRemoteMcpClient(
            ObjectMapper objectMapper,
            TlsClientProvider clients) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        ).copy();
        this.clients = Objects.requireNonNull(clients, "clients");
    }

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

    private Map<String, String> responseHeaders(
            io.netty.handler.codec.http.HttpHeaders source) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        source.forEach(entry -> result.putIfAbsent(
                entry.getKey().toLowerCase(Locale.ROOT),
                entry.getValue()
        ));
        return Map.copyOf(result);
    }

    private int timeoutMillis(Duration timeout) {
        return Math.toIntExact(Math.min(
                Integer.MAX_VALUE,
                Math.max(1L, timeout.toMillis())
        ));
    }

    @FunctionalInterface
    public interface TlsClientProvider {

        HttpClient client(String tlsProfileReference);
    }

    private static final class BoundedResponseBody {

        private final ByteArrayOutputStream output =
                new ByteArrayOutputStream();

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

        private byte[] bytes() {
            return output.toByteArray();
        }
    }
}
