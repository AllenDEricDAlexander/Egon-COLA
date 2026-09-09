package top.egon.cola.component.yuheng.admin.openapi.validation;

import java.util.Objects;

/**
 * Classified result returned by one OpenAPI validation rule.
 *
 * @param valid whether the document passed the rule
 * @param code stable failure code when invalid
 * @param message bounded safe operator message when invalid
 */
public record GatewayOpenApiValidationResult(
        boolean valid,
        String code,
        String message
) {

    public GatewayOpenApiValidationResult {
        if (valid) {
            if (code != null || message != null) {
                throw new IllegalArgumentException(
                        "a valid result cannot contain an error"
                );
            }
        } else {
            code = required(code, "code", 128);
            message = required(message, "message", 1024);
        }
    }

    public static GatewayOpenApiValidationResult passed() {
        return new GatewayOpenApiValidationResult(true, null, null);
    }

    public static GatewayOpenApiValidationResult invalid(
            String code,
            String message) {
        return new GatewayOpenApiValidationResult(false, code, message);
    }

    private static String required(String value, String field, int max) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty() || normalized.length() > max) {
            throw new IllegalArgumentException(
                    field + " is blank or exceeds " + max + " characters"
            );
        }
        return normalized;
    }
}
