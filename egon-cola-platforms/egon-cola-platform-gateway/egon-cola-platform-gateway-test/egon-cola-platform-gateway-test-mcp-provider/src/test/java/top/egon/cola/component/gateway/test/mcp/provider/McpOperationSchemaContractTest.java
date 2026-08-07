package top.egon.cola.component.gateway.test.mcp.provider;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaShape;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class McpOperationSchemaContractTest {

    @Test
    void everyManagedOperationDeclaresCompleteRequestAndResponseRoots() {
        Arrays.stream(McpJobController.class.getDeclaredMethods())
                .map(method -> method.getAnnotation(GatewayOperation.class))
                .filter(java.util.Objects::nonNull)
                .filter(GatewayOperation::registerMcp)
                .forEach(operation -> {
                    assertThat(operation.requestSchemaFields())
                            .as(operation.mcpName() + " request")
                            .isNotEmpty();
                    assertThat(operation.responseSchema().schema())
                            .as(operation.mcpName() + " response")
                            .isNotEqualTo(Void.class);
                    assertThat(operation.responseSchema().shape())
                            .isNotEqualTo(GatewaySchemaShape.AUTO);
                });
    }
}
