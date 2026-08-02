package top.egon.cola.component.gateway.mcp.remote;

import top.egon.cola.component.gateway.contract.mcp.protocol.McpErrorCode;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.core.mcp.remote.RemoteMcpClient;
import top.egon.cola.component.gateway.mcp.protocol.McpProtocolException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Adapter between the caller dialect and the fixed remote Provider dialect.
 */
public final class McpDialectTranslator {

    private static final Set<String> TRACE_HEADERS = Set.of(
            "traceparent",
            "tracestate",
            "x-egon-request-id"
    );

    public OutboundCall outbound(
            McpProtocolDialect inboundDialect,
            McpProtocolDialect remoteDialect,
            String method,
            Map<String, Object> params,
            Map<String, Object> meta,
            Map<String, String> traceHeaders) {
        if (inboundDialect == null || remoteDialect == null) {
            throw new IllegalArgumentException("MCP dialect is required");
        }
        String translatedMethod = translateMethod(
                inboundDialect,
                remoteDialect,
                required(method, "method")
        );
        LinkedHashMap<String, Object> translatedParams = new LinkedHashMap<>();
        if (params != null) {
            params.forEach((name, value) -> {
                if (!sensitive(name)) {
                    translatedParams.put(name, value);
                }
            });
        }
        LinkedHashMap<String, Object> translatedMeta = new LinkedHashMap<>();
        if (meta != null) {
            meta.forEach((name, value) -> {
                if (!sensitive(name)) {
                    translatedMeta.put(name, value);
                }
            });
        }
        translatedMeta.put("protocolVersion", remoteDialect.protocolVersion());
        translatedMeta.put("gatewayDialect", inboundDialect.protocolVersion());
        copyTraceMetadata(traceHeaders, translatedMeta);

        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        headers.put("mcp-protocol-version", remoteDialect.protocolVersion());
        copyTraceHeaders(traceHeaders, headers);
        if (remoteDialect == McpProtocolDialect.RC_2026_07_28) {
            headers.put("mcp-method", translatedMethod);
            Object name = translatedParams.get("name");
            if (name != null) {
                headers.put("mcp-name", String.valueOf(name));
            }
        }
        if (remoteDialect == McpProtocolDialect.LEGACY_2024_SSE) {
            headers.remove("mcp-protocol-version");
        }
        return new OutboundCall(
                translatedMethod,
                Map.copyOf(translatedParams),
                Map.copyOf(translatedMeta),
                Map.copyOf(headers)
        );
    }

    public Map<String, Object> result(
            RemoteMcpClient.ExchangeResponse response) {
        if (response == null) {
            throw unavailable("remote MCP response was empty", null);
        }
        if (response.error() == null) {
            return response.result();
        }
        RemoteMcpClient.RemoteError error = response.error();
        McpErrorCode code = switch (error.code()) {
            case -32600 -> McpErrorCode.MCP_INVALID_REQUEST;
            case -32601 -> McpErrorCode.MCP_METHOD_NOT_FOUND;
            case -32602 -> McpErrorCode.MCP_INVALID_PARAMS;
            case -32023 -> McpErrorCode.MCP_UNAUTHENTICATED;
            case -32024 -> McpErrorCode.MCP_FORBIDDEN;
            case -32025 -> McpErrorCode.MCP_APPROVAL_REQUIRED;
            case -32028 -> McpErrorCode.MCP_TASK_NOT_FOUND;
            case -32029 -> McpErrorCode.MCP_RESOURCE_REJECTED;
            default -> McpErrorCode.MCP_REMOTE_UNAVAILABLE;
        };
        throw new McpProtocolException(code, safeRemoteMessage(error.message()));
    }

    private String translateMethod(
            McpProtocolDialect inbound,
            McpProtocolDialect outbound,
            String method) {
        if (inbound == outbound) {
            return method;
        }
        if (outbound == McpProtocolDialect.LEGACY_2024_SSE
                && "server/discover".equals(method)) {
            return "initialize";
        }
        if (inbound == McpProtocolDialect.LEGACY_2024_SSE
                && "initialize".equals(method)
                && outbound == McpProtocolDialect.RC_2026_07_28) {
            return "server/discover";
        }
        return method;
    }

    private void copyTraceMetadata(
            Map<String, String> source,
            Map<String, Object> target) {
        if (source == null) {
            return;
        }
        source.forEach((name, value) -> {
            String normalized = name.toLowerCase(Locale.ROOT);
            if (TRACE_HEADERS.contains(normalized)
                    && value != null && !value.isBlank()) {
                target.put(normalized, value.trim());
            }
        });
    }

    private void copyTraceHeaders(
            Map<String, String> source,
            Map<String, String> target) {
        if (source == null) {
            return;
        }
        source.forEach((name, value) -> {
            String normalized = name.toLowerCase(Locale.ROOT);
            if (TRACE_HEADERS.contains(normalized)
                    && value != null && !value.isBlank()) {
                target.put(normalized, value.trim());
            }
        });
    }

    private boolean sensitive(String name) {
        String normalized = name.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "");
        return normalized.contains("authorization")
                || normalized.contains("bearer")
                || normalized.contains("password")
                || normalized.contains("secret")
                || normalized.equals("token")
                || normalized.equals("approvaltoken");
    }

    private String safeRemoteMessage(String message) {
        if (message == null || message.isBlank()) {
            return "remote MCP request failed";
        }
        String result = message.replaceAll(
                "(?i)bearer\\s+[a-z0-9._~+/=-]+",
                "Bearer [redacted]"
        );
        return result.length() > 512 ? result.substring(0, 512) : result;
    }

    private McpProtocolException unavailable(
            String message,
            Throwable cause) {
        McpProtocolException exception = new McpProtocolException(
                McpErrorCode.MCP_REMOTE_UNAVAILABLE,
                message
        );
        if (cause != null) {
            exception.initCause(cause);
        }
        return exception;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MCP " + field + " is required");
        }
        return value.trim();
    }

    public record OutboundCall(
            String method,
            Map<String, Object> params,
            Map<String, Object> meta,
            Map<String, String> headers
    ) {

        public OutboundCall {
            method = requiredValue(method);
            params = params == null ? Map.of() : Map.copyOf(params);
            meta = meta == null ? Map.of() : Map.copyOf(meta);
            headers = headers == null ? Map.of() : Map.copyOf(headers);
        }

        private static String requiredValue(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        "MCP outbound method is required"
                );
            }
            return value.trim();
        }
    }
}
