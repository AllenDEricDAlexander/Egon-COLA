package top.egon.cola.component.gateway.admin.mcp.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.application.catalog.GatewayCatalogStore;
import top.egon.cola.component.gateway.admin.domain.AdminActor;
import top.egon.cola.component.gateway.admin.infrastructure.persistence.GatewayDraftRepository;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpArtifactMetadataStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpCapabilityDraftStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpManagedToolOverrideStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteProviderStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.JdbcMcpRemoteToolDraftStore;
import top.egon.cola.component.gateway.admin.mcp.persistence.McpServerEntity;
import top.egon.cola.component.gateway.admin.mcp.persistence.McpServerRepository;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpReleaseContentFactoryTest {

    private static final Instant NOW = Instant.parse("2026-08-06T00:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private McpServerRepository servers;

    private JdbcMcpManagedToolOverrideStore overrides;

    private GatewayCatalogStore catalog;

    private McpReleaseContentFactory factory;

    @BeforeEach
    void setUp() {
        servers = mock(McpServerRepository.class);
        overrides = mock(JdbcMcpManagedToolOverrideStore.class);
        catalog = mock(GatewayCatalogStore.class);
        factory = new McpReleaseContentFactory(
                servers,
                mock(JdbcMcpCapabilityDraftStore.class),
                overrides,
                mock(JdbcMcpRemoteToolDraftStore.class),
                mock(JdbcMcpRemoteProviderStore.class),
                mock(JdbcMcpArtifactMetadataStore.class),
                mock(GatewayDraftRepository.class),
                catalog,
                mock(McpValidationService.class),
                objectMapper
        );
        when(servers
                .findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(
                        "group-1"
                )).thenReturn(List.of(server(
                "server-1",
                "orders"
        )));
        when(overrides.load("group-1")).thenReturn(List.of());
    }

    @Test
    void projectsStableHttpToolWithPathQueryAndBodyInputs() throws Exception {
        when(catalog.loadCurrentOperationDefinitions("group-1"))
                .thenReturn(List.of(current(
                        "operation-1",
                        "http:orders:GET:/orders/{id}",
                        "HTTP",
                        Map.of(
                                "description", "Get one order",
                                "streaming", false,
                                "parameters", List.of(
                                        parameter(
                                                "id",
                                                "PATH",
                                                true,
                                                Map.of("type", "string")
                                        ),
                                        parameter(
                                                "verbose",
                                                "QUERY",
                                                false,
                                                Map.of("type", "boolean")
                                        ),
                                        parameter(
                                                "request",
                                                "BODY",
                                                true,
                                                Map.of(
                                                        "type", "object",
                                                        "properties", Map.of(
                                                                "note",
                                                                Map.of(
                                                                        "type",
                                                                        "string"
                                                                )
                                                        )
                                                )
                                        )
                                ),
                                "mcpExposure", exposure(
                                        "orders",
                                        "orders.get",
                                        "LOW"
                                )
                        ),
                        Map.of("type", "object"),
                        Map.of("type", "object", "title", "Order")
                )));

        McpRuntimeTool first = factory.managedTools("group-1")
                .getFirst().tool();
        McpRuntimeTool second = factory.managedTools("group-1")
                .getFirst().tool();

        assertThat(first.toolId()).isEqualTo(
                "59da10910192faf61cf5c56d05d572c4440858ded75f4350b65807e873c55d21"
        );
        assertThat(second.toolId()).isEqualTo(first.toolId());
        assertThat(first.operationProtocol()).isEqualTo("HTTP");
        assertThat(first.inputLocations()).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        "id", "PATH",
                        "verbose", "QUERY",
                        "request", "BODY"
                )
        );
        Map<String, Object> schema = objectMapper.readValue(
                first.inputSchema(),
                new TypeReference<>() {
                }
        );
        assertThat(schema.get("type")).isEqualTo("object");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties =
                (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schema.get("required");
        assertThat(properties)
                .containsKeys("id", "verbose", "request");
        assertThat(required)
                .containsExactly("id", "request");
        assertThat(first.outputSchema()).contains("Order");
        assertThat(first.description()).isEqualTo("Get one order");
    }

    @Test
    void rpcUsesWholeRequestSchemaAndHasNoInputLocations() throws Exception {
        Map<String, Object> requestSchema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "orderId", Map.of("type", "string")
                )
        );
        when(catalog.loadCurrentOperationDefinitions("group-1"))
                .thenReturn(List.of(current(
                        "operation-rpc",
                        "rpc:orders:get",
                        "RPC",
                        Map.of("mcpExposure", exposure(
                                "orders",
                                "orders.rpc.get",
                                "MEDIUM"
                        )),
                        requestSchema,
                        Map.of("type", "object")
                )));

        McpRuntimeTool tool = factory.managedTools("group-1")
                .getFirst().tool();

        assertThat(tool.operationProtocol()).isEqualTo("RPC");
        assertThat(tool.inputLocations()).isEmpty();
        assertThat(objectMapper.readValue(
                tool.inputSchema(),
                new TypeReference<Map<String, Object>>() {
                }
        )).isEqualTo(requestSchema);
    }

    @Test
    void excludesOptionalHeadersCookiesAndInjectedAuthorization() {
        when(catalog.loadCurrentOperationDefinitions("group-1"))
                .thenReturn(List.of(current(
                        "operation-1",
                        "http:orders:GET:/orders",
                        "HTTP",
                        Map.of(
                                "parameters", List.of(
                                        parameter(
                                                "filter",
                                                "QUERY",
                                                false,
                                                Map.of("type", "string")
                                        ),
                                        parameter(
                                                "X-Trace",
                                                "HEADER",
                                                false,
                                                Map.of("type", "string")
                                        ),
                                        parameter(
                                                "Authorization",
                                                "HEADER",
                                                true,
                                                Map.of("type", "string")
                                        ),
                                        parameter(
                                                "SESSION",
                                                "COOKIE",
                                                false,
                                                Map.of("type", "string")
                                        )
                                ),
                                "mcpExposure", exposure(
                                        "orders",
                                        "orders.list",
                                        "LOW"
                                )
                        ),
                        Map.of("type", "object"),
                        Map.of("type", "object")
                )));

        McpRuntimeTool tool = factory.managedTools("group-1")
                .getFirst().tool();

        assertThat(tool.inputLocations()).containsOnlyKeys("filter");
        assertThat(tool.inputSchema()).contains("filter")
                .doesNotContain("X-Trace", "Authorization", "SESSION");
    }

    @Test
    void rejectsRequiredModelControlledHeader() {
        when(catalog.loadCurrentOperationDefinitions("group-1"))
                .thenReturn(List.of(current(
                        "operation-1",
                        "http:orders:GET:/orders",
                        "HTTP",
                        Map.of(
                                "parameters", List.of(parameter(
                                        "X-Tenant",
                                        "HEADER",
                                        true,
                                        Map.of("type", "string")
                                )),
                                "mcpExposure", exposure(
                                        "orders",
                                        "orders.list",
                                        "LOW"
                                )
                        ),
                        Map.of("type", "object"),
                        Map.of("type", "object")
                )));

        assertThatThrownBy(() -> factory.managedTools("group-1"))
                .isInstanceOf(McpValidationException.class)
                .hasMessageContaining("X-Tenant")
                .hasMessageContaining("HEADER");
    }

    @Test
    void ignoresForgedExposureOnManualOperation() {
        GatewayCatalogStore.CurrentOperationDefinition starter = current(
                "operation-1",
                "http:orders:GET:/orders",
                "HTTP",
                Map.of("mcpExposure", exposure(
                        "orders",
                        "orders.list",
                        "LOW"
                )),
                Map.of("type", "object"),
                Map.of("type", "object")
        );
        GatewayCatalogStore.OperationRecord manualOperation =
                new GatewayCatalogStore.OperationRecord(
                        starter.operation().id(),
                        starter.operation().applicationId(),
                        starter.operation().interfaceGroupId(),
                        starter.operation().operationKey(),
                        starter.operation().protocol(),
                        starter.operation().methodIdentity(),
                        starter.operation().externalAccessible(),
                        starter.operation().providerServiceIdentity(),
                        "MANUAL",
                        starter.operation().lifecycleStatus(),
                        starter.operation().currentDefinitionId(),
                        starter.operation().revision(),
                        starter.operation().createdAt(),
                        starter.operation().updatedAt()
                );
        when(catalog.loadCurrentOperationDefinitions("group-1"))
                .thenReturn(List.of(
                        new GatewayCatalogStore.CurrentOperationDefinition(
                                manualOperation,
                                starter.definition()
                        )
                ));

        assertThat(factory.managedTools("group-1")).isEmpty();
    }

    @Test
    void overrideCanOnlyTightenPermissionsRiskAndEnabledState() {
        McpServerEntity codeServer = server("server-1", "orders");
        McpServerEntity strictServer = server("server-2", "restricted");
        when(servers
                .findAllByGatewayGroupIdAndDeletedFalseOrderByServerCode(
                        "group-1"
                )).thenReturn(List.of(codeServer, strictServer));
        var current = current(
                "operation-1",
                "http:orders:GET:/orders/{id}",
                "HTTP",
                Map.of(
                        "parameters", List.of(),
                        "mcpExposure", Map.of(
                                "registerMcp", true,
                                "mcpServerCode", "orders",
                                "mcpName", "orders.get",
                                "requiredPermissions", List.of(
                                        "mcp:orders:read"
                                ),
                                "riskLevel", "MEDIUM",
                                "idempotent", true
                        )
                ),
                Map.of("type", "object"),
                Map.of("type", "object")
        );
        String toolId = McpReleaseContentFactory.managedToolId(
                "orders",
                current.operation().operationKey()
        );
        when(catalog.loadCurrentOperationDefinitions("group-1"))
                .thenReturn(List.of(current));
        when(overrides.load("group-1")).thenReturn(List.of(
                new JdbcMcpManagedToolOverrideStore.ManagedToolOverride(
                        toolId,
                        "group-1",
                        "operation-1",
                        "server-2",
                        Set.of("mcp:orders:admin"),
                        "HIGH",
                        false,
                        3
                )
        ));

        var projection = factory.managedTools("group-1").getFirst();
        McpRuntimeTool tool = projection.tool();

        assertThat(tool.toolId()).isEqualTo(toolId);
        assertThat(tool.serverCode()).isEqualTo("restricted");
        assertThat(tool.requiredPermissions()).containsExactlyInAnyOrder(
                "mcp:orders:read",
                "mcp:orders:admin"
        );
        assertThat(tool.riskLevel()).isEqualTo("HIGH");
        assertThat(tool.enabled()).isFalse();
        assertThat(projection.overrideRevision()).isEqualTo(3);
    }

    private GatewayCatalogStore.CurrentOperationDefinition current(
            String operationId,
            String operationKey,
            String protocol,
            Map<String, Object> attributes,
            Map<String, Object> requestSchema,
            Map<String, Object> responseSchema) {
        var operation = new GatewayCatalogStore.OperationRecord(
                operationId,
                "app-1",
                "interface-1",
                operationKey,
                protocol,
                operationKey,
                false,
                Map.of(),
                "STARTER",
                "ACTIVE",
                "definition-1",
                0,
                NOW,
                NOW
        );
        var definition = new GatewayCatalogStore.OperationDefinition(
                "definition-1",
                operationId,
                1,
                "a".repeat(64),
                "Summary",
                List.of(),
                requestSchema,
                responseSchema,
                List.of(),
                null,
                attributes,
                false,
                NOW,
                "STARTER"
        );
        return new GatewayCatalogStore.CurrentOperationDefinition(
                operation,
                definition
        );
    }

    private Map<String, Object> exposure(
            String serverCode,
            String name,
            String risk) {
        return Map.of(
                "registerMcp", true,
                "mcpServerCode", serverCode,
                "mcpName", name,
                "requiredPermissions", List.of(),
                "riskLevel", risk,
                "idempotent", true
        );
    }

    private Map<String, Object> parameter(
            String name,
            String location,
            boolean required,
            Map<String, Object> schema) {
        return Map.of(
                "name", name,
                "location", location,
                "required", required,
                "schema", schema
        );
    }

    private McpServerEntity server(String id, String code) {
        return new McpServerEntity(
                id,
                "group-1",
                code,
                code,
                null,
                null,
                Set.of("STABLE_2025_11_25"),
                "gateway-mcp",
                30,
                new AdminActor(
                        "admin",
                        AdminActor.ActorType.USER,
                        Set.of(),
                        Set.of()
                ),
                NOW
        );
    }
}
