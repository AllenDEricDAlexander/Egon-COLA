package top.egon.cola.component.gateway.admin.reporting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.shared.repository.IdempotencyRepository;
import top.egon.cola.component.gateway.admin.reporting.repository.GatewayDefinitionReportRepository;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class GatewayDefinitionReportServiceTest {

    @Test
    void rejectsReportedMcpExposureWithRequiredHeaderInput() {
        GatewayDefinitionReportService service =
                new GatewayDefinitionReportService(
                        mock(GatewayDefinitionReportRepository.class),
                        mock(IdempotencyRepository.class),
                        new ObjectMapper()
                );
        GatewayInterfaceDefinitionReport report = report();

        assertThatThrownBy(() -> service.accept(
                new GatewayReportAuthentication(
                        "app-1",
                        "trade",
                        "orders",
                        "test",
                        "default",
                        "access-key"
                ),
                report,
                "report-1",
                "v2"
                )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid mcpExposure")
                .hasMessageContaining("required HEADER parameter")
                .hasMessageContaining("tenant");
    }

    private GatewayInterfaceDefinitionReport report() {
        var provider = new GatewayInterfaceDefinitionReport.ProviderService(
                "trade",
                "orders",
                "test",
                "default",
                "HTTP",
                "orders",
                "default",
                "1.0.0",
                "HTTP"
        );
        var operation = new GatewayInterfaceDefinitionReport.Operation(
                "http:orders:GET:/orders",
                "HTTP",
                "GET /orders",
                "orders",
                "Orders",
                "List orders",
                "orders-team",
                List.of(),
                false,
                "SUPPORTED",
                provider,
                Map.of(
                        "$schema", "https://json-schema.org/draft/2020-12/schema",
                        "x-egon-schema-model", "gateway-operation-request/v2",
                        "type", "object",
                        "properties", Map.of(
                                "header", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "tenant", Map.of("type", "string")
                                        ),
                                        "required", List.of("tenant"),
                                        "additionalProperties", false
                                )
                        ),
                        "required", List.of("header"),
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
                Map.of("mcpExposure", Map.of(
                        "registerMcp", true,
                        "mcpServerCode", "orders",
                        "mcpName", "orders.list",
                        "requiredPermissions", List.of(
                                "mcp:orders:read"
                        ),
                        "riskLevel", "LOW",
                        "idempotent", true
                )),
                false
        );
        var group = new GatewayInterfaceDefinitionReport.InterfaceGroup(
                "orders",
                "Orders",
                null,
                "STARTER",
                "OrdersController",
                "HTTP",
                Map.of(),
                List.of(operation)
        );
        var entity = new GatewayInterfaceDefinitionReport.EntityDomain(
                "order",
                "Order",
                null,
                List.of(group)
        );
        var business = new GatewayInterfaceDefinitionReport.BusinessDomain(
                "trade",
                "Trade",
                null,
                List.of(entity)
        );
        return new GatewayInterfaceDefinitionReport(
                "v2",
                "report-1",
                Instant.parse("2026-08-06T00:00:00Z"),
                new GatewayInterfaceDefinitionReport.Application(
                        "trade",
                        "orders",
                        "Orders",
                        "test",
                        "default"
                ),
                new GatewayInterfaceDefinitionReport.Build(
                        "1.0.0",
                        "build-1",
                        Map.of()
                ),
                true,
                "definition-set-1",
                "fingerprint-1",
                List.of(business)
        );
    }
}
