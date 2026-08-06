package top.egon.cola.component.gateway.starter.discovery;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRiskLevel;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOperationSemanticsTest {

    @Test
    void derivesIdempotencyOnlyFromTheExplicitField()
            throws Exception {
        assertThat(GatewayOperationSemantics.idempotent(annotation("explicit")))
                .isTrue();
        assertThat(GatewayOperationSemantics.idempotent(annotation("tagged")))
                .isFalse();
        assertThat(GatewayOperationSemantics.idempotent(annotation("negative")))
                .isFalse();
        assertThat(GatewayOperationSemantics.idempotent(null)).isFalse();
    }

    @Test
    void preservesDeclaredTagsAndUsesAnImmutableEmptyDefault()
            throws Exception {
        GatewayOperation operation = annotation("explicit");

        assertThat(GatewayOperationSemantics.tags(operation))
                .containsExactly("rpc");
        assertThat(GatewayOperationSemantics.tags(null)).isEmpty();
    }

    @Test
    void keepsMcpExposureOptInWithLowRiskDefaults() throws Exception {
        GatewayOperation operation = annotation("defaulted");
        GatewayInterfaceGroup group = Samples.class.getAnnotation(
                GatewayInterfaceGroup.class
        );

        assertThat(operation.idempotent()).isFalse();
        assertThat(operation.registerMcp()).isFalse();
        assertThat(operation.mcpName()).isEmpty();
        assertThat(operation.mcpRequiredPermissions()).isEmpty();
        assertThat(operation.mcpRiskLevel()).isEqualTo(McpRiskLevel.LOW);
        assertThat(group.mcpServerCode()).isEmpty();
    }

    private GatewayOperation annotation(String methodName) throws Exception {
        Method method = Samples.class.getDeclaredMethod(methodName);
        return method.getAnnotation(GatewayOperation.class);
    }

    @GatewayInterfaceGroup(
            businessDomainCode = "trade",
            businessDomainName = "Trade",
            entityDomainCode = "order",
            entityDomainName = "Order",
            code = "orders",
            name = "Orders"
    )
    private static final class Samples {

        @GatewayOperation(idempotent = true, tags = {"rpc"})
        private void explicit() {
        }

        @GatewayOperation(tags = {"rpc", "idempotent"})
        private void tagged() {
        }

        @GatewayOperation(tags = {"rpc", "non-idempotent"})
        private void negative() {
        }

        @GatewayOperation
        private void defaulted() {
        }
    }
}
