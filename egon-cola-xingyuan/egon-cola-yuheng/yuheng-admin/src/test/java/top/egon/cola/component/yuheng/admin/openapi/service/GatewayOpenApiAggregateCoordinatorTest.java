package top.egon.cola.component.yuheng.admin.openapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.admin.openapi.GatewayOpenApiValidationTestFixture;
import top.egon.cola.component.yuheng.admin.openapi.converter.GatewayOpenApi31ContractAdapter;
import top.egon.cola.component.yuheng.admin.openapi.converter.GatewayOpenApiDefinitionConverter;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiAggregateDTO;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiDefinitionDTO;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;
import top.egon.cola.component.yuheng.admin.reporting.service.GatewayDefinitionIngestionService;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.yuheng.contract.reporting.GatewayInterfaceDefinitionReportResult;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GatewayOpenApiAggregateCoordinatorTest {

    private static final Instant NOW = Instant.parse(
            "2026-08-26T03:00:00Z"
    );

    private GatewayDefinitionIngestionService ingestion;

    private GatewayOpenApi31ContractAdapter adapter;

    private GatewayOpenApiAggregateCoordinator coordinator;

    private final GatewayInterfaceDefinitionReport.Application application =
            new GatewayInterfaceDefinitionReport.Application(
                    "trade",
                    "orders",
                    "Orders",
                    "test",
                    "default"
            );

    @BeforeEach
    void setUp() {
        ingestion = mock(GatewayDefinitionIngestionService.class);
        adapter = mock(GatewayOpenApi31ContractAdapter.class);
        coordinator = new GatewayOpenApiAggregateCoordinator(
                adapter,
                Mappers.getMapper(GatewayOpenApiDefinitionConverter.class),
                ingestion,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void completeManifestCreatesOneCommandWithAllSortedSnapshots() {
        GatewayOpenApiDefinitionDTO orders = definition(
                "orders",
                "orders:http:GET:/orders",
                "a"
        );
        GatewayOpenApiDefinitionDTO inventory = definition(
                "inventory",
                "inventory:http:GET:/inventory",
                "b"
        );
        GatewayOpenApiDocumentDTO ordersDocument =
                GatewayOpenApiValidationTestFixture.document();
        GatewayOpenApiDocumentDTO inventoryDocument =
                GatewayOpenApiValidationTestFixture.document(
                        new top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiSyncCandidateDTO(
                                "app-1",
                                "trade",
                                "orders",
                                "build-1",
                                "1.0.0",
                                "inventory",
                                "orders-service",
                                "default",
                                "1.0.0",
                                "instance-1",
                                "provider.internal",
                                9443,
                                true,
                                "/v3/api-docs/{group}",
                                java.net.URI.create(
                                        "https://provider.example/resource"
                                ),
                                NOW,
                                NOW.plusSeconds(60)
                        ),
                        "{\"openapi\":\"3.1.0\"}"
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        "application/json",
                        200
                );
        when(adapter.adapt(any(), any())).thenAnswer(invocation ->
                "orders".equals(
                        invocation.getArgument(
                                0,
                                GatewayOpenApiDocumentDTO.class
                        ).candidate().openapiGroup()
                ) ? orders : inventory
        );
        GatewayInterfaceDefinitionReportResult accepted = result(
                "set-http-1"
        );
        when(ingestion.ingest(any())).thenReturn(accepted);

        GatewayOpenApiAggregateDTO aggregate =
                GatewayOpenApiAggregateDTO.of(
                        "app-1",
                        "build-1",
                        List.of("orders", "inventory"),
                        Map.of(
                                "orders", ordersDocument,
                                "inventory", inventoryDocument
                        ),
                        Map.of(
                                "orders", "snapshot-orders",
                                "inventory", "snapshot-inventory"
                        )
                );

        assertThat(coordinator.aggregateAndIngest(aggregate, application))
                .isSameAs(accepted);
        verify(ingestion).ingest(argThat(command ->
                command.applicationId().equals("app-1")
                        && command.sourceScope().equals(
                        aggregate.aggregateSha256()
                )
                        && command.snapshotIds().equals(List.of(
                        "snapshot-inventory",
                        "snapshot-orders"
                ))
                        && command.report().businessDomains().stream()
                        .flatMap(domain -> domain.entityDomains().stream())
                        .flatMap(domain -> domain.interfaceGroups().stream())
                        .flatMap(group -> group.operations().stream())
                        .count() == 2
        ));
    }

    @Test
    void missingOrExtraManifestGroupNeverCallsIngestion() {
        GatewayOpenApiDocumentDTO document =
                GatewayOpenApiValidationTestFixture.document();
        assertThatThrownBy(() -> new GatewayOpenApiAggregateDTO(
                "app-1",
                "build-1",
                List.of("orders", "inventory"),
                Map.of("orders", document),
                List.of("snapshot-orders"),
                "a".repeat(64)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("documents");
        verifyNoInteractions(ingestion);
    }

    @Test
    void duplicateOperationKeyAcrossGroupsFailsClosed() {
        GatewayOpenApiDefinitionDTO first = definition(
                "orders",
                "same-operation",
                "a"
        );
        GatewayOpenApiDefinitionDTO second = definition(
                "inventory",
                "same-operation",
                "b"
        );
        when(adapter.adapt(any(), any())).thenReturn(first, second);
        GatewayOpenApiAggregateDTO aggregate = GatewayOpenApiAggregateDTO.of(
                "app-1",
                "build-1",
                List.of("orders", "inventory"),
                Map.of(
                        "orders", GatewayOpenApiValidationTestFixture.document(),
                        "inventory", GatewayOpenApiValidationTestFixture.document(
                                new top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiSyncCandidateDTO(
                                        "app-1",
                                        "trade",
                                        "orders",
                                        "build-1",
                                        "1.0.0",
                                        "inventory",
                                        "orders-service",
                                        "default",
                                        "1.0.0",
                                        "instance-1",
                                        "provider.internal",
                                        9443,
                                        true,
                                        "/v3/api-docs/{group}",
                                        java.net.URI.create(
                                                "https://provider.example/resource"
                                        ),
                                        NOW,
                                        NOW.plusSeconds(60)
                                ),
                                "{\"openapi\":\"3.1.0\"}"
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                "application/json",
                                200
                        )
                ),
                Map.of(
                        "orders", "snapshot-orders",
                        "inventory", "snapshot-inventory"
                )
        );

        assertThatThrownBy(() -> coordinator.aggregateAndIngest(
                aggregate,
                application
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operationKey");
        verifyNoInteractions(ingestion);
    }

    private GatewayInterfaceDefinitionReportResult result(String setId) {
        return new GatewayInterfaceDefinitionReportResult(
                "report-1",
                setId,
                GatewayInterfaceDefinitionReportResult.Status.ACCEPTED,
                "app-1",
                new GatewayInterfaceDefinitionReportResult.Counts(
                        1,
                        1,
                        2,
                        2,
                        2,
                        0,
                        0
                ),
                List.of(),
                List.of(),
                NOW
        );
    }

    private GatewayOpenApiDefinitionDTO definition(
            String groupCode,
            String operationKey,
            String hashPrefix) {
        GatewayOpenApiDefinitionDTO.ProviderService provider =
                new GatewayOpenApiDefinitionDTO.ProviderService(
                        "trade",
                        "orders",
                        "test",
                        "default",
                        "HTTP",
                        "orders-service",
                        "default",
                        "1.0.0",
                        "HTTP"
                );
        GatewayOpenApiDefinitionDTO.Operation operation =
                new GatewayOpenApiDefinitionDTO.Operation(
                        operationKey,
                        "HTTP",
                        "GET /" + groupCode,
                        operationKey,
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
                        null,
                        Map.of(
                                "httpMethod", "GET",
                                "path", "/" + groupCode,
                                "openapiGroup", groupCode,
                                "responseMode", "TRANSPARENT",
                                "streaming", false,
                                "idempotent", true
                        ),
                        false
                );
        GatewayOpenApiDefinitionDTO.InterfaceGroup group =
                new GatewayOpenApiDefinitionDTO.InterfaceGroup(
                        groupCode,
                        groupCode,
                        null,
                        top.egon.cola.component.yuheng.contract.reporting.GatewayDefinitionSourceTypeEnum.OPENAPI31,
                        null,
                        "HTTP",
                        Map.of("openapiGroup", groupCode),
                        List.of(operation)
                );
        GatewayOpenApiDefinitionDTO.EntityDomain entity =
                new GatewayOpenApiDefinitionDTO.EntityDomain(
                        groupCode,
                        groupCode,
                        null,
                        List.of(group)
                );
        GatewayOpenApiDefinitionDTO.BusinessDomain business =
                new GatewayOpenApiDefinitionDTO.BusinessDomain(
                        "trade",
                        "Trade",
                        null,
                        List.of(entity)
                );
        return new GatewayOpenApiDefinitionDTO(
                (hashPrefix + "0").repeat(64).substring(0, 64),
                "v2",
                "report-" + groupCode,
                NOW,
                new GatewayOpenApiDefinitionDTO.Application(
                        "trade",
                        "orders",
                        "Orders",
                        "test",
                        "default"
                ),
                new GatewayOpenApiDefinitionDTO.Build(
                        "1.0.0",
                        "build-1",
                        Map.of()
                ),
                true,
                "set-" + groupCode,
                "f".repeat(64),
                List.of(business)
        );
    }
}
