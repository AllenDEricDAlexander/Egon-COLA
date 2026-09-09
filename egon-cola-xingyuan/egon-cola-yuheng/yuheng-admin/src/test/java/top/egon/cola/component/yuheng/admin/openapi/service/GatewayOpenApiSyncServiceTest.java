package top.egon.cola.component.yuheng.admin.openapi.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.admin.application.domain.po.GatewayApplicationPO;
import top.egon.cola.component.yuheng.admin.application.repository.GatewayApplicationRepository;
import top.egon.cola.component.yuheng.admin.config.properties.GatewayAdminOpenApiProperties;
import top.egon.cola.component.yuheng.admin.openapi.client.GatewayOpenApiFetchException;
import top.egon.cola.component.yuheng.admin.openapi.client.GatewayProviderOpenApiClient;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiDefinitionDTO;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiSyncCandidateDTO;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiSyncKeyDTO;
import top.egon.cola.component.yuheng.admin.openapi.domain.enums.GatewayOpenApiSyncStateEnum;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSnapshotPO;
import top.egon.cola.component.yuheng.admin.openapi.domain.po.GatewayOpenApiSyncPO;
import top.egon.cola.component.yuheng.admin.openapi.repository.GatewayOpenApiSnapshotRepository;
import top.egon.cola.component.yuheng.admin.openapi.repository.GatewayOpenApiSyncRepository;
import top.egon.cola.component.yuheng.admin.openapi.validation.GatewayOpenApiValidationChain;
import top.egon.cola.component.yuheng.admin.openapi.validation.GatewayOpenApiValidationResult;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReportResult;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceCatalog;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceInstance;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceKey;
import top.egon.cola.component.tianshu.model.management.DdcManagementServiceSnapshot;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.yuheng.admin.openapi.converter.GatewayOpenApi31ContractAdapter;

class GatewayOpenApiSyncServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-26T03:00:00Z");

    @Test
    void claimsFetchesSnapshotsAggregatesAndCasCompletesTheGroup() {
        GatewayOpenApiSyncRepository syncStates = mock(GatewayOpenApiSyncRepository.class);
        GatewayOpenApiSnapshotRepository snapshots = mock(GatewayOpenApiSnapshotRepository.class);
        GatewayProviderOpenApiClient client = mock(GatewayProviderOpenApiClient.class);
        GatewayOpenApiValidationChain validation = mock(GatewayOpenApiValidationChain.class);
        GatewayOpenApiAggregateCoordinator coordinator = mock(GatewayOpenApiAggregateCoordinator.class);
        GatewayApplicationRepository applications = mock(GatewayApplicationRepository.class);
        GatewayOpenApiSyncCandidateDTO candidate = candidate("orders", "instance-1");
        GatewayOpenApiSyncKeyDTO key = new GatewayOpenApiSyncKeyDTO(
                "application-1", "build-1", "orders");
        GatewayOpenApiSyncPO row = row(GatewayOpenApiSyncStateEnum.DISCOVERED, 0);
        GatewayOpenApiDocumentDTO document = document(candidate);
        GatewayOpenApiSnapshotPO snapshot = snapshot(document, "snapshot-1");

        when(syncStates.findByKey(key)).thenReturn(Optional.of(row));
        when(syncStates.claim("sync-1", 0, NOW)).thenReturn(true);
        when(syncStates.transition(
                eq("sync-1"), any(Long.TYPE),
                eq(GatewayOpenApiSyncStateEnum.FETCHING),
                eq(GatewayOpenApiSyncStateEnum.VALIDATING), eq(NOW)))
                .thenReturn(true);
        when(syncStates.transition(
                eq("sync-1"), any(Long.TYPE),
                eq(GatewayOpenApiSyncStateEnum.VALIDATING),
                eq(GatewayOpenApiSyncStateEnum.INGESTING), eq(NOW)))
                .thenReturn(true);
        when(syncStates.setValid(
                eq("sync-1"), any(Long.TYPE), eq("snapshot-1"),
                eq("set-1"), eq(NOW))).thenReturn(true);
        when(client.fetch(candidate)).thenReturn(document);
        when(validation.validate(document))
                .thenReturn(GatewayOpenApiValidationResult.passed());
        when(snapshots.insertOrReuse(any())).thenReturn(snapshot);
        when(applications.findByIdAndDeletedFalse("application-1"))
                .thenReturn(Optional.of(application()));
        when(coordinator.aggregateAndIngest(
                any(top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiAggregateDTO.class),
                any(top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReport.Application.class)))
                .thenReturn(result("set-1"));

        GatewayOpenApiSyncService service = service(
                syncStates, snapshots, client, validation, coordinator,
                applications);

        service.synchronize(candidate);

        verify(syncStates).claim("sync-1", 0, NOW);
        verify(client).fetch(candidate);
        ArgumentCaptor<GatewayOpenApiSnapshotPO> persisted = ArgumentCaptor.forClass(GatewayOpenApiSnapshotPO.class);
        verify(snapshots).insertOrReuse(persisted.capture());
        assertThat(persisted.getValue().canonicalSha256())
                .isEqualTo(new GatewayOpenApi31ContractAdapter().canonicalSha256(document));
        assertThat(persisted.getValue().documentJson()).containsKey("servers");
        assertThat(persisted.getValue().operationCount()).isEqualTo(2);
        verify(coordinator).aggregateAndIngest(
                any(top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiAggregateDTO.class),
                any(top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReport.Application.class));
        verify(syncStates).setValid(
                eq("sync-1"), any(Long.TYPE), eq("snapshot-1"),
                eq("set-1"), eq(NOW));
    }

    @Test
    void lostClaimDoesNotPerformNetworkOrMutationWork() {
        GatewayOpenApiSyncRepository syncStates = mock(GatewayOpenApiSyncRepository.class);
        GatewayOpenApiSyncCandidateDTO candidate = candidate("orders", "instance-1");
        GatewayOpenApiSyncKeyDTO key = new GatewayOpenApiSyncKeyDTO(
                "application-1", "build-1", "orders");
        when(syncStates.findByKey(key)).thenReturn(Optional.of(row(
                GatewayOpenApiSyncStateEnum.DISCOVERED, 7)));
        when(syncStates.claim("sync-1", 7, NOW)).thenReturn(false);
        GatewayProviderOpenApiClient client = mock(GatewayProviderOpenApiClient.class);

        GatewayOpenApiSyncService service = service(
                syncStates,
                mock(GatewayOpenApiSnapshotRepository.class),
                client,
                mock(GatewayOpenApiValidationChain.class),
                mock(GatewayOpenApiAggregateCoordinator.class),
                mock(GatewayApplicationRepository.class));

        service.synchronize(candidate);

        verify(client, never()).fetch(any());
    }

    @Test
    void retryableFetchFailureUsesBoundedRetryStateAndSafeMessage() {
        GatewayOpenApiSyncRepository syncStates = mock(GatewayOpenApiSyncRepository.class);
        GatewayOpenApiSyncCandidateDTO candidate = candidate("orders", "instance-1");
        GatewayOpenApiSyncKeyDTO key = new GatewayOpenApiSyncKeyDTO(
                "application-1", "build-1", "orders");
        when(syncStates.findByKey(key)).thenReturn(Optional.of(row(
                GatewayOpenApiSyncStateEnum.DISCOVERED, 0)));
        when(syncStates.claim("sync-1", 0, NOW)).thenReturn(true);
        GatewayOpenApiFetchException failure = new GatewayOpenApiFetchException(
                "GATEWAY_OPENAPI_FETCH_TIMEOUT", true,
                "provider OpenAPI fetch timed out");
        GatewayProviderOpenApiClient client = mock(GatewayProviderOpenApiClient.class);
        doThrow(failure).when(client).fetch(candidate);
        GatewayApplicationRepository applications = mock(GatewayApplicationRepository.class);
        when(applications.findByIdAndDeletedFalse("application-1"))
                .thenReturn(Optional.of(application()));

        GatewayOpenApiSyncService service = service(
                syncStates,
                mock(GatewayOpenApiSnapshotRepository.class),
                client,
                mock(GatewayOpenApiValidationChain.class),
                mock(GatewayOpenApiAggregateCoordinator.class),
                applications);

        service.synchronize(candidate);

        verify(syncStates).markFailure(
                eq("sync-1"), any(Long.TYPE),
                eq(GatewayOpenApiSyncStateEnum.FETCH_FAILED),
                eq("GATEWAY_OPENAPI_FETCH_TIMEOUT"),
                eq("provider OpenAPI fetch timed out"),
                any(Instant.class), eq(NOW));
    }

    @Test
    void freshDdcManifestDiscoversAllGroupsAndIngestsOneAggregate() {
        GatewayOpenApiSyncRepository syncStates = mock(GatewayOpenApiSyncRepository.class);
        GatewayOpenApiSnapshotRepository snapshots = mock(GatewayOpenApiSnapshotRepository.class);
        GatewayProviderOpenApiClient client = mock(GatewayProviderOpenApiClient.class);
        GatewayOpenApiValidationChain validation = mock(GatewayOpenApiValidationChain.class);
        GatewayOpenApiAggregateCoordinator coordinator = mock(GatewayOpenApiAggregateCoordinator.class);
        GatewayApplicationRepository applications = mock(GatewayApplicationRepository.class);
        GatewayOpenApiSyncCandidateDTO orders = candidate("orders", "instance-1");
        GatewayOpenApiSyncCandidateDTO inventory = candidate("inventory", "instance-1");
        when(applications.findAllByDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(application()));
        when(applications.findByIdAndDeletedFalse("application-1"))
                .thenReturn(Optional.of(application()));
        DdcManagementServiceInstance instance = instance();
        DdcManagementServiceKey service = new DdcManagementServiceKey(
                "platform", "test", "orders", "service-1", "HTTP_PROVIDER",
                "orders-http", "default", "1.0.0", "https");
        DdcManagementServiceSnapshot snapshot = new DdcManagementServiceSnapshot(
                service, 4, NOW, List.of(instance));
        DdcManagementServiceCatalog catalog = new DdcManagementServiceCatalog(
                4, NOW, List.of(service));
        top.egon.cola.component.tianshu.api.client.DdcManagementClient ddc =
                mock(top.egon.cola.component.tianshu.api.client.DdcManagementClient.class);
        when(ddc.getServiceKeys(any())).thenReturn(catalog);
        when(ddc.getInstances(any())).thenReturn(snapshot);
        when(syncStates.findByKey(any())).thenReturn(Optional.empty());
        when(syncStates.upsertDiscovered(any())).thenAnswer(invocation ->
                invocation.getArgument(0));
        GatewayOpenApiSyncPO ordersRow = rowFor("orders", "sync-orders");
        GatewayOpenApiSyncPO inventoryRow = rowFor("inventory", "sync-inventory");
        when(syncStates.findDue(eq(NOW), eq(50)))
                .thenReturn(List.of(ordersRow, inventoryRow));
        when(syncStates.claim(any(), any(Long.TYPE), eq(NOW))).thenReturn(true);
        when(syncStates.transition(
                any(), any(Long.TYPE),
                eq(GatewayOpenApiSyncStateEnum.FETCHING),
                eq(GatewayOpenApiSyncStateEnum.VALIDATING), eq(NOW)))
                .thenReturn(true);
        when(syncStates.transition(
                any(), any(Long.TYPE),
                eq(GatewayOpenApiSyncStateEnum.VALIDATING),
                eq(GatewayOpenApiSyncStateEnum.INGESTING), eq(NOW)))
                .thenReturn(true);
        when(syncStates.setValid(
                any(), any(Long.TYPE), any(), eq("set-1"), eq(NOW)))
                .thenReturn(true);
        when(validation.validate(any())).thenReturn(
                GatewayOpenApiValidationResult.passed());
        GatewayOpenApiDocumentDTO ordersDocument = document(orders);
        GatewayOpenApiDocumentDTO inventoryDocument = document(inventory);
        when(client.fetch(any())).thenAnswer(invocation -> {
            GatewayOpenApiSyncCandidateDTO requested = invocation.getArgument(0);
            return "orders".equals(requested.openapiGroup())
                    ? ordersDocument
                    : inventoryDocument;
        });
        when(snapshots.insertOrReuse(any())).thenAnswer(invocation ->
                invocation.getArgument(0));
        when(coordinator.aggregateAndIngest(
                any(top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiAggregateDTO.class),
                any(top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReport.Application.class)))
                .thenReturn(result("set-1"));

        GatewayAdminOpenApiProperties properties =
                new GatewayAdminOpenApiProperties();
        GatewayOpenApiSyncService serviceUnderTest = new GatewayOpenApiSyncService(
                ddc,
                applications,
                syncStates,
                snapshots,
                client,
                validation,
                coordinator,
                properties,
                new SimpleMeterRegistry(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        int claimed = serviceUnderTest.reconcile();
        verify(ddc).getServiceKeys(any());
        verify(ddc).getInstances(any());
        verify(syncStates).findDue(eq(NOW), eq(50));
        verify(syncStates, org.mockito.Mockito.times(2)).upsertDiscovered(any());
        assertThat(claimed).isEqualTo(2);
        verify(syncStates, org.mockito.Mockito.times(2))
                .upsertDiscovered(any());
        verify(coordinator).aggregateAndIngest(
                any(top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiAggregateDTO.class),
                any(top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReport.Application.class));
        verify(syncStates, org.mockito.Mockito.times(2)).setValid(
                any(), any(Long.TYPE), any(), eq("set-1"), eq(NOW));
    }

    @Test
    void staleDdcObservationAbortsWithoutMarkingRowsStale() {
        GatewayApplicationRepository applications = mock(GatewayApplicationRepository.class);
        when(applications.findAllByDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(application()));
        top.egon.cola.component.tianshu.api.client.DdcManagementClient ddc =
                mock(top.egon.cola.component.tianshu.api.client.DdcManagementClient.class);
        DdcManagementServiceKey service = new DdcManagementServiceKey(
                "platform", "test", "orders", "service-1", "HTTP_PROVIDER",
                "orders-http", "default", "1.0.0", "https");
        when(ddc.getServiceKeys(any())).thenReturn(
                new DdcManagementServiceCatalog(4, NOW.minusSeconds(301), List.of(service)));
        GatewayOpenApiSyncRepository syncStates = mock(GatewayOpenApiSyncRepository.class);
        GatewayOpenApiSyncService serviceUnderTest = new GatewayOpenApiSyncService(
                ddc,
                applications,
                syncStates,
                mock(GatewayOpenApiSnapshotRepository.class),
                mock(GatewayProviderOpenApiClient.class),
                mock(GatewayOpenApiValidationChain.class),
                mock(GatewayOpenApiAggregateCoordinator.class),
                new GatewayAdminOpenApiProperties(),
                new SimpleMeterRegistry(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        assertThat(serviceUnderTest.reconcile()).isZero();

        verify(syncStates, never()).markFailure(
                any(), any(Long.TYPE), any(), any(), any(), any(), any());
    }

    @Test
    void repairsAnIngestingRowFromAnAlreadyLinkedSnapshot() {
        GatewayApplicationRepository applications = mock(GatewayApplicationRepository.class);
        when(applications.findAllByDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(application()));
        top.egon.cola.component.tianshu.api.client.DdcManagementClient ddc =
                mock(top.egon.cola.component.tianshu.api.client.DdcManagementClient.class);
        when(ddc.getServiceKeys(any())).thenReturn(
                new DdcManagementServiceCatalog(4, NOW, List.of()));
        GatewayOpenApiSyncRepository syncStates = mock(GatewayOpenApiSyncRepository.class);
        GatewayOpenApiSyncPO ingesting = rowFor(
                "orders", "sync-orders", GatewayOpenApiSyncStateEnum.INGESTING);
        when(syncStates.findByStatus(GatewayOpenApiSyncStateEnum.INGESTING))
                .thenReturn(List.of(ingesting));
        GatewayOpenApiDocumentDTO document = document(candidate("orders", "instance-1"));
        GatewayOpenApiSnapshotPO linked = new GatewayOpenApiSnapshotPO(
                "snapshot-1",
                "application-1",
                "set-1",
                "build-1",
                "1.0.0",
                "orders",
                "3.1.0",
                document.documentSha256(),
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                Map.of("openapi", "3.1.0"),
                "VALID",
                List.of(),
                0,
                0,
                "instance-1",
                NOW,
                NOW,
                NOW);
        GatewayOpenApiSnapshotRepository snapshots = mock(GatewayOpenApiSnapshotRepository.class);
        when(snapshots.findById("snapshot-1")).thenReturn(Optional.of(linked));
        when(syncStates.setValid(
                "sync-orders", 0, "snapshot-1", "set-1", NOW))
                .thenReturn(true);

        GatewayOpenApiSyncService serviceUnderTest = new GatewayOpenApiSyncService(
                ddc,
                applications,
                syncStates,
                snapshots,
                mock(GatewayProviderOpenApiClient.class),
                mock(GatewayOpenApiValidationChain.class),
                mock(GatewayOpenApiAggregateCoordinator.class),
                new GatewayAdminOpenApiProperties(),
                new SimpleMeterRegistry(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        serviceUnderTest.reconcile();

        verify(syncStates).setValid(
                "sync-orders", 0, "snapshot-1", "set-1", NOW);
    }

    private GatewayOpenApiSyncService service(
            GatewayOpenApiSyncRepository syncStates,
            GatewayOpenApiSnapshotRepository snapshots,
            GatewayProviderOpenApiClient client,
            GatewayOpenApiValidationChain validation,
            GatewayOpenApiAggregateCoordinator coordinator,
            GatewayApplicationRepository applications) {
        GatewayAdminOpenApiProperties properties =
                new GatewayAdminOpenApiProperties();
        return new GatewayOpenApiSyncService(
                mock(top.egon.cola.component.tianshu.api.client.DdcManagementClient.class),
                applications,
                syncStates,
                snapshots,
                client,
                validation,
                coordinator,
                properties,
                new SimpleMeterRegistry(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private GatewayOpenApiSyncCandidateDTO candidate(String group, String instanceId) {
        return new GatewayOpenApiSyncCandidateDTO(
                "application-1", "platform", "orders", "build-1", "1.0.0",
                group, "orders-http", "default", "1.0.0", instanceId,
                "provider.internal", 18443, true,
                "/v3/api-docs/{group}", URI.create("https://orders.internal"),
                NOW.minusSeconds(1), NOW.plusSeconds(30));
    }

    private GatewayOpenApiDocumentDTO document(
            GatewayOpenApiSyncCandidateDTO candidate) {
        var json = JsonNodeFactory.instance.objectNode().put("openapi", "3.1.0");
        json.putArray("servers").addObject().put("url", "https://provider.internal");
        var path = json.putObject("paths").putObject("/orders");
        path.putArray("parameters");
        path.putObject("get").put("operationId", "orders.list");
        path.putObject("post").put("operationId", "orders.create");
        json.putObject("components").putObject("schemas").putObject("Order")
                .put("type", "object").putArray("required").add("z").add("a");
        byte[] raw = json.toString().getBytes(StandardCharsets.UTF_8);
        return new GatewayOpenApiDocumentDTO(
                candidate,
                raw,
                sha(raw),
                json,
                "application/json",
                200,
                NOW);
    }

    private GatewayOpenApiSnapshotPO snapshot(
            GatewayOpenApiDocumentDTO document,
            String id) {
        return new GatewayOpenApiSnapshotPO(
                id,
                "application-1",
                null,
                "build-1",
                "1.0.0",
                document.candidate().openapiGroup(),
                "3.1.0",
                document.documentSha256(),
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                Map.of("openapi", "3.1.0"),
                "VALID",
                List.of(),
                0,
                0,
                document.candidate().instanceId(),
                NOW,
                NOW,
                NOW);
    }

    private GatewayOpenApiSyncPO row(
            GatewayOpenApiSyncStateEnum status,
            long revision) {
        return new GatewayOpenApiSyncPO(
                "sync-1", "application-1", "build-1", "1.0.0", "orders",
                "orders-http", "default", "1.0.0", status, null, null,
                null, 0, null, null, NOW.minusSeconds(10), null, null,
                null, revision, NOW.minusSeconds(10));
    }

    private GatewayOpenApiSyncPO rowFor(String group, String id) {
        return rowFor(group, id, GatewayOpenApiSyncStateEnum.DISCOVERED);
    }

    private GatewayOpenApiSyncPO rowFor(
            String group,
            String id,
            GatewayOpenApiSyncStateEnum status) {
        GatewayOpenApiSyncPO base = row(
                status,
                0
        );
        String snapshotId = status == GatewayOpenApiSyncStateEnum.INGESTING
                ? "snapshot-1"
                : base.latestSnapshotId();
        return new GatewayOpenApiSyncPO(
                id,
                base.applicationId(),
                base.buildId(),
                base.artifactVersion(),
                group,
                base.providerServiceName(),
                base.providerGroup(),
                base.providerVersion(),
                base.status(),
                snapshotId,
                base.definitionSetId(),
                base.lastInstanceId(),
                base.attemptCount(),
                base.lastErrorCode(),
                base.lastErrorMessage(),
                base.firstDiscoveredAt(),
                base.lastAttemptAt(),
                base.lastSuccessAt(),
                base.nextRetryAt(),
                base.revision(),
                base.updatedAt());
    }

    private DdcManagementServiceInstance instance() {
        return new DdcManagementServiceInstance(
                "instance-1",
                "lease-1",
                "provider.internal",
                18443,
                true,
                Map.of(
                        "gateway.definition-source", "OPENAPI31",
                        "gateway.openapi.enabled", "true",
                        "gateway.openapi.path-template", "/v3/api-docs/{group}",
                        "gateway.openapi.spec", "3.1",
                        "gateway.openapi.groups", "inventory,orders",
                        "gateway.openapi.resource-uri", "https://orders.internal",
                        "gateway.artifact-version", "1.0.0",
                        "gateway.build-id", "build-1"),
                "ONLINE",
                NOW.minusSeconds(10),
                NOW.minusSeconds(1),
                NOW.plusSeconds(60));
    }

    private GatewayApplicationPO application() {
        return new GatewayApplicationPO(
                "application-1", "platform", "orders", "Orders", "test",
                "gateway", null, "admin", NOW.minusSeconds(100));
    }

    private GatewayInterfaceDefinitionReportResult result(String setId) {
        return new GatewayInterfaceDefinitionReportResult(
                "report-1", setId,
                GatewayInterfaceDefinitionReportResult.Status.ACCEPTED,
                "application-1",
                new GatewayInterfaceDefinitionReportResult.Counts(0, 0, 1, 0, 1, 0, 0),
                List.of(), List.of(), NOW);
    }

    private static String sha(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
    }
}
