package top.egon.cola.component.yuheng.test.live;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.yuheng.engine.rule.service.ApiRpcGatewayRuleCompilerStrategy;
import top.egon.cola.component.yuheng.engine.bootstrap.config.GatewayEngineConfiguration;
import top.egon.cola.component.yuheng.mcp.engine.rule.service.McpGatewayRuleCompilerStrategy;
import top.egon.cola.component.yuheng.mcp.engine.bootstrap.config.McpGatewayEngineConfiguration;
import top.egon.cola.component.yuheng.runtime.provider.service.ProviderDirectory;
import top.egon.cola.component.yuheng.runtime.rule.domain.GatewayCompiledRulesDTO;
import top.egon.cola.component.yuheng.runtime.rule.repository.GatewayRuleChunkStore;
import top.egon.cola.component.yuheng.runtime.rule.repository.GatewayRuleLkgRepository;
import top.egon.cola.component.yuheng.runtime.rule.service.GatewayRuleActivationApplier;
import top.egon.cola.component.yuheng.runtime.rule.service.GatewayRuleCompilerStrategy;
import top.egon.cola.component.yuheng.admin.rule.service.GatewayRuleCanonicalizer;
import top.egon.cola.component.yuheng.admin.rule.service.GatewayRuleCompiler;
import top.egon.cola.component.yuheng.contract.protocol.AccessZone;
import top.egon.cola.component.yuheng.contract.protocol.GatewayProtocol;
import top.egon.cola.component.yuheng.contract.rule.GatewayRequestBodyMode;
import top.egon.cola.component.yuheng.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.yuheng.contract.rule.GatewayRouteProfile;
import top.egon.cola.component.yuheng.contract.rule.GatewayRouteTransportPolicy;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuleContent;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimePolicy;
import top.egon.cola.component.yuheng.contract.rule.GatewayRuntimeRoute;
import top.egon.cola.component.yuheng.contract.rule.GatewayTransportProtocol;
import top.egon.cola.component.yuheng.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.yuheng.runtime.rule.adapter.json.GatewayRuleJsonCodec;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GatewayRuleWireCompatibilityTest {

    @TempDir
    Path dataDirectory;

    @Test
    void bothRolesActivateOneArtifactButKeepFailureAndLkgStateIndependent() {
        var content = new GatewayRuleContent("group-1", "default", "test", "yuheng-live",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        var publisher = new GatewayRuleCompiler(new GatewayRuleCanonicalizer());
        var first = publisher.compile("release-1", Instant.parse("2026-09-05T00:00:00Z"), content);
        var second = publisher.compile("release-2", Instant.parse("2026-09-05T00:01:00Z"), content);
        var api = applier("api", new ApiRpcGatewayRuleCompilerStrategy());
        var mcpCompiler = new McpGatewayRuleCompilerStrategy();
        var mcp = applier("mcp", snapshot -> {
            if ("release-2".equals(snapshot.releaseId())) {
                throw new IllegalStateException("test-only MCP compiler failure");
            }
            return mcpCompiler.compile(snapshot);
        });
        api.apply(GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY, first.activationJson(), 41);
        mcp.apply(GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY, first.activationJson(), 41);
        var apiMetadata = new GatewayEngineConfiguration().gatewayRuntimeMetadata(api).metadata();
        var mcpMetadata = new McpGatewayEngineConfiguration().gatewayRuntimeMetadata(mcp).metadata();
        assertEquals("API_RPC", apiMetadata.get("yuheng.engine.role"));
        assertEquals("MCP", mcpMetadata.get("yuheng.engine.role"));
        for (String key : List.of("activeReleaseId", "activeRuleVersion", "activeRuleChecksum")) {
            assertEquals(apiMetadata.get(key), mcpMetadata.get(key));
        }
        assertEquals(first.snapshot().artifactSha256(), api.active().ruleChecksum());
        var previousMcp = mcp.active();
        api.apply(GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY, second.activationJson(), 42);
        assertThrows(IllegalStateException.class, () ->
                mcp.apply(GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY, second.activationJson(), 42));
        assertEquals("release-2", api.active().releaseId());
        assertSame(previousMcp, mcp.active());
        assertEquals(41, mcp.status().activeDdcVersion());
        assertTrue(mcp.status().ready());
        assertEquals("FAILED", mcp.status().lastStage().name());
        assertFalse(mcp.status().degraded());
        var restoredApi = applier("api", new ApiRpcGatewayRuleCompilerStrategy());
        var restoredMcp = applier("mcp", new McpGatewayRuleCompilerStrategy());
        restoredApi.restoreLkg();
        restoredMcp.restoreLkg();
        assertEquals("release-2", restoredApi.active().releaseId());
        assertEquals("release-1", restoredMcp.active().releaseId());
        assertEquals(0, restoredMcp.status().activeDdcVersion());
        assertTrue(restoredMcp.status().ready());
        assertTrue(restoredMcp.status().degraded());
        restoredApi.apply(GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY, second.activationJson(), 42);
        restoredMcp.apply(GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY, second.activationJson(), 42);
        assertEquals(restoredApi.active().ruleChecksum(), restoredMcp.active().ruleChecksum());
        assertEquals(restoredApi.status().activeDdcVersion(), restoredMcp.status().activeDdcVersion());
        assertEquals(42, restoredMcp.status().activeDdcVersion());
    }

    private <T extends GatewayCompiledRulesDTO> GatewayRuleActivationApplier<T> applier(
            String roleDirectory, GatewayRuleCompilerStrategy<T> compiler) {
        return new GatewayRuleActivationApplier<>(new GatewayRuleJsonCodec(), compiler, new GatewayRuleChunkStore(),
                mock(ProviderDirectory.class), new GatewayRuleLkgRepository(dataDirectory.resolve(roleDirectory), "default"),
                Clock.systemUTC());
    }

    @Test
    void engineVerifiesAdminSnapshotWithMultiZoneRouteAndTrafficPolicy() {
        GatewayRuntimePolicy rateLimit = new GatewayRuntimePolicy(
                "live-http-rate",
                "RATE_LIMIT",
                "OPERATION",
                Map.of(
                        "operationIds", List.of("orders"),
                        "keyExpression", "${operationId}",
                        "capacity", 1,
                        "initialTokens", 1,
                        "refillTokens", 1,
                        "refillPeriod", "PT1H",
                        "mode", "DISTRIBUTED"
                )
        );
        GatewayRuntimeOperation operation = new GatewayRuntimeOperation(
                "orders",
                "GET /api/orders/{id}",
                GatewayProtocol.HTTP,
                "GET /api/orders/{id}",
                "{}",
                "{}",
                true,
                new GatewayProviderServiceRef(
                        "test-biz",
                        "test-app",
                        "test",
                        "yuheng-live",
                        GatewayProtocol.HTTP,
                        "orders-http-provider",
                        "default",
                        "1.0.0-live",
                        "http"
                ),
                "TRANSPARENT",
                Set.of(rateLimit.policyId()),
                Map.of("framework", "mvc"),
                false
        );
        GatewayRuntimeRoute route = new GatewayRuntimeRoute(
                "live-http-order",
                operation.operationId(),
                "api.yuheng.test",
                "GET",
                "/api/orders/{id}",
                Set.of(AccessZone.PUBLIC, AccessZone.INTERNAL),
                0,
                true
        );
        GatewayRuleContent content = new GatewayRuleContent(
                "group-1",
                "default",
                "test",
                "yuheng-live",
                List.of(operation),
                List.of(route),
                List.of(),
                List.of(rateLimit),
                List.of(),
                List.of(),
                List.of()
        );
        var release = new GatewayRuleCompiler(
                new GatewayRuleCanonicalizer()
        ).compile(
                "release-1",
                Instant.parse("2026-07-27T00:00:00Z"),
                content
        );
        GatewayRuleJsonCodec engineCodec = new GatewayRuleJsonCodec();
        var engineSnapshot = engineCodec.readSnapshot(
                release.snapshotJson().getBytes(StandardCharsets.UTF_8)
        );

        assertNull(engineSnapshot.content().routes().getFirst()
                .transportPolicy());
        assertEquals(
                "6c7dd1dd00823a68b978f1d1c696a013ca34af9922b246e24a0d20ffa4092518",
                engineSnapshot.ruleContentSha256()
        );
        engineCodec.verify(engineSnapshot);
    }

    @Test
    void engineVerifiesAdminSnapshotWithOpenAiTransportPolicy() {
        GatewayRouteTransportPolicy transport =
                new GatewayRouteTransportPolicy(
                        GatewayRouteProfile.OPENAI_HTTP,
                        GatewayTransportProtocol.HTTP,
                        GatewayRequestBodyMode.STREAMING,
                        GatewayTransportResponseMode.SSE,
                        536_870_912L,
                        10_000L,
                        120_000L,
                        90_000L,
                        1_800_000L,
                        null,
                        null,
                        false,
                        false
                );
        GatewayRuntimeOperation operation = new GatewayRuntimeOperation(
                "openai-responses",
                "POST /v1/responses",
                GatewayProtocol.HTTP,
                "POST /v1/responses",
                "{}",
                "{}",
                true,
                new GatewayProviderServiceRef(
                        "test-biz",
                        "test-app",
                        "test",
                        "yuheng-live",
                        GatewayProtocol.HTTP,
                        "openai-compatible-provider",
                        "default",
                        "v1",
                        "https"
                ),
                "TRANSPARENT",
                Set.of(),
                Map.of("framework", "webflux"),
                false
        );
        GatewayRuntimeRoute route = new GatewayRuntimeRoute(
                "openai-responses",
                operation.operationId(),
                "api.openai.example",
                "POST",
                "/v1/responses",
                Set.of(AccessZone.PUBLIC),
                0,
                true,
                transport
        );
        GatewayRuleContent content = new GatewayRuleContent(
                "group-openai",
                "default",
                "test",
                "yuheng-live",
                List.of(operation),
                List.of(route),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        var release = new GatewayRuleCompiler(
                new GatewayRuleCanonicalizer()
        ).compile(
                "release-openai",
                Instant.parse("2026-07-30T00:00:00Z"),
                content
        );
        GatewayRuleJsonCodec engineCodec = new GatewayRuleJsonCodec();
        var engineSnapshot = engineCodec.readSnapshot(
                release.snapshotJson().getBytes(StandardCharsets.UTF_8)
        );

        assertEquals(
                transport,
                engineSnapshot.content().routes().getFirst()
                        .transportPolicy()
        );
        engineCodec.verify(engineSnapshot);
    }
}
