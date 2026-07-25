package top.egon.cola.component.gateway.contract.reporting;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record GatewayInterfaceDefinitionReportResult(
        String reportId,
        String definitionSetId,
        Status status,
        String applicationId,
        Counts counts,
        List<OperationRef> operationRefs,
        List<Warning> warnings,
        Instant receivedAt
) {

    public GatewayInterfaceDefinitionReportResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(counts, "counts");
        operationRefs = List.copyOf(operationRefs);
        warnings = List.copyOf(warnings);
        Objects.requireNonNull(receivedAt, "receivedAt");
    }

    public enum Status {
        ACCEPTED,
        ACCEPTED_WITH_WARNINGS,
        REJECTED
    }

    public record Counts(
            int businessDomains,
            int entityDomains,
            int interfaceGroups,
            int operations,
            int created,
            int updated,
            int missingFromThisSet
    ) {
    }

    public record OperationRef(
            String operationKey,
            String operationId,
            String changeType
    ) {
    }

    public record Warning(
            String path,
            String code,
            String message
    ) {
    }
}
