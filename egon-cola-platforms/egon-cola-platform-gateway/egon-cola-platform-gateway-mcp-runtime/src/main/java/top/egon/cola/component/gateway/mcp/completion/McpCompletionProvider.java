package top.egon.cola.component.gateway.mcp.completion;

import org.reactivestreams.Publisher;
import top.egon.cola.component.gateway.mcp.prompt.McpPromptDriver;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Strategy for bounded, deterministic completion sources.
 */
public interface McpCompletionProvider {

    String sourceType();

    Publisher<Result> complete(Request request);

    static boolean sensitiveArgumentName(String value) {
        String normalized = value.toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "");
        return normalized.contains("secret")
                || normalized.contains("password")
                || normalized.contains("token")
                || normalized.contains("credential")
                || normalized.contains("privatekey")
                || normalized.contains("apikey");
    }

    static boolean sensitiveValue(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("secret://")
                || normalized.contains("vault://")
                || normalized.contains("password=")
                || normalized.contains("-----begin private key");
    }

    record Request(
            String serverCode,
            String referenceType,
            String referenceName,
            String argumentName,
            String valuePrefix,
            String operationId,
            Map<String, Object> attributes
    ) {

        public Request {
            serverCode = required(serverCode, "serverCode");
            referenceType = required(referenceType, "referenceType");
            referenceName = required(referenceName, "referenceName");
            argumentName = required(argumentName, "argumentName");
            valuePrefix = valuePrefix == null ? "" : valuePrefix;
            if (valuePrefix.length() > 256) {
                throw McpPromptDriver.invalid(
                        "MCP completion prefix is too large"
                );
            }
            operationId = operationId == null || operationId.isBlank()
                    ? null
                    : operationId.trim();
            attributes = attributes == null ? Map.of() : Map.copyOf(
                    attributes
            );
        }
    }

    record Result(List<String> values, int total, boolean hasMore) {

        public Result {
            values = List.copyOf(Objects.requireNonNull(values, "values"));
            if (values.size() > 100 || total < values.size()) {
                throw new IllegalArgumentException(
                        "MCP completion result is invalid"
                );
            }
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw McpPromptDriver.invalid(
                    "MCP completion " + field + " is required"
            );
        }
        return value.trim();
    }
}
