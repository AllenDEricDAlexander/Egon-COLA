package top.egon.cola.component.gateway.test.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRiskLevel;
import top.egon.cola.component.gateway.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.gateway.openapi.annotation.EgonMcpTool;
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
        Set<EgonMcpTool> operations = Arrays.stream(
                        McpJobController.class.getDeclaredMethods()
                )
                .map(method -> AnnotatedElementUtils.findMergedAnnotation(
                        method, EgonMcpTool.class
                ))
                .filter(java.util.Objects::nonNull)
                .filter(EgonMcpTool::enabled)
                .collect(Collectors.toSet());
        EgonApiCatalog catalog = AnnotatedElementUtils.findMergedAnnotation(
                McpJobController.class,
                EgonApiCatalog.class
        );

        assertEquals("jobs", catalog.interfaceGroupCode());
        assertEquals(Set.of("unified-local"), operations.stream()
                .map(EgonMcpTool::serverCode)
                .collect(Collectors.toSet()));
        assertEquals("HTTP", McpRemoteFixtureCatalog.httpOperation()
                .protocol());
        assertEquals("OPENAPI31", McpRemoteFixtureCatalog.httpOperation()
                .sourceType());
        assertEquals("RPC", McpRemoteFixtureCatalog.rpcOperation()
                .protocol());
        assertEquals("RPC_DESCRIPTOR", McpRemoteFixtureCatalog.rpcOperation()
                .sourceType());
        assertEquals(Set.of(
                "local_echo_task",
                "local_query",
                "high_risk_action"
        ), operations.stream().map(EgonMcpTool::name)
                .collect(Collectors.toSet()));
        assertTrue(operations.stream().anyMatch(operation ->
                "high_risk_action".equals(operation.name())
                        && operation.riskLevel() == McpRiskLevel.HIGH
                        && Set.of(operation.permissions())
                        .equals(Set.of("mock:admin"))
        ));
        assertTrue(McpRemoteFixtureCatalog.stable().tools()
                .contains("remote_echo"));
        assertTrue(McpRemoteFixtureCatalog.rc().apps()
                .contains("remote_dashboard"));
    }
}
