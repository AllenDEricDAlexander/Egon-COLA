package top.egon.cola.component.gateway.openapi.customizer;

import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.method.HandlerMethod;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRiskLevel;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.component.gateway.openapi.annotation.EgonMcpTool;
import top.egon.cola.component.gateway.openapi.config.GatewayOpenApiProperties;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EgonOperationCustomizerTest {

    @Test
    void mapsGovernanceAnnotationsToVersionedOptionalExtensions() throws Exception {
        GatewayOpenApiProperties properties = new GatewayOpenApiProperties();
        properties.setPublishedGroups(List.of("orders"));
        EgonOperationCustomizer customizer =
                new EgonOperationCustomizer(properties);
        Method method = Fixture.class.getDeclaredMethod("create");
        var operation = new io.swagger.v3.oas.models.Operation()
                .operationId("orders.create");

        customizer.customize(
                operation,
                new HandlerMethod(new Fixture(), method)
        );

        Map<String, Object> extension = (Map<String, Object>) operation.getExtensions()
                .get("x-egon");
        assertThat(extension).containsEntry("version", 1);
        assertThat(extension).containsKey("catalog");
        assertThat(extension).containsKey("policy");
        assertThat(extension).containsKey("mcp");
        Map<String, Object> mcp = (Map<String, Object>) extension.get("mcp");
        assertThat(mcp).containsEntry("name", "orders.create");
        assertThat(mcp).containsEntry("serverCode", "orders");
        assertThat(mcp).containsEntry("riskLevel", McpRiskLevel.MEDIUM.name());
        assertThat(mcp.get("permissions")).isEqualTo(List.of("orders:write"));
    }

    @EgonApiCatalog(
            businessDomainCode = "trade",
            entityDomainCode = "order",
            interfaceGroupCode = "orders"
    )
    static final class Fixture {

        @PostMapping("/orders")
        @Operation(operationId = "orders.create")
        @EgonGatewayPolicy(
                owner = "trade-team",
                exposure = EgonGatewayPolicy.Exposure.EXTERNAL,
                idempotency = EgonGatewayPolicy.Idempotency.FALSE
        )
        @EgonMcpTool(
                enabled = true,
                serverCode = "orders",
                permissions = "orders:write",
                riskLevel = McpRiskLevel.MEDIUM
        )
        void create() {
        }
    }
}
