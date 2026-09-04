package top.egon.cola.component.gateway.runtime.rule.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.gateway.runtime.observability.domain.GatewayTelemetry;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivationMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleContent;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.runtime.provider.domain.RuntimeProviderPolicy;
import top.egon.cola.component.gateway.runtime.provider.service.ProviderDirectory;
import top.egon.cola.component.gateway.runtime.rule.adapter.json.GatewayRuleJsonCodec;
import top.egon.cola.component.gateway.runtime.rule.domain.GatewayCompiledRulesDTO;
import top.egon.cola.component.gateway.runtime.rule.domain.GatewayRuleApplyStage;
import top.egon.cola.component.gateway.runtime.rule.repository.GatewayRuleChunkStore;
import top.egon.cola.component.gateway.runtime.rule.repository.GatewayRuleLkgRepository;
import top.egon.cola.component.gateway.runtime.traffic.domain.RuntimeTrafficPolicy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GatewayRuleCompilerStrategyTest {

    @TempDir
    Path directory;

    private final GatewayRuleJsonCodec codec = new GatewayRuleJsonCodec();
    private final ProviderDirectory providers = mock(ProviderDirectory.class);

    @Test
    void generatedDependencyConstructorRetainsQualifiersAndNullChecks() throws Exception {
        var constructor = GatewayRuleActivationApplier.class.getConstructor(
                GatewayRuleJsonCodec.class, GatewayRuleCompilerStrategy.class,
                GatewayRuleChunkStore.class, ProviderDirectory.class,
                GatewayRuleLkgRepository.class, Clock.class, GatewayTelemetry.class);
        assertEquals(List.of("gatewayRuleJsonCodec", "gatewayRuleCompilerStrategy",
                        "gatewayRuleChunkStore", "gatewayProviderDirectory", "gatewayRuleLkgRepository",
                        "gatewayClock", "gatewayTelemetry"),
                Arrays.stream(constructor.getParameters())
                        .map(parameter -> parameter.getAnnotation(Qualifier.class).value()).toList());
        assertThrows(NullPointerException.class, () -> applier(null, repository()));
    }

    @Test
    void activatesOneSnapshotAndKeepsDdcVersionOutsideCompiledIdentity() {
        GatewayRuleSnapshot snapshot = snapshot("release-1");
        var applier = applier(TestCompiledRulesDTO::new, repository());

        applier.apply(GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY, activation(snapshot), 7);

        assertEquals(snapshot, applier.active().snapshot());
        assertEquals(snapshot.releaseId(), applier.active().releaseId());
        assertEquals(snapshot.artifactSha256(), applier.active().ruleChecksum());
        assertEquals(7, applier.status().activeDdcVersion());
        assertEquals(GatewayRuleApplyStage.ACK_SUCCESS, applier.status().lastStage());
        assertTrue(Files.exists(directory.resolve("rules/orders/releases/release-1.json")));
    }

    @Test
    void compilerFailureRetainsPreviousMemoryLkgAndAckIdentity() {
        var applier = applier(snapshot -> {
            if (snapshot.releaseId().equals("release-2")) {
                throw new IllegalArgumentException("projection rejected");
            }
            return new TestCompiledRulesDTO(snapshot);
        }, repository());
        applier.apply(GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY, activation(snapshot("release-1")), 7);
        TestCompiledRulesDTO previous = applier.active();

        assertThrows(IllegalArgumentException.class, () -> applier.apply(
                GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY, activation(snapshot("release-2")), 8));

        assertSame(previous, applier.active());
        assertEquals("release-1", repository().loadActive().orElseThrow().releaseId());
        assertEquals(7, applier.status().activeDdcVersion());
        assertEquals(GatewayRuleApplyStage.FAILED, applier.status().lastStage());
    }

    @Test
    void rejectsCompilerIdentityDriftBeforeProviderPreparation() {
        GatewayRuleSnapshot snapshot = snapshot("release-1");
        List<GatewayRuleCompilerStrategy<TestCompiledRulesDTO>> invalid = List.of(
                value -> new TestCompiledRulesDTO(value, "different", value.artifactSha256()),
                value -> new TestCompiledRulesDTO(value, value.releaseId(), "different"),
                value -> new TestCompiledRulesDTO(snapshot("different"), value.releaseId(), value.artifactSha256()));
        for (GatewayRuleCompilerStrategy<TestCompiledRulesDTO> compiler : invalid) {
            var applier = applier(compiler, repository());
            assertThrows(IllegalArgumentException.class, () -> applier.apply(
                    GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY, activation(snapshot), 7));
            assertNull(applier.active());
        }
        verifyNoInteractions(providers);
        assertTrue(repository().loadActive().isEmpty());
    }

    @Test
    void durableWriteFailureReleasesPreparedProvidersWithoutSwappingMemory() {
        GatewayRuleLkgRepository failing = mock(GatewayRuleLkgRepository.class);
        doThrow(new IllegalStateException("disk unavailable"))
                .when(failing).persistAndActivate(any(), any());
        var applier = applier(TestCompiledRulesDTO::new, failing);

        assertThrows(IllegalStateException.class, () -> applier.apply(
                GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY, activation(snapshot("release-1")), 7));

        assertNull(applier.active());
        var order = inOrder(providers, failing);
        order.verify(providers).activate(Set.of());
        order.verify(failing).persistAndActivate(any(), any());
        order.verify(providers).release(Set.of());
        assertEquals(0, applier.status().activeDdcVersion());
    }

    @Test
    void restoreUsesSameStrategyWithDegradedZeroVersionUntilDdcApply() {
        GatewayRuleSnapshot snapshot = snapshot("release-1");
        repository().persistAndActivate(snapshot, codec.write(snapshot));
        AtomicInteger compiled = new AtomicInteger();
        var applier = applier(value -> {
            compiled.incrementAndGet();
            return new TestCompiledRulesDTO(value);
        }, repository());

        assertTrue(applier.restoreLkg());
        assertEquals(snapshot.artifactSha256(), applier.active().ruleChecksum());
        assertEquals(0, applier.status().activeDdcVersion());
        assertTrue(applier.status().degraded());

        applier.apply(GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY, activation(snapshot), 7);
        applier.apply(GatewayRuleActivationApplier.ACTIVE_CONFIG_KEY, activation(snapshot("stale")), 6);
        assertEquals(2, compiled.get());
        assertEquals(7, applier.status().activeDdcVersion());
        assertFalse(applier.status().degraded());
    }

    @Test
    void corruptLkgNeverPreparesProvidersOrPublishesActiveState() throws Exception {
        GatewayRuleSnapshot snapshot = snapshot("release-1");
        repository().persistAndActivate(snapshot, codec.write(snapshot));
        Files.writeString(directory.resolve("rules/orders/releases/release-1.sha256"), "corrupt");
        var applier = applier(TestCompiledRulesDTO::new, repository());

        assertThrows(IllegalArgumentException.class, applier::restoreLkg);
        assertNull(applier.active());
        verifyNoInteractions(providers);
    }

    private GatewayRuleActivationApplier<TestCompiledRulesDTO> applier(
            GatewayRuleCompilerStrategy<TestCompiledRulesDTO> compiler,
            GatewayRuleLkgRepository repository) {
        return new GatewayRuleActivationApplier<>(codec, compiler,
                new GatewayRuleChunkStore(), providers, repository, Clock.systemUTC());
    }

    private GatewayRuleLkgRepository repository() {
        return new GatewayRuleLkgRepository(directory, "orders");
    }

    private GatewayRuleSnapshot snapshot(String releaseId) {
        GatewayRuleContent content = new GatewayRuleContent(
                "group-1", "orders", "local", "default",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        Instant generatedAt = Instant.parse("2026-07-25T00:00:00Z");
        String contentSha = GatewayRuleJsonCodec.sha256(codec.write(content));
        String artifactSha = GatewayRuleJsonCodec.sha256(codec.write(Map.of(
                "content", content, "generatedAt", generatedAt, "releaseId", releaseId,
                "ruleContentSha256", contentSha, "ruleSchemaVersion", "v1")));
        return new GatewayRuleSnapshot("v1", releaseId, generatedAt, contentSha, artifactSha, content);
    }

    private String activation(GatewayRuleSnapshot snapshot) {
        byte[] bytes = codec.write(snapshot);
        GatewayRuleActivation activation = new GatewayRuleActivation("v1", snapshot.releaseId(),
                GatewayRuleActivationMode.INLINE, "v1", bytes.length, snapshot.ruleContentSha256(),
                snapshot.artifactSha256(), new String(bytes, StandardCharsets.UTF_8), List.of());
        return new String(codec.write(activation), StandardCharsets.UTF_8);
    }

    private record TestCompiledRulesDTO(
            GatewayRuleSnapshot snapshot, String releaseId, String ruleChecksum
    ) implements GatewayCompiledRulesDTO {

        private TestCompiledRulesDTO(GatewayRuleSnapshot snapshot) {
            this(snapshot, snapshot.releaseId(), snapshot.artifactSha256());
        }

        @Override
        public Set<ProviderServiceKey> providerServices() {
            return Set.of();
        }

        @Override
        public Map<String, RuntimeProviderPolicy> providerPolicies() {
            return Map.of();
        }

        @Override
        public Map<String, RuntimeTrafficPolicy> trafficPolicies() {
            return Map.of();
        }
    }
}
