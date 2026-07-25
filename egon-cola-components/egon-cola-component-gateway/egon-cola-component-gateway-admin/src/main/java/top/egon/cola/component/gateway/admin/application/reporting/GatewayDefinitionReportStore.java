package top.egon.cola.component.gateway.admin.application.reporting;

import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GatewayDefinitionReportStore {

    Optional<String> findBuildFingerprint(
            String applicationId,
            String buildId);

    boolean definitionSetExists(
            String applicationId,
            String definitionSetId);

    int countStarterOperations(String applicationId);

    StoredReport ingest(
            String applicationId,
            GatewayInterfaceDefinitionReport report,
            Instant now);

    record StoredReport(
            int created,
            int updated,
            List<GatewayInterfaceDefinitionReportResult.OperationRef>
                    operationRefs
    ) {
    }
}
