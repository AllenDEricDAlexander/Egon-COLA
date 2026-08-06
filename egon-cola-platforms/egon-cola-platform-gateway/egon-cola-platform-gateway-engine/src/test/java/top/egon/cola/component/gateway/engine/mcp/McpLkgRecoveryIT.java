package top.egon.cola.component.gateway.engine.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.gateway.contract.mcp.protocol.McpProtocolDialect;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuleContent;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeServer;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRuntimeTool;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivationMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderCatalogSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderQuery;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.provider.ProviderServiceRegistry;
import top.egon.cola.component.gateway.core.provider.ProviderServiceSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderSubscription;
import top.egon.cola.component.gateway.engine.discovery.ProviderDirectory;
import top.egon.cola.component.gateway.engine.rule.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.rule.EngineGatewayRuleCompiler;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleActivationApplier;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleChunkStore;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleJsonCodec;
import top.egon.cola.component.gateway.engine.rule.GatewayRuleLkgRepository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpLkgRecoveryIT {

    @TempDir
    Path dataDirectory;

    @Test
    void failedReleaseKeepsActiveMcpAndRestartRestoresSameLkg() {
        GatewayRuleActivationApplier running = applier();
        TestRelease valid = release("release-1", validMcp());
        running.apply(
                GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY,
                valid.activationJson(),
                1L
        );
        CompiledGatewayRules before = running.active();

        TestRelease invalid = release("release-2", invalidMcp());
        assertThrows(IllegalArgumentException.class, () -> running.apply(
                GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY,
                invalid.activationJson(),
                2L
        ));

        assertSame(before, running.active());
        assertEquals("release-1", running.active().snapshot().releaseId());
        assertTrue(running.active().mcpRules().server("developer").isPresent());

        GatewayRuleActivationApplier restarted = applier();
        assertTrue(restarted.restoreLkg());
        assertEquals("release-1", restarted.active().snapshot().releaseId());
        assertTrue(restarted.active().mcpRules()
                .server("developer")
                .isPresent());
        assertTrue(restarted.status().degraded());
    }

    private GatewayRuleActivationApplier applier() {
        Clock clock = Clock.systemUTC();
        return new GatewayRuleActivationApplier(
                new GatewayRuleJsonCodec(),
                new EngineGatewayRuleCompiler(),
                new GatewayRuleChunkStore(),
                new ProviderDirectory(new EmptyRegistry(), clock),
                new GatewayRuleLkgRepository(dataDirectory, "developer"),
                clock
        );
    }

    private TestRelease release(String releaseId, McpRuleContent mcp) {
        GatewayRuleContent content = new GatewayRuleContent(
                "group-1",
                "developer",
                "local",
                "default",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                mcp
        );
        GatewayRuleJsonCodec codec = new GatewayRuleJsonCodec();
        Instant generatedAt = Instant.parse("2026-08-02T00:00:00Z");
        String contentSha = GatewayRuleJsonCodec.sha256(codec.write(content));
        String artifactSha = GatewayRuleJsonCodec.sha256(codec.write(Map.of(
                "content", content,
                "generatedAt", generatedAt,
                "releaseId", releaseId,
                "ruleContentSha256", contentSha,
                "ruleSchemaVersion", "v1"
        )));
        GatewayRuleSnapshot snapshot = new GatewayRuleSnapshot(
                "v1",
                releaseId,
                generatedAt,
                contentSha,
                artifactSha,
                content
        );
        byte[] snapshotJson = codec.write(snapshot);
        GatewayRuleActivation activation = new GatewayRuleActivation(
                "v1",
                releaseId,
                GatewayRuleActivationMode.INLINE,
                "v1",
                snapshotJson.length,
                contentSha,
                artifactSha,
                new String(snapshotJson, StandardCharsets.UTF_8),
                List.of()
        );
        return new TestRelease(new String(
                codec.write(activation),
                StandardCharsets.UTF_8
        ));
    }

    private McpRuleContent validMcp() {
        return new McpRuleContent(
                List.of(server()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private McpRuleContent invalidMcp() {
        return new McpRuleContent(
                List.of(server()),
                List.of(new McpRuntimeTool(
                        "tool-1",
                        "missing-server",
                        "orders.get",
                        "Get an order",
                        "LOCAL_OPERATION",
                        "orders",
                        "HTTP",
                        null,
                        "{}",
                        "{}",
                        Map.of(),
                        Map.of(),
                        Set.of(),
                        "LOW",
                        true,
                        true
                )),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private McpRuntimeServer server() {
        return new McpRuntimeServer(
                "server-1",
                "developer",
                "Developer",
                "Developer capabilities",
                "Use reviewed tools.",
                Set.of(McpProtocolDialect.STABLE_2025_11_25),
                "gateway-mcp",
                30,
                true
        );
    }

    private record TestRelease(String activationJson) {
    }

    private static final class EmptyRegistry
            implements ProviderServiceRegistry {

        @Override
        public ProviderCatalogSnapshot getServiceKeys(ProviderQuery query) {
            return new ProviderCatalogSnapshot(1, Instant.now(), List.of());
        }

        @Override
        public ProviderServiceSnapshot getInstances(ProviderServiceKey key) {
            return new ProviderServiceSnapshot(
                    key,
                    1,
                    Instant.now(),
                    List.of()
            );
        }

        @Override
        public ProviderSubscription subscribeServices(
                ProviderQuery query,
                ProviderCatalogListener listener) {
            return subscription();
        }

        @Override
        public ProviderSubscription subscribe(
                ProviderServiceKey key,
                ProviderSnapshotListener listener) {
            return subscription();
        }

        private ProviderSubscription subscription() {
            return new ProviderSubscription() {
                @Override
                public boolean active() {
                    return true;
                }

                @Override
                public void close() {
                }
            };
        }
    }
}
