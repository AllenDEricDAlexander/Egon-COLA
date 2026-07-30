package top.egon.cola.component.gateway.engine.traffic;

import top.egon.cola.component.gateway.core.http.GatewayRequestRejectedException;
import top.egon.cola.component.gateway.core.http.NormalizedHttpRequest;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class GatewayRequestResourceGuard {

    private final GatewayResourceLimits limits;

    public GatewayRequestResourceGuard(GatewayResourceLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public void validate(NormalizedHttpRequest request) {
        Objects.requireNonNull(request, "request");
        int queryCount = request.rawQuery().isEmpty()
                ? 0
                : request.rawQuery().split("&", -1).length;
        int pathSegments = request.normalizedPath().equals("/")
                ? 0
                : request.normalizedPath().substring(1).split("/", -1).length;
        int metadataBytes = request.headers().entrySet().stream()
                .mapToInt(entry -> entry.getKey().getBytes(
                        StandardCharsets.UTF_8
                ).length + entry.getValue().stream().mapToInt(
                        value -> value.getBytes(StandardCharsets.UTF_8).length
                ).sum())
                .sum();
        if (queryCount > limits.maximumQueryParameters()
                || pathSegments > limits.maximumPathSegments()
                || metadataBytes > limits.maximumMetadataBytes()) {
            throw new GatewayRequestRejectedException(
                    "GATEWAY_REQUEST_LIMIT_EXCEEDED",
                    413,
                    "request resource limit exceeded"
            );
        }
    }
}
