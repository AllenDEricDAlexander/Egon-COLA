package top.egon.cola.component.gateway.core.context;

import top.egon.cola.component.gateway.contract.protocol.AccessZone;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable typed snapshot of request execution state.
 */
public final class GatewayContext {

    private final String requestId;

    private final String traceId;

    private final String traceparent;

    private final String tracestate;

    private final AccessZone accessZone;

    private final String gatewayGroupId;

    private final String engineNodeId;

    private final String operationId;

    private final String routeId;

    private final String releaseId;

    private final GatewayPrincipal principal;

    private final GatewayProviderSelection providerSelection;

    private final Instant deadline;

    private final Instant startedAt;

    private final GatewayStage stage;

    private final List<GatewayGovernanceDecision> governanceDecisions;

    private final List<GatewayDiagnostic> diagnostics;

    public GatewayContext(
            String requestId,
            String traceId,
            String traceparent,
            String tracestate,
            AccessZone accessZone,
            String gatewayGroupId,
            String engineNodeId,
            String operationId,
            String routeId,
            String releaseId,
            GatewayPrincipal principal,
            GatewayProviderSelection providerSelection,
            Instant deadline,
            Instant startedAt,
            GatewayStage stage,
            List<GatewayGovernanceDecision> governanceDecisions,
            List<GatewayDiagnostic> diagnostics) {
        this.requestId = required(requestId, "requestId");
        this.traceId = required(traceId, "traceId");
        this.traceparent = optional(traceparent);
        this.tracestate = optional(tracestate);
        this.accessZone = Objects.requireNonNull(accessZone, "accessZone");
        this.gatewayGroupId = required(gatewayGroupId, "gatewayGroupId");
        this.engineNodeId = required(engineNodeId, "engineNodeId");
        this.operationId = optional(operationId);
        this.routeId = optional(routeId);
        this.releaseId = optional(releaseId);
        this.principal = principal;
        this.providerSelection = providerSelection;
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.deadline = Objects.requireNonNull(deadline, "deadline");
        if (deadline.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                    "deadline must not be before startedAt"
            );
        }
        this.stage = Objects.requireNonNull(stage, "stage");
        this.governanceDecisions = List.copyOf(
                Objects.requireNonNull(
                        governanceDecisions,
                        "governanceDecisions"
                )
        );
        this.diagnostics = List.copyOf(
                Objects.requireNonNull(diagnostics, "diagnostics")
        );
    }

    public String requestId() {
        return requestId;
    }

    public String traceId() {
        return traceId;
    }

    public Optional<String> traceparent() {
        return Optional.ofNullable(traceparent);
    }

    public Optional<String> tracestate() {
        return Optional.ofNullable(tracestate);
    }

    public AccessZone accessZone() {
        return accessZone;
    }

    public String gatewayGroupId() {
        return gatewayGroupId;
    }

    public String engineNodeId() {
        return engineNodeId;
    }

    public Optional<String> operationId() {
        return Optional.ofNullable(operationId);
    }

    public Optional<String> routeId() {
        return Optional.ofNullable(routeId);
    }

    public Optional<String> releaseId() {
        return Optional.ofNullable(releaseId);
    }

    public Optional<GatewayPrincipal> principal() {
        return Optional.ofNullable(principal);
    }

    public Optional<GatewayProviderSelection> providerSelection() {
        return Optional.ofNullable(providerSelection);
    }

    public Instant deadline() {
        return deadline;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public GatewayStage stage() {
        return stage;
    }

    public List<GatewayGovernanceDecision> governanceDecisions() {
        return governanceDecisions;
    }

    public List<GatewayDiagnostic> diagnostics() {
        return diagnostics;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
