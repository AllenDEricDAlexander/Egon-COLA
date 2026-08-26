package top.egon.cola.component.gateway.starter.discovery;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRiskLevel;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.discovery.mcp.McpExposureMapper;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpExposureMapperTest {

    @Test
    void emitsNormalizedRpcExposureWhenExplicitlyEnabled() throws Exception {
        Map<String, Object> exposure = map(Methods.class, "valid", false);

        assertThat(exposure).isEqualTo(Map.of(
                "registerMcp", true,
                "mcpServerCode", "trade-mcp",
                "mcpName", "order_get",
                "requiredPermissions", List.of("order:read", "tenant:read"),
                "riskLevel", "HIGH",
                "idempotent", true
        ));
    }

    @Test
    void omitsExposureWhenOperationDoesNotOptIn() throws Exception {
        assertThat(map(Methods.class, "internal", false)).isEmpty();
    }

    @Test
    void requiresServerNameAndValidPermissions() {
        assertThatThrownBy(() -> map(
                MissingServerMethods.class,
                "missingServer",
                false
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mcpServerCode is required");
        assertThatThrownBy(() -> map(Methods.class, "missingName", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mcpName is required");
        assertThatThrownBy(() -> map(
                Methods.class,
                "invalidPermission",
                false
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid MCP permission");
    }

    @Test
    void rejectsStreamingRpcExposure() {
        assertThatThrownBy(() -> map(Methods.class, "valid", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("streaming operations are unsupported");
    }

    private Map<String, Object> map(
            Class<?> groupType,
            String methodName,
            boolean streaming) {
        try {
            Method method = groupType.getDeclaredMethod(methodName);
            return McpExposureMapper.map(
                    groupType.getAnnotation(
                            GatewayInterfaceGroup.class
                    ),
                    method.getAnnotation(GatewayOperation.class),
                    methodName,
                    streaming
            );
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    @GatewayInterfaceGroup(
            businessDomainCode = "trade",
            businessDomainName = "Trade",
            entityDomainCode = "order",
            entityDomainName = "Order",
            code = "rpc-orders",
            name = "RPC Orders",
            mcpServerCode = " trade-mcp "
    )
    private static final class Methods {

        @GatewayOperation(
                idempotent = true,
                registerMcp = true,
                mcpName = " order_get ",
                mcpRequiredPermissions = {
                        "tenant:read",
                        " order:read ",
                        "order:read"
                },
                mcpRiskLevel = McpRiskLevel.HIGH
        )
        private static void valid() {
        }

        @GatewayOperation
        private static void internal() {
        }

        @GatewayOperation(registerMcp = true)
        private static void missingServer() {
        }

        @GatewayOperation(registerMcp = true, mcpName = "")
        private static void missingName() {
        }

        @GatewayOperation(
                registerMcp = true,
                mcpName = "invalid_permission",
                mcpRequiredPermissions = {"Order Read"}
        )
        private static void invalidPermission() {
        }
    }

    @GatewayInterfaceGroup(
            businessDomainCode = "trade",
            businessDomainName = "Trade",
            entityDomainCode = "order",
            entityDomainName = "Order",
            code = "rpc-orders-missing-server",
            name = "RPC Orders"
    )
    private static final class MissingServerMethods {

        @GatewayOperation(registerMcp = true, mcpName = "valid")
        private static void missingServer() {
        }
    }
}
