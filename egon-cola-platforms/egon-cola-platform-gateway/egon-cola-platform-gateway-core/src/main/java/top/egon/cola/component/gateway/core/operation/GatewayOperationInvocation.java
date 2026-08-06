package top.egon.cola.component.gateway.core.operation;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Trusted operation identity plus caller data for an in-process invocation.
 */
public record GatewayOperationInvocation(
        GatewayOperationCall call,
        String originalBearerToken,
        String callerId,
        String clientIp,
        Map<String, String> traceHeaders
) {

    private static final java.util.Set<String> ALLOWED_TRACE_HEADERS =
            java.util.Set.of(
                    "traceparent",
                    "tracestate",
                    "x-egon-request-id"
            );

    public GatewayOperationInvocation {
        call = java.util.Objects.requireNonNull(call, "call");
        originalBearerToken = optional(originalBearerToken);
        callerId = optional(callerId);
        clientIp = optional(clientIp);
        traceHeaders = allowedTraceHeaders(traceHeaders);
    }

    public String operationId() {
        return call.operationId();
    }

    private static Map<String, String> allowedTraceHeaders(
            Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String normalized = required(key, "trace header")
                    .toLowerCase(Locale.ROOT);
            if (ALLOWED_TRACE_HEADERS.contains(normalized)
                    && value != null
                    && !value.isBlank()) {
                copy.put(normalized, value.trim());
            }
        });
        return Map.copyOf(copy);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
