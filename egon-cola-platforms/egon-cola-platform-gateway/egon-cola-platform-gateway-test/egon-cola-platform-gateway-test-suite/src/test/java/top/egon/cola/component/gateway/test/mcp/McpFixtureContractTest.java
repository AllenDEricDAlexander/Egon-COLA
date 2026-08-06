package top.egon.cola.component.gateway.test.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRiskLevel;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.test.mcp.provider.McpJobController;
import top.egon.cola.component.gateway.test.mcp.remote.McpRemoteFixtureCatalog;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpFixtureContractTest {

    @Test
    void fixturesExposeHttpRpcJobStableRcAndAppCapabilities() {
        Set<GatewayOperation> operations = Arrays.stream(
                        McpJobController.class.getDeclaredMethods()
                )
                .map(method -> AnnotatedElementUtils.findMergedAnnotation(
                        method, GatewayOperation.class
                ))
                .filter(java.util.Objects::nonNull)
                .filter(GatewayOperation::registerMcp)
                .collect(Collectors.toSet());
        GatewayInterfaceGroup group = AnnotatedElementUtils.findMergedAnnotation(
                McpJobController.class,
                GatewayInterfaceGroup.class
        );

        assertEquals("unified-local", group.mcpServerCode());
        assertEquals("HTTP", McpRemoteFixtureCatalog.httpOperation()
                .protocol());
        assertEquals("RPC", McpRemoteFixtureCatalog.rpcOperation()
                .protocol());
        assertEquals(Set.of(
                "local_echo_task",
                "local_query",
                "high_risk_action"
        ), operations.stream().map(GatewayOperation::mcpName)
                .collect(Collectors.toSet()));
        assertTrue(operations.stream().anyMatch(operation ->
                "high_risk_action".equals(operation.mcpName())
                        && operation.mcpRiskLevel() == McpRiskLevel.HIGH
                        && Set.of(operation.mcpRequiredPermissions())
                        .equals(Set.of("mock:admin"))
        ));
        assertTrue(McpRemoteFixtureCatalog.stable().tools()
                .contains("remote_echo"));
        assertTrue(McpRemoteFixtureCatalog.rc().apps()
                .contains("remote_dashboard"));
    }
}
