package top.egon.cola.component.gateway.core.operation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record GatewayInvocationResult(
        int statusCode,
        Map<String, List<String>> headers,
        byte[] body
) {

    public GatewayInvocationResult {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("invalid statusCode");
        }
        headers = Objects.requireNonNull(headers, "headers")
                .entrySet()
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())
                ));
        body = Objects.requireNonNull(body, "body").clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }
}
