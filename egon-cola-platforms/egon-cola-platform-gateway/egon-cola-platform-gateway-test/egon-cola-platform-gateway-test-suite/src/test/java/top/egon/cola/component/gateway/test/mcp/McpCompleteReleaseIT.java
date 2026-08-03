package top.egon.cola.component.gateway.test.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.mcp.rule.CompiledMcpRules;
import top.egon.cola.component.gateway.mcp.rule.McpRuleCompiler;

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
