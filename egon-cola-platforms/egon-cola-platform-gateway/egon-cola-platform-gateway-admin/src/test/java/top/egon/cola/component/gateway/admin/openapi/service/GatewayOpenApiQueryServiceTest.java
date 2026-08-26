package top.egon.cola.component.gateway.admin.openapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.application.domain.po.GatewayApplicationPO;
import top.egon.cola.component.gateway.admin.application.repository.GatewayApplicationRepository;
import top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationDefinitionPO;
import top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO;
import top.egon.cola.component.gateway.admin.catalog.repository.GatewayCatalogRepository;
import top.egon.cola.component.gateway.admin.openapi.domain.enums.GatewayOpenApiSyncStateEnum;
import top.egon.cola.component.gateway.admin.openapi.domain.po.GatewayOpenApiSnapshotPO;
import top.egon.cola.component.gateway.admin.openapi.domain.po.GatewayOpenApiSyncPO;
import top.egon.cola.component.gateway.admin.openapi.repository.GatewayOpenApiSnapshotRepository;
import top.egon.cola.component.gateway.admin.openapi.repository.GatewayOpenApiSyncRepository;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayOpenApiSourceNotAvailableException;
import top.egon.cola.component.gateway.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.gateway.contract.reporting.GatewayDefinitionSourceTypeEnum;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayOpenApiQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T03:00:00Z");

    @Test
    void listsSortedStateRowsAndJoinsSnapshotMetadataReadOnly() {
        GatewayApplicationRepository applications = mock(GatewayApplicationRepository.class);
        GatewayOpenApiSyncRepository syncStates = mock(GatewayOpenApiSyncRepository.class);
        GatewayOpenApiSnapshotRepository snapshots = mock(GatewayOpenApiSnapshotRepository.class);
        GatewayCatalogRepository catalog = mock(GatewayCatalogRepository.class);
        GatewayApplicationPO application = application();
        when(applications.findAllByDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(application));
        when(syncStates.findByApplicationId("application-1"))
                .thenReturn(List.of(
                        row("inventory", "build-2", GatewayOpenApiSyncStateEnum.INVALID),
                        row("orders", "build-1", GatewayOpenApiSyncStateEnum.VALID)
                ));
        when(snapshots.findById("snapshot-orders"))
                .thenReturn(Optional.of(snapshot("snapshot-orders", "orders", "VALID")));
        when(snapshots.findById("snapshot-inventory"))
                .thenReturn(Optional.of(snapshot("snapshot-inventory", "inventory", "INVALID")));

        GatewayOpenApiQueryService service = service(
                syncStates, snapshots, catalog, applications);

        var result = service.listSyncStates(null, null, null, null);

        assertThat(result).extracting("openapiGroup")
                .containsExactly("orders", "inventory");
        assertThat(result.get(1).canonicalSha256()).hasSize(64);
        assertThat(result.get(0).operationCount()).isZero();
        verify(syncStates).findByApplicationId("application-1");
        verify(syncStates, never()).markFailure(
                any(), any(Long.TYPE), any(), any(), any(), any(), any());
    }

    @Test
    void resolvesOpenApiOperationFragmentAndStoredSnapshotWithoutNetwork() {
        GatewayApplicationRepository applications = mock(GatewayApplicationRepository.class);
        GatewayOpenApiSyncRepository syncStates = mock(GatewayOpenApiSyncRepository.class);
        GatewayOpenApiSnapshotRepository snapshots = mock(GatewayOpenApiSnapshotRepository.class);
        GatewayCatalogRepository catalog = mock(GatewayCatalogRepository.class);
        GatewayOperationPO operation = operation("OPENAPI31");
        GatewayOperationDefinitionPO definition = definition();
        when(catalog.findOperation("operation-1")).thenReturn(Optional.of(operation));
        when(catalog.loadDefinitions("operation-1")).thenReturn(List.of(definition));
        when(snapshots.findByApplicationGroupAndCanonicalSha256(
                "application-1", "orders", HASH))
                .thenReturn(Optional.of(snapshot("snapshot-orders", "orders", "VALID")));

        GatewayOpenApiQueryService service = service(
                syncStates, snapshots, catalog, applications);

        var result = service.getOperationOpenApi("operation-1");

        assertThat(result.operationId()).isEqualTo("operation-1");
        assertThat(result.snapshotId()).isEqualTo("snapshot-orders");
        assertThat(result.openapiGroup()).isEqualTo("orders");
        assertThat(result.path()).isEqualTo("/orders/{id}");
        assertThat(result.method()).isEqualTo("GET");
        assertThat(result.operation()).containsEntry("operationId", "getOrder");
        assertThat(result.requestContentTypes()).containsExactly("application/json");
        assertThat(result.responseContentTypes()).containsExactly("application/json");
        verify(syncStates, never()).upsertDiscovered(any());
    }

    @Test
    void doesNotPretendRpcOrManualDefinitionsHaveOpenApiFragments() {
        GatewayCatalogRepository catalog = mock(GatewayCatalogRepository.class);
        GatewayOperationPO operation = operation("RPC_DESCRIPTOR");
        when(catalog.findOperation("operation-1")).thenReturn(Optional.of(operation));
        GatewayOpenApiQueryService service = service(
                mock(GatewayOpenApiSyncRepository.class),
                mock(GatewayOpenApiSnapshotRepository.class),
                catalog,
                mock(GatewayApplicationRepository.class));

        assertThatThrownBy(() -> service.getOperationOpenApi("operation-1"))
                .isInstanceOf(GatewayOpenApiSourceNotAvailableException.class);
    }

    @Test
    void missingSnapshotIsReportedAsNotFound() {
        GatewayCatalogRepository catalog = mock(GatewayCatalogRepository.class);
        when(catalog.findOperation("operation-1"))
                .thenReturn(Optional.of(operation("OPENAPI31")));
        when(catalog.loadDefinitions("operation-1"))
                .thenReturn(List.of(definition()));
        GatewayOpenApiQueryService service = service(
                mock(GatewayOpenApiSyncRepository.class),
                mock(GatewayOpenApiSnapshotRepository.class),
                catalog,
                mock(GatewayApplicationRepository.class));

        assertThatThrownBy(() -> service.getOperationOpenApi("operation-1"))
                .isInstanceOf(GatewayAdminNotFoundException.class);
    }

    @Test
    void mapsImmutableRawSnapshotMetadataAndJson() {
        GatewayOpenApiSnapshotRepository snapshots = mock(GatewayOpenApiSnapshotRepository.class);
        when(snapshots.findById("snapshot-orders"))
                .thenReturn(Optional.of(snapshot("snapshot-orders", "orders", "INVALID")));
        GatewayOpenApiQueryService service = service(
                mock(GatewayOpenApiSyncRepository.class),
                snapshots,
                mock(GatewayCatalogRepository.class),
                mock(GatewayApplicationRepository.class));

        var result = service.getSnapshotDocument("snapshot-orders");

        assertThat(result.snapshotId()).isEqualTo("snapshot-orders");
        assertThat(result.document()).containsEntry("openapi", "3.1.0");
        assertThat(result.documentSha256()).hasSize(64);
    }

    private GatewayOpenApiQueryService service(
            GatewayOpenApiSyncRepository syncStates,
            GatewayOpenApiSnapshotRepository snapshots,
            GatewayCatalogRepository catalog,
            GatewayApplicationRepository applications) {
        return new GatewayOpenApiQueryService(
                syncStates,
                snapshots,
                catalog,
                applications,
                new ObjectMapper());
    }

    private GatewayApplicationPO application() {
        return new GatewayApplicationPO(
                "application-1", "platform", "orders", "Orders", "test",
                "gateway", null, "admin", NOW);
    }

    private GatewayOpenApiSyncPO row(
            String group,
            String build,
            GatewayOpenApiSyncStateEnum state) {
        return new GatewayOpenApiSyncPO(
                "sync-" + group,
                "application-1",
                build,
                "1.0.0",
                group,
                "orders-http",
                "default",
                "1.0.0",
                state,
                "snapshot-" + group,
                state == GatewayOpenApiSyncStateEnum.VALID ? "set-1" : null,
                "instance-1",
                0,
                null,
                null,
                NOW,
                null,
                null,
                null,
                0,
                NOW);
    }

    private GatewayOpenApiSnapshotPO snapshot(
            String id,
            String group,
            String status) {
        return new GatewayOpenApiSnapshotPO(
                id,
                "application-1",
                status.equals("VALID") ? "set-1" : null,
                "build-1",
                "1.0.0",
                group,
                "3.1.0",
                HASH,
                HASH,
                Map.of(
                        "openapi", "3.1.0",
                        "paths", Map.of(
                                "/orders/{id}", Map.of(
                                        "get", Map.of(
                                                "operationId", "getOrder",
                                                "responses", Map.of("200", Map.of()))
                                )
                        )
                ),
                status,
                List.of(),
                0,
                0,
                "instance-1",
                NOW,
                NOW,
                NOW);
    }

    private GatewayOperationPO operation(String sourceType) {
        return new GatewayOperationPO(
                "operation-1",
                "application-1",
                "group-1",
                "orders:http:GET:/orders/{id}",
                "HTTP",
                "GET /orders/{id}",
                true,
                Map.of(),
                sourceType,
                "ACTIVE",
                "definition-1",
                0,
                NOW,
                NOW);
    }

    private GatewayOperationDefinitionPO definition() {
        return new GatewayOperationDefinitionPO(
                "definition-1",
                "operation-1",
                1,
                HASH,
                "get order",
                List.of("orders"),
                Map.of(),
                Map.of(),
                List.of(),
                null,
                Map.of(
                        "openapiGroup", "orders",
                        "openapiCanonicalSha256", HASH,
                        "path", "/orders/{id}",
                        "httpMethod", "GET",
                        "openapiOperationId", "getOrder",
                        "consumes", List.of("application/json"),
                        "produces", List.of("application/json")
                ),
                true,
                NOW,
                "admin");
    }

    private static final String HASH =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
}
