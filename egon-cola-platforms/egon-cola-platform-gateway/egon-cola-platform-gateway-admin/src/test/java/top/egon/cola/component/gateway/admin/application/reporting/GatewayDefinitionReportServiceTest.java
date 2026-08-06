package top.egon.cola.component.gateway.admin.application.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.admin.application.IdempotencyStore;
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
                        mock(GatewayDefinitionReportStore.class),
                        mock(IdempotencyStore.class),
                        new ObjectMapper()
                );
        GatewayInterfaceDefinitionReport report = report(
                new GatewayInterfaceDefinitionReport.Parameter(
                        "tenant",
                        "HEADER",
                        true,
                        String.class.getName(),
                        Map.of("type", "string"),
                        null,
                        Map.of(),
                        null
                )
        );

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
                "v1"
                )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid mcpExposure")
                .hasMessageContaining("required HEADER parameter")
                .hasMessageContaining("tenant");
    }

    private GatewayInterfaceDefinitionReport report(
            GatewayInterfaceDefinitionReport.Parameter parameter) {
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
                List.of(parameter),
                Map.of("type", "object"),
                Map.of("type", "object"),
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
                "v1",
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
