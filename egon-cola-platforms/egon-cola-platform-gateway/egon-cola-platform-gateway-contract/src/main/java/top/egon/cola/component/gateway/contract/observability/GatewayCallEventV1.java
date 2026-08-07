package top.egon.cola.component.gateway.contract.observability;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 网关调用观测事件 v1。
 *
 * <p>事件把链路、请求、路由、治理决策、最终结果和每次 provider 尝试统一封装，供审计、指标和
 * 故障分析使用；它描述事实，不参与调用流程控制。
 */
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

    /**
     * 调用关联的链路标识。
     */
    public record Trace(
            String traceId,
            String engineSpanId,
            boolean sampled
    ) {
    }

    /**
     * 进入网关的请求摘要，不包含请求体等敏感业务数据。
     */
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

    /**
     * 本次调用实际使用的发布、操作、路由和 provider 身份。
     */
    public record Routing(
            String env,
            String namespace,
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

    /**
     * 限流、熔断、安全和重试等治理阶段的最终决策。
     */
    public record Governance(
            String terminalStage,
            String rateLimitDecision,
            String circuitDecision,
            String securityDecision,
            int retryCount
    ) {
    }

    /**
     * 调用最终结果及传输层状态、响应大小和耗时。
     */
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

    /**
     * 一次具体 provider 尝试的执行记录，用于还原重试和选址过程。
     */
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
