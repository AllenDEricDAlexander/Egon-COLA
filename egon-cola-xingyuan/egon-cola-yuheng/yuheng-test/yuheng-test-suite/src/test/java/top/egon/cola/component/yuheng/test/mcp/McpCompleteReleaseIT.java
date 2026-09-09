package top.egon.cola.component.yuheng.test.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.yuheng.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.yuheng.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.yuheng.mcp.rule.domain.CompiledMcpRules;
import top.egon.cola.component.yuheng.mcp.rule.service.McpRuleCompiler;
import top.egon.cola.component.yuheng.mcp.engine.rule.service.McpGatewayRuleCompilerStrategy;
import top.egon.cola.component.yuheng.admin.rule.service.GatewayRuleCanonicalizer;
import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;
import top.egon.cola.component.yuheng.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleContent;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimeOperation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Single release artifact covering every supported MCP capability family.
 */
class McpCompleteReleaseIT {

    @Test
    void completeReleaseCompilesLocalRemoteTaskAndAppCapabilities()
            throws Exception {
        McpRuleContent content;
        try (var input = getClass().getResourceAsStream(
                "/mcp/complete-release.json"
        )) {
            content = new ObjectMapper().readValue(
                    input,
                    McpRuleContent.class
            );
        }
        CompiledMcpRules rules = new McpRuleCompiler().compile(
                content,
                Set.of("operation-http", "operation-rpc")
        );
        var snapshot = new GatewayRuleCanonicalizer().snapshot("complete-release",
                Instant.parse("2026-09-05T00:00:00Z"),
                new GatewayRuleContent("group-1", "default", "test", "default",
                        List.of(operation("operation-http", GatewayProtocol.HTTP),
                                operation("operation-rpc", GatewayProtocol.RPC)),
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), content));
        var executableRules = new McpGatewayRuleCompilerStrategy().compile(snapshot);
        assertEquals(rules, executableRules.mcpRules());
        assertEquals(snapshot.artifactSha256(), executableRules.ruleChecksum());
        assertEquals(2, executableRules.providerServices().size());

        assertAll(
                () -> assertEquals(1, rules.serversByCode().size()),
                () -> assertEquals(4, rules.toolsByQualifiedName().size()),
                () -> assertEquals(2, rules.resourcesByQualifiedName().size()),
                () -> assertEquals(2, rules.templatesByQualifiedName().size()),
                () -> assertEquals(2, rules.promptsByQualifiedName().size()),
                () -> assertEquals(
                        1,
                        rules.taskPoliciesByQualifiedTool().size()
                ),
                () -> assertEquals(1, rules.appsByQualifiedName().size()),
                () -> assertEquals(2, rules.remoteProvidersByCode().size()),
                () -> assertEquals(2, rules.remoteMountsById().size()),
                () -> assertEquals(
                        "operation-http",
                        rules.tool("commerce", "http_query")
                                .orElseThrow()
                                .operationId()
                ),
                () -> assertEquals(
                        "operation-rpc",
                        rules.tool("commerce", "rpc_export")
                                .orElseThrow()
                                .operationId()
                ),
                () -> assertTrue(rules.remoteAvailable(
                        "mount-stable",
                        "TOOL"
                )),
                () -> assertTrue(rules.remoteAvailable(
                        "mount-rc",
                        "COMPLETION"
                )),
                () -> assertEquals(
                        Set.of(
                                McpProtocolDialect.LEGACY_2024_SSE,
                                McpProtocolDialect.STABLE_2025_11_25,
                                McpProtocolDialect.RC_2026_07_28
                        ),
                        rules.server("commerce").orElseThrow().dialects()
                )
        );
    }

    private GatewayRuntimeOperation operation(String id, GatewayProtocol protocol) {
        return new GatewayRuntimeOperation(id, id, protocol,
                protocol == GatewayProtocol.HTTP ? "POST /fixture" : "fixture.Service/Call",
                "{}", "{}", true,
                new GatewayProviderServiceRef("test", "fixture", "test", "default",
                        protocol, "fixture", "default", "v1", protocol == GatewayProtocol.HTTP ? "http" : "grpc"),
                "TRANSPARENT", Set.of(), Map.of(), false);
    }

    @Test
    void releaseRemoteContractsExecuteAgainstStableAndRcFixtures()
            throws Exception {
        try (RemoteMcpFixtureServer fixture =
                     RemoteMcpFixtureServer.start()) {
            StableMcpTestClient stable = new StableMcpTestClient(
                    fixture.stableEndpoint(),
                    null
            );
            RcMcpTestClient rc = new RcMcpTestClient(
                    fixture.rcEndpoint(),
                    null
            );

            assertEquals(
                    "2025-11-25",
                    McpStableConformanceIT.result(stable.initialize())
                            .get("protocolVersion")
            );
            assertEquals(
                    "stable-release",
                    McpStableConformanceIT.object(
                            McpStableConformanceIT.result(stable.call(
                                    "tools/call",
                                    Map.of(
                                            "name", "remote_echo",
                                            "arguments", Map.of(
                                                    "value",
                                                    "stable-release"
                                            )
                                    )
                            )).get("structuredContent")
                    ).get("value")
            );
            assertEquals(
                    "2026-07-28",
                    McpStableConformanceIT.result(rc.call(
                            "discover",
                            Map.of()
                    )).get("protocolVersion")
            );
            assertEquals(
                    "remote_dashboard",
                    ((Map<?, ?>) ((java.util.List<?>)
                            McpStableConformanceIT.result(rc.call(
                                    "apps/list",
                                    Map.of()
                            )).get("apps")).getFirst()).get("name")
            );
        }
    }
}
