package top.egon.cola.component.gateway.contract.error;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * HTTP 和 RPC 共同使用的安全错误契约。
 *
 * <p>网关将内部异常转换为该类型后再交给协议适配层，避免把实现细节直接暴露给调用方。
 */
public record GatewayError(
        String code,
        GatewayErrorCategory category,
        String message,
        String traceId,
        boolean retryable,
        Map<String, String> details
) {

    public GatewayError {
        code = required(code, "code");
        category = Objects.requireNonNull(category, "category");
        message = required(message, "message");
        traceId = required(traceId, "traceId");
        details = immutableDetails(details);
    }

    public static GatewayError internal(String traceId) {
        return new GatewayError(
                "GATEWAY_INTERNAL_ERROR",
                GatewayErrorCategory.INTERNAL_ERROR,
                "Gateway request failed",
                traceId,
                false,
                Map.of()
        );
    }

    private static Map<String, String> immutableDetails(
            Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        details.forEach((key, value) -> {
            if (key == null || value == null) {
                throw new IllegalArgumentException(
                        "error detail keys and values must not be null"
                );
            }
            copy.put(key, value);
        });
        return Map.copyOf(copy);
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
