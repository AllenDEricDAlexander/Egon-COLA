package top.egon.cola.component.gateway.contract.observability;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record GatewayCallEventV1(
        String eventSchemaVersion,
        String eventId,
        long occurredAt,
        long completedAt,
        Trace trace,
        Request request,
        Routing routing,
        Governance governance,
        Result result,
        List<Attempt> attempts
) {

    public GatewayCallEventV1 {
        if (!"v1".equals(eventSchemaVersion)) {
            throw new IllegalArgumentException(
                    "eventSchemaVersion must be v1"
            );
        }
        required(eventId, "eventId");
        if (occurredAt < 0 || completedAt < occurredAt) {
            throw new IllegalArgumentException("invalid event timestamps");
        }
        Objects.requireNonNull(trace, "trace");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(routing, "routing");
        Objects.requireNonNull(governance, "governance");
        Objects.requireNonNull(result, "result");
        attempts = List.copyOf(Objects.requireNonNull(
                attempts,
                "attempts"
        ));
    }

    public record Trace(
            String traceId,
            String engineSpanId,
            boolean sampled
    ) {
    }

    public record Request(
            String requestId,
            String protocol,
            String accessZone,
            String normalizedMethod,
            String normalizedRouteTemplate,
            long requestBytes,
            String clientNetworkClass
    ) {
    }

    public record Routing(
            String gatewayGroupId,
            String engineNodeId,
            String releaseId,
            String operationId,
            String routeId,
            Map<String, Object> providerServiceIdentity
    ) {

        public Routing {
            providerServiceIdentity = providerServiceIdentity == null
                    ? Map.of()
                    : Map.copyOf(providerServiceIdentity);
        }
    }

    public record Governance(
            String terminalStage,
            String rateLimitDecision,
            String circuitDecision,
            String securityDecision,
            int retryCount
    ) {
    }

    public record Result(
            String category,
            String gatewayErrorCode,
            Integer httpStatus,
            String grpcStatus,
            long responseBytes,
            long durationMs
    ) {

        public Result {
            if (responseBytes < 0 || durationMs < 0) {
                throw new IllegalArgumentException(
                        "result sizes and duration must be non-negative"
                );
            }
        }
    }

    public record Attempt(
            int attempt,
            String spanId,
            String providerInstanceId,
            long startedAt,
            long durationMs,
            String resultCategory,
            String retryReason
    ) {

        public Attempt {
            if (attempt < 1 || startedAt < 0 || durationMs < 0) {
                throw new IllegalArgumentException(
                        "invalid provider attempt"
                );
            }
        }
    }

    private static void required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
