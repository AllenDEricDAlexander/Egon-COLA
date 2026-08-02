package top.egon.cola.component.gateway.test.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
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
        Set<String> operations = Arrays.stream(
                        McpJobController.class.getDeclaredMethods()
                )
                .filter(method -> AnnotatedElementUtils.hasAnnotation(
                        method,
                        GatewayOperation.class
                ))
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        assertEquals("HTTP", McpRemoteFixtureCatalog.httpOperation()
                .protocol());
        assertEquals("RPC", McpRemoteFixtureCatalog.rpcOperation()
                .protocol());
        assertTrue(operations.containsAll(Set.of(
                "echo",
                "query",
                "write",
                "highRisk",
                "startJob",
                "cancelJob"
        )));
        assertTrue(McpRemoteFixtureCatalog.stable().tools()
                .contains("remote_echo"));
        assertTrue(McpRemoteFixtureCatalog.rc().apps()
                .contains("remote_dashboard"));
    }
}
