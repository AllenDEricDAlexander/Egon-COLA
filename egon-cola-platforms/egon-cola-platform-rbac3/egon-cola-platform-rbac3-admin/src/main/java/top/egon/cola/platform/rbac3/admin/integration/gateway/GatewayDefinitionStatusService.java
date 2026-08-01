package top.egon.cola.platform.rbac3.admin.integration.gateway;

import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportingState;
import top.egon.cola.platform.rbac3.admin.integration.runtime.GatewayDdcRuntimeStatusService;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Exposes the definition receipt without conflating it with provider or release state.
 */
public final class GatewayDefinitionStatusService {

    private final Supplier<GatewayReportingState.Snapshot> snapshot;
    private final GatewayDdcRuntimeStatusService.ServiceIdentity identity;

    public GatewayDefinitionStatusService(
            GatewayReportingState state,
            GatewayReportingProperties properties) {
        this(state::snapshot, new GatewayDdcRuntimeStatusService.ServiceIdentity(
                properties.getEnv(), properties.getNamespace(), "HTTP_PROVIDER", "http",
                properties.getApplicationCode(), "default", properties.getArtifactVersion()));
    }

    public GatewayDefinitionStatusService(
            Supplier<GatewayReportingState.Snapshot> snapshot,
            GatewayDdcRuntimeStatusService.ServiceIdentity identity) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.identity = Objects.requireNonNull(identity, "identity");
    }

    public DefinitionStatus status() {
        GatewayReportingState.Snapshot current = snapshot.get();
        GatewayInterfaceDefinitionReportResult result = current.result();
        if (result == null || !"SUCCESS".equals(current.status())) {
            return new DefinitionStatus(
                    "UNKNOWN", null, List.of(safe(current.lastError())), identity);
        }
        return new DefinitionStatus(
                result.status().name(), result.definitionSetId(),
                result.warnings().stream()
                        .map(GatewayInterfaceDefinitionReportResult.Warning::code)
                        .toList(), identity);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "DEFINITION_NOT_ACKNOWLEDGED"
                : "DEFINITION_REPORT_FAILED";
    }

    public record DefinitionStatus(
            String status,
            String definitionSetId,
            List<String> warnings,
            GatewayDdcRuntimeStatusService.ServiceIdentity identity) {

        public DefinitionStatus {
            warnings = List.copyOf(warnings);
        }

        public boolean accepted() {
            return "ACCEPTED".equals(status)
                    || "ACCEPTED_WITH_WARNINGS".equals(status);
        }
    }
}
