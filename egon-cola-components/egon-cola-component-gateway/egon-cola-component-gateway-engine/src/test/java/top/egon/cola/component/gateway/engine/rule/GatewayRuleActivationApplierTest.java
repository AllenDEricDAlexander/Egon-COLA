package top.egon.cola.component.gateway.engine.rule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.contract.rule.GatewayProviderServiceRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivationMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleChunkRef;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeOperation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeRoute;
import top.egon.cola.component.gateway.core.provider.ProviderCatalogSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderQuery;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.provider.ProviderServiceRegistry;
import top.egon.cola.component.gateway.core.provider.ProviderServiceSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderSubscription;
import top.egon.cola.component.gateway.engine.discovery.ProviderDirectory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayRuleActivationApplierTest {

    private static final int INLINE_LIMIT_BYTES = 512 * 1024;

    private static final int CHUNK_LIMIT_BYTES = 256 * 1024;

    @TempDir
    Path dataDirectory;

    @Test
    void inlineApplyPersistsBeforeActivationAndInvalidNextRuleKeepsOld() {
        TestRelease release = release("release-1", "{}");
        GatewayRuleActivationApplier applier = applier();

        assertEquals(100, applier.priority());

        applier.apply(
                GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY,
                release.activationJson(),
                1
        );

        assertEquals("release-1", applier.active().snapshot().releaseId());
        assertEquals(
                GatewayRuleApplyStage.ACK_SUCCESS,
                applier.status().lastStage()
        );
        assertTrue(Files.exists(dataDirectory.resolve(
                "rules/orders/releases/release-1.json"
        )));
        GatewayRuleActivation invalid = new GatewayRuleActivation(
                release.activation().activationSchemaVersion(),
                release.activation().releaseId(),
                release.activation().mode(),
                release.activation().ruleSchemaVersion(),
                release.activation().totalSize(),
                release.activation().ruleContentSha256(),
                "bad-artifact-sha",
                release.activation().inlineSnapshot(),
                release.activation().chunks()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> applier.apply(
                        GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY,
                        new String(new GatewayRuleJsonCodec().write(invalid)),
                        2
                )
        );
        assertEquals("release-1", applier.active().snapshot().releaseId());
        assertEquals(GatewayRuleApplyStage.FAILED,
                applier.status().lastStage());
    }

    @Test
    void chunkedApplySupportsOutOfOrderStagingAndLkgRecovery() {
        TestRelease release = release(
                "release-large",
                "x".repeat(INLINE_LIMIT_BYTES + 10)
        );
        GatewayRuleChunkStore chunks = new GatewayRuleChunkStore();
        GatewayRuleActivationApplier applier = applier(chunks);
        release.activation().chunks().reversed().forEach(reference ->
                chunks.apply(
                        reference.configKey(),
                        release.chunkValues().get(reference.configKey()),
                        reference.index() + 1L
                ));

        applier.apply(
                GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY,
                release.activationJson(),
                7
        );

        assertEquals("release-large", applier.active().snapshot().releaseId());
        assertEquals(0, chunks.size());
        GatewayRuleActivationApplier restored = applier(
                new GatewayRuleChunkStore()
        );
        assertTrue(restored.restoreLkg());
        assertNotNull(restored.active());
        assertEquals("release-large",
                restored.active().snapshot().releaseId());
        assertTrue(restored.status().degraded());
    }

    @Test
    void failedLkgWriteKeepsAssembledChunksForRetry() throws Exception {
        TestRelease release = release(
                "release-large",
                "x".repeat(INLINE_LIMIT_BYTES + 10)
        );
        GatewayRuleChunkStore chunks = new GatewayRuleChunkStore();
        release.activation().chunks().forEach(reference -> chunks.apply(
                reference.configKey(),
                release.chunkValues().get(reference.configKey()),
                reference.index() + 1L
        ));
        Path invalidDataDirectory = dataDirectory.resolve("not-a-directory");
        Files.writeString(invalidDataDirectory, "blocked");
        Clock clock = Clock.systemUTC();
        GatewayRuleActivationApplier applier =
                new GatewayRuleActivationApplier(
                        new GatewayRuleJsonCodec(),
                        new EngineGatewayRuleCompiler(),
                        chunks,
                        new ProviderDirectory(new EmptyRegistry(), clock),
                        new GatewayRuleLkgRepository(
                                invalidDataDirectory,
                                "orders"
                        ),
                        clock
                );

        assertThrows(IllegalStateException.class, () -> applier.apply(
                GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY,
                release.activationJson(),
                7
        ));
        assertTrue(chunks.size() > 0);
    }

    private GatewayRuleActivationApplier applier() {
        return applier(new GatewayRuleChunkStore());
    }

    private GatewayRuleActivationApplier applier(
            GatewayRuleChunkStore chunks) {
        Clock clock = Clock.systemUTC();
        ProviderDirectory providers = new ProviderDirectory(
                new EmptyRegistry(),
                clock
        );
        return new GatewayRuleActivationApplier(
                new GatewayRuleJsonCodec(),
                new EngineGatewayRuleCompiler(),
                chunks,
                providers,
                new GatewayRuleLkgRepository(dataDirectory, "orders"),
                clock
        );
    }

    private TestRelease release(String releaseId, String schema) {
        GatewayProviderServiceRef service = new GatewayProviderServiceRef(
                "local",
                "default",
                GatewayProtocol.HTTP,
                "orders",
                "default",
                "v1",
                "http"
        );
        GatewayRuntimeOperation operation = new GatewayRuntimeOperation(
                "orders",
                "orders",
                GatewayProtocol.HTTP,
                "GET /orders",
                schema,
                "{}",
                true,
                service,
                "TRANSPARENT",
                Set.of(),
                Map.of(),
                false
        );
        GatewayRuntimeRoute route = new GatewayRuntimeRoute(
                "orders",
                "orders",
                "api.example.com",
                "GET",
                "/orders",
                Set.of(AccessZone.PUBLIC),
                0,
                true
        );
        GatewayRuleContent content = new GatewayRuleContent(
                "group-1",
                "orders",
                "local",
                "default",
                List.of(operation),
                List.of(route),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        GatewayRuleJsonCodec codec = new GatewayRuleJsonCodec();
        Instant generatedAt = Instant.parse("2026-07-25T00:00:00Z");
        String contentSha = GatewayRuleJsonCodec.sha256(
                codec.write(content)
        );
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
        byte[] snapshotBytes = codec.write(snapshot);
        Map<String, String> chunks = new LinkedHashMap<>();
        List<GatewayRuleChunkRef> references = new ArrayList<>();
        GatewayRuleActivationMode mode;
        String inlineSnapshot;
        if (snapshotBytes.length <= INLINE_LIMIT_BYTES) {
            mode = GatewayRuleActivationMode.INLINE;
            inlineSnapshot = new String(
                    snapshotBytes,
                    StandardCharsets.UTF_8
            );
        } else {
            mode = GatewayRuleActivationMode.CHUNKED;
            inlineSnapshot = null;
            for (int offset = 0, index = 0;
                 offset < snapshotBytes.length;
                 offset += CHUNK_LIMIT_BYTES, index++) {
                int length = Math.min(
                        CHUNK_LIMIT_BYTES,
                        snapshotBytes.length - offset
                );
                byte[] chunk = java.util.Arrays.copyOfRange(
                        snapshotBytes,
                        offset,
                        offset + length
                );
                String configKey = "gateway.rules.chunk."
                        + releaseId
                        + "."
                        + index;
                chunks.put(
                        configKey,
                        Base64.getEncoder().encodeToString(chunk)
                );
                references.add(new GatewayRuleChunkRef(
                        configKey,
                        index,
                        length,
                        GatewayRuleJsonCodec.sha256(chunk)
                ));
            }
        }
        GatewayRuleActivation activation = new GatewayRuleActivation(
                "v1",
                releaseId,
                mode,
                "v1",
                snapshotBytes.length,
                contentSha,
                artifactSha,
                inlineSnapshot,
                references
        );
        return new TestRelease(
                activation,
                new String(codec.write(activation), StandardCharsets.UTF_8),
                chunks
        );
    }

    private record TestRelease(
            GatewayRuleActivation activation,
            String activationJson,
            Map<String, String> chunkValues
    ) {

        private TestRelease {
            chunkValues = Map.copyOf(chunkValues);
        }
    }

    private static final class EmptyRegistry
            implements ProviderServiceRegistry {

        @Override
        public ProviderCatalogSnapshot getServiceKeys(ProviderQuery query) {
            return new ProviderCatalogSnapshot(
                    1,
                    Instant.now(),
                    List.of()
            );
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
