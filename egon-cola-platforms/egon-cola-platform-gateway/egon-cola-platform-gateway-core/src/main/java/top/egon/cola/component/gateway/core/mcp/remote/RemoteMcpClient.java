package top.egon.cola.component.gateway.core.mcp.remote;

import org.reactivestreams.Publisher;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeRemoteProvider;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Transport port for one remote MCP Provider.
 *
 * <p>The request deliberately has no inbound bearer field. Authentication is
 * resolved by {@link RemoteAuthProvider} before the transport is invoked.</p>
 */
@FunctionalInterface
public interface RemoteMcpClient extends AutoCloseable {

    Publisher<ExchangeResponse> exchange(ExchangeRequest request);

    @Override
    default void close() {
    }

    @FunctionalInterface
    interface Factory {

        RemoteMcpClient create(McpRuntimeRemoteProvider provider);
    }

    record ExchangeRequest(
            McpRuntimeRemoteProvider provider,
            Object id,
            String method,
            Map<String, Object> params,
            Map<String, Object> meta,
            Map<String, String> headers,
            String tlsProfileReference,
            Duration timeout
    ) {

        public ExchangeRequest {
            provider = Objects.requireNonNull(provider, "provider");
            if (id == null) {
                throw new IllegalArgumentException("remote MCP id is required");
            }
            method = required(method, "method");
            params = params == null ? Map.of() : Map.copyOf(params);
            meta = meta == null ? Map.of() : Map.copyOf(meta);
            headers = headers(headers);
            tlsProfileReference = optional(tlsProfileReference);
            timeout = Objects.requireNonNull(timeout, "timeout");
            if (timeout.isZero() || timeout.isNegative()) {
                throw new IllegalArgumentException(
                        "remote MCP timeout must be positive"
                );
            }
        }

        private static Map<String, String> headers(
                Map<String, String> source) {
            if (source == null || source.isEmpty()) {
                return Map.of();
            }
            LinkedHashMap<String, String> result = new LinkedHashMap<>();
            source.forEach((name, value) -> {
                String normalized = required(name, "header name")
                        .toLowerCase(Locale.ROOT);
                String checked = required(value, "header value");
                if (normalized.contains("\r") || normalized.contains("\n")
                        || checked.contains("\r")
                        || checked.contains("\n")) {
                    throw new IllegalArgumentException(
                            "remote MCP header contains a line break"
                    );
                }
                if (result.putIfAbsent(normalized, checked) != null) {
                    throw new IllegalArgumentException(
                            "duplicate remote MCP header: " + normalized
                    );
                }
            });
            return Map.copyOf(result);
        }
    }

    record ExchangeResponse(
            Map<String, Object> result,
            RemoteError error,
            Map<String, String> headers
    ) {

        public ExchangeResponse {
            if ((result == null) == (error == null)) {
                throw new IllegalArgumentException(
                        "remote MCP response requires one result or error"
                );
            }
            result = result == null ? null : Map.copyOf(result);
            headers = headers == null ? Map.of() : Map.copyOf(headers);
        }

        public static ExchangeResponse success(
                Map<String, Object> result,
                Map<String, String> headers) {
            return new ExchangeResponse(
                    Objects.requireNonNull(result, "result"),
                    null,
                    headers
            );
        }

        public static ExchangeResponse failure(
                int code,
                String message,
                Map<String, Object> data,
                Map<String, String> headers) {
            return new ExchangeResponse(
                    null,
                    new RemoteError(code, message, data),
                    headers
            );
        }
    }

    record RemoteError(int code, String message, Map<String, Object> data) {

        public RemoteError {
            message = required(message, "error message");
            data = data == null ? Map.of() : Map.copyOf(data);
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "remote MCP " + field + " is required"
            );
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
