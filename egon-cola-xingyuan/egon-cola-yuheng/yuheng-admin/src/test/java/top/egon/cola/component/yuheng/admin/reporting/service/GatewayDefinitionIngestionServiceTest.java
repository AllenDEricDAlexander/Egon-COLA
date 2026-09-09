package top.egon.cola.component.yuheng.admin.reporting.service;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSnapshotPO;
import top.egon.cola.component.yuheng.admin.openapi.repository.GatewayOpenApiSnapshotRepository;
import top.egon.cola.component.yuheng.admin.reporting.domain.po.GatewayStoredReportPO;
import top.egon.cola.component.yuheng.admin.reporting.repository.GatewayDefinitionReportRepository;
import top.egon.cola.component.yuheng.admin.reporting.domain.dto.GatewayDefinitionIngestionCommandDTO;
import top.egon.cola.component.yuheng.contract.reporting.GatewayDefinitionSourceTypeEnum;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReportResult;

import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayDefinitionIngestionServiceTest {

    private static final Instant NOW = Instant.parse(
            "2026-08-26T03:00:00Z"
    );

    @Test
    void openapiCommandLinksEveryValidatedSnapshotAfterDefinitionWrite() {
        GatewayDefinitionReportRepository reports =
                mock(GatewayDefinitionReportRepository.class);
        GatewayOpenApiSnapshotRepository snapshots =
                mock(GatewayOpenApiSnapshotRepository.class);
        GatewayInterfaceDefinitionReport report = report(
                "set-http-1",
                "HTTP",
                "OPENAPI31"
        );
        when(reports.definitionSetExists("app-1", report.definitionSetId()))
                .thenReturn(false);
        when(reports.ingest(eq("app-1"), any(), any()))
                .thenReturn(new GatewayStoredReportPO(1, 0, List.of()));
        when(snapshots.findById("snapshot-orders"))
                .thenReturn(Optional.of(snapshot("snapshot-orders")));
        when(snapshots.linkAllToDefinitionSet(
                List.of("snapshot-orders"),
                report.definitionSetId()
        )).thenReturn(1);
        GatewayDefinitionIngestionService service = service(
                reports,
                snapshots
        );

        GatewayInterfaceDefinitionReportResult result = service.ingest(
                new GatewayDefinitionIngestionCommandDTO(
                        "app-1",
                        GatewayDefinitionSourceTypeEnum.OPENAPI31,
                        "a".repeat(64),
                        report,
                        List.of("snapshot-orders")
                )
        );

        assertThat(result.definitionSetId()).isEqualTo(report.definitionSetId());
        verify(snapshots).linkAllToDefinitionSet(
                List.of("snapshot-orders"),
                report.definitionSetId()
        );
        verify(reports).ingest(eq("app-1"), any(), eq(NOW));
    }

    @Test
    void rpcCommandMustNotCarryOpenapiSnapshots() {
        GatewayDefinitionIngestionService service = service(
                mock(GatewayDefinitionReportRepository.class),
                mock(GatewayOpenApiSnapshotRepository.class)
        );

        assertThatThrownBy(() -> service.ingest(
                new GatewayDefinitionIngestionCommandDTO(
                        "app-1",
                        GatewayDefinitionSourceTypeEnum.RPC_DESCRIPTOR,
                        "rpc:orders:1",
                        report("set-rpc-1", "RPC", "RPC_DESCRIPTOR"),
                        List.of("snapshot-orders")
                )
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("snapshotIds");
    }

    @Test
    void snapshotLinkFailureIsPropagatedForTransactionRollback() {
        GatewayDefinitionReportRepository reports =
                mock(GatewayDefinitionReportRepository.class);
        GatewayOpenApiSnapshotRepository snapshots =
                mock(GatewayOpenApiSnapshotRepository.class);
        GatewayInterfaceDefinitionReport report = report(
                "set-http-1",
                "HTTP",
                "OPENAPI31"
        );
        when(reports.definitionSetExists("app-1", report.definitionSetId()))
                .thenReturn(false);
        when(reports.ingest(eq("app-1"), any(), any()))
                .thenReturn(new GatewayStoredReportPO(1, 0, List.of()));
        when(snapshots.findById("snapshot-orders"))
                .thenReturn(Optional.of(snapshot("snapshot-orders")));
        when(snapshots.linkAllToDefinitionSet(
                List.of("snapshot-orders"),
                report.definitionSetId()
        )).thenThrow(new IllegalStateException("link failure"));
        GatewayDefinitionIngestionService service = service(
                reports,
                snapshots
        );

        assertThatThrownBy(() -> service.ingest(
                new GatewayDefinitionIngestionCommandDTO(
                        "app-1",
                        GatewayDefinitionSourceTypeEnum.OPENAPI31,
                        "a".repeat(64),
                        report,
                        List.of("snapshot-orders")
                )
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("link failure");
    }

    private GatewayDefinitionIngestionService service(
            GatewayDefinitionReportRepository reports,
            GatewayOpenApiSnapshotRepository snapshots) {
        return new GatewayDefinitionIngestionService(
                reports,
                snapshots,
                new GatewayOperationSchemaValidator(new com.fasterxml.jackson.databind.ObjectMapper()),
                new GatewayReportCanonicalizer(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private GatewayOpenApiSnapshotPO snapshot(String id) {
        return new GatewayOpenApiSnapshotPO(
                id,
                "app-1",
                null,
                "build-1",
                "1.0.0",
                "orders",
                "3.1.0",
                "a".repeat(64),
                "b".repeat(64),
                Map.of("openapi", "3.1.0"),
                "VALID",
                List.of(),
                1,
                0,
                "instance-1",
                NOW,
                NOW,
                NOW
        );
    }

    private GatewayInterfaceDefinitionReport report(
            String setId,
            String protocol,
            String source) {
        GatewayInterfaceDefinitionReport.ProviderService provider =
                new GatewayInterfaceDefinitionReport.ProviderService(
                        "trade",
                        "orders",
                        "test",
                        "default",
                        protocol,
                        "orders-service",
                        "default",
                        "1.0.0",
                        protocol
                );
        GatewayInterfaceDefinitionReport.Operation operation =
                new GatewayInterfaceDefinitionReport.Operation(
                        "orders:" + protocol.toLowerCase() + ":GET:/orders",
                        protocol,
                        "GET /orders",
                        "orders",
                        "summary",
                        "description",
                        "orders-team",
                        List.of(),
                        false,
                        "SUPPORTED",
                        provider,
                        Map.of(
                                "$schema", "https://json-schema.org/draft/2020-12/schema",
                                "x-egon-schema-model", "gateway-operation-request/v2",
                                "type", "object",
                                "properties", Map.of(),
                                "additionalProperties", false
                        ),
                        Map.of(
                                "$schema", "https://json-schema.org/draft/2020-12/schema",
                                "x-egon-schema-model", "gateway-operation-response/v2",
                                "type", "object",
                                "properties", Map.of(),
                                "additionalProperties", false
                        ),
                        List.of(),
                        "RPC_DESCRIPTOR".equals(source)
                                ? Map.of("descriptor", "base64")
                                : null,
                        Map.of(),
                        false
                );
        GatewayInterfaceDefinitionReport.InterfaceGroup group =
                new GatewayInterfaceDefinitionReport.InterfaceGroup(
                        "orders",
                        "Orders",
                        null,
                        GatewayDefinitionSourceTypeEnum.valueOf(source),
                        null,
                        protocol,
                        Map.of(),
                        List.of(operation)
                );
        GatewayInterfaceDefinitionReport.EntityDomain entity =
                new GatewayInterfaceDefinitionReport.EntityDomain(
                        "order",
                        "Order",
                        null,
                        List.of(group)
                );
        GatewayInterfaceDefinitionReport.BusinessDomain business =
                new GatewayInterfaceDefinitionReport.BusinessDomain(
                        "trade",
                        "Trade",
                        null,
                        List.of(entity)
                );
        GatewayInterfaceDefinitionReport.Application application =
                new GatewayInterfaceDefinitionReport.Application(
                        "trade",
                        "orders",
                        "Orders",
                        "test",
                        "default"
                );
        GatewayInterfaceDefinitionReport.Build build =
                new GatewayInterfaceDefinitionReport.Build(
                        "1.0.0",
                        "build-1",
                        Map.of()
                );
        GatewayReportCanonicalizer canonicalizer =
                new GatewayReportCanonicalizer();
        String fingerprint = canonicalizer.definitionFingerprint(
                application,
                build,
                true,
                List.of(business)
        );
        return new GatewayInterfaceDefinitionReport(
                "v2",
                "report-1",
                NOW,
                application,
                build,
                true,
                canonicalizer.definitionSetId(application, build, fingerprint),
                fingerprint,
                List.of(business)
        );
    }
}
