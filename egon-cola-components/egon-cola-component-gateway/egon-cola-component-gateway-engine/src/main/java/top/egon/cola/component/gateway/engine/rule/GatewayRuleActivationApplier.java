package top.egon.cola.component.gateway.engine.rule;

import top.egon.cola.component.ddc.service.DdcConfigApplier;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivationMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.engine.discovery.ProviderDirectory;
import top.egon.cola.component.gateway.engine.observability.GatewayTelemetry;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class GatewayRuleActivationApplier implements DdcConfigApplier {

    public static final String ACTIVE_CONFIG_KEY = "gateway.rules.active";

    private final GatewayRuleJsonCodec codec;

    private final EngineGatewayRuleCompiler compiler;

    private final GatewayRuleChunkStore chunks;

    private final ProviderDirectory providerDirectory;

    private final GatewayRuleLkgRepository lkgRepository;

    private final Clock clock;

    private final GatewayTelemetry telemetry;

    private final AtomicReference<CompiledGatewayRules> active =
            new AtomicReference<>();

    private final AtomicReference<GatewayRuleRuntimeStatus> status =
            new AtomicReference<>(GatewayRuleRuntimeStatus.empty());

    public GatewayRuleActivationApplier(
            GatewayRuleJsonCodec codec,
            EngineGatewayRuleCompiler compiler,
            GatewayRuleChunkStore chunks,
            ProviderDirectory providerDirectory,
            GatewayRuleLkgRepository lkgRepository,
            Clock clock) {
        this(
                codec,
                compiler,
                chunks,
                providerDirectory,
                lkgRepository,
                clock,
                GatewayTelemetry.noop()
        );
    }

    public GatewayRuleActivationApplier(
            GatewayRuleJsonCodec codec,
            EngineGatewayRuleCompiler compiler,
            GatewayRuleChunkStore chunks,
            ProviderDirectory providerDirectory,
            GatewayRuleLkgRepository lkgRepository,
            Clock clock,
            GatewayTelemetry telemetry) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.compiler = Objects.requireNonNull(compiler, "compiler");
        this.chunks = Objects.requireNonNull(chunks, "chunks");
        this.providerDirectory = Objects.requireNonNull(
                providerDirectory,
                "providerDirectory"
        );
        this.lkgRepository = Objects.requireNonNull(
                lkgRepository,
                "lkgRepository"
        );
        this.clock = Objects.requireNonNull(clock, "clock");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
    }

    @Override
    public synchronized void apply(String key, String value, long version) {
        GatewayTelemetry.Operation operation =
                telemetry.startDdcApply(key, version);
        if (!ACTIVE_CONFIG_KEY.equals(key)) {
            operation.failure(new IllegalArgumentException(
                    "unexpected active rule key"
            ));
            throw new IllegalArgumentException("unexpected active rule key");
        }
        if (version <= status.get().activeDdcVersion()) {
            operation.ignored();
            return;
        }
        try {
            updateStage(GatewayRuleApplyStage.RECEIVED, null);
            GatewayRuleActivation activation = codec.readActivation(value);
            byte[] snapshotJson = snapshotJson(activation);
            GatewayRuleSnapshot snapshot = codec.readSnapshot(snapshotJson);
            codec.verify(snapshot);
            verifyActivation(activation, snapshot, snapshotJson);
            updateStage(GatewayRuleApplyStage.CHECKSUM_VERIFIED, null);
            updateStage(GatewayRuleApplyStage.SCHEMA_VALIDATED, null);
            CompiledGatewayRules prepared = compiler.compile(snapshot);
            updateStage(GatewayRuleApplyStage.COMPILED, null);
            CompiledGatewayRules previous = active.get();
            Set<ProviderServiceKey> additions = difference(
                    prepared.providerServices(),
                    previous == null
                            ? Set.of()
                            : previous.providerServices()
            );
            providerDirectory.activate(additions);
            updateStage(GatewayRuleApplyStage.RESOURCE_PREPARED, null);
            try {
                lkgRepository.persistAndActivate(snapshot, snapshotJson);
            } catch (RuntimeException failure) {
                providerDirectory.release(additions);
                throw failure;
            }
            updateStage(GatewayRuleApplyStage.DURABLE_STAGED, null);
            updateStage(GatewayRuleApplyStage.ACTIVE_POINTER_WRITTEN, null);
            active.set(prepared);
            updateStage(GatewayRuleApplyStage.MEMORY_ACTIVATED, null);
            chunks.removeRelease(activation.releaseId());
            if (previous != null) {
                providerDirectory.release(difference(
                        previous.providerServices(),
                        prepared.providerServices()
                ));
            }
            status.set(successStatus(prepared, version));
            operation.success();
        } catch (RuntimeException failure) {
            operation.failure(failure);
            GatewayRuleRuntimeStatus current = status.get();
            status.set(new GatewayRuleRuntimeStatus(
                    current.activeReleaseId(),
                    current.activeDdcVersion(),
                    current.ruleSchemaVersion(),
                    current.ruleContentSha256(),
                    current.artifactSha256(),
                    GatewayRuleApplyStage.FAILED,
                    safeError(failure),
                    current.routeCount(),
                    current.operationCount(),
                    current.providerServiceCount(),
                    chunks.size(),
                    current.ready(),
                    current.degraded(),
                    clock.instant()
            ));
            throw failure;
        }
    }

    @Override
    public int priority() {
        return 100;
    }

    public synchronized boolean restoreLkg() {
        GatewayRuleLkgRepository.StoredGatewayRule stored = lkgRepository
                .loadActive()
                .orElse(null);
        if (stored == null) {
            return false;
        }
        GatewayRuleSnapshot snapshot = codec.readSnapshot(
                stored.snapshotJson()
        );
        codec.verify(snapshot);
        if (!stored.artifactSha256().equals(snapshot.artifactSha256())) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_CHECKSUM_MISMATCH: LKG"
            );
        }
        CompiledGatewayRules prepared = compiler.compile(snapshot);
        providerDirectory.activate(prepared.providerServices());
        active.set(prepared);
        status.set(successStatus(prepared, 0));
        return true;
    }

    public CompiledGatewayRules active() {
        return active.get();
    }

    public GatewayRuleRuntimeStatus status() {
        return status.get();
    }

    private byte[] snapshotJson(GatewayRuleActivation activation) {
        if (activation.mode() == GatewayRuleActivationMode.INLINE) {
            byte[] value = activation.inlineSnapshot()
                    .getBytes(StandardCharsets.UTF_8);
            if (value.length != activation.totalSize()) {
                throw new IllegalArgumentException(
                        "GATEWAY_RULE_CHECKSUM_MISMATCH: total size"
                );
            }
            return value;
        }
        return chunks.assemble(activation);
    }

    private void verifyActivation(
            GatewayRuleActivation activation,
            GatewayRuleSnapshot snapshot,
            byte[] snapshotJson) {
        if (!activation.releaseId().equals(snapshot.releaseId())
                || !activation.ruleSchemaVersion().equals(
                snapshot.ruleSchemaVersion()
        )
                || !activation.ruleContentSha256().equals(
                snapshot.ruleContentSha256()
        )
                || !activation.artifactSha256().equals(
                snapshot.artifactSha256()
        )
                || snapshotJson.length != activation.totalSize()) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_CHECKSUM_MISMATCH: activation"
            );
        }
    }

    private Set<ProviderServiceKey> difference(
            Set<ProviderServiceKey> left,
            Set<ProviderServiceKey> right) {
        Set<ProviderServiceKey> result = new LinkedHashSet<>(left);
        result.removeAll(right);
        return Set.copyOf(result);
    }

    private void updateStage(
            GatewayRuleApplyStage stage,
            String error) {
        GatewayRuleRuntimeStatus current = status.get();
        status.set(new GatewayRuleRuntimeStatus(
                current.activeReleaseId(),
                current.activeDdcVersion(),
                current.ruleSchemaVersion(),
                current.ruleContentSha256(),
                current.artifactSha256(),
                stage,
                error,
                current.routeCount(),
                current.operationCount(),
                current.providerServiceCount(),
                chunks.size(),
                current.ready(),
                current.degraded(),
                clock.instant()
        ));
    }

    private GatewayRuleRuntimeStatus successStatus(
            CompiledGatewayRules rules,
            long version) {
        GatewayRuleSnapshot snapshot = rules.snapshot();
        return new GatewayRuleRuntimeStatus(
                snapshot.releaseId(),
                version,
                snapshot.ruleSchemaVersion(),
                snapshot.ruleContentSha256(),
                snapshot.artifactSha256(),
                GatewayRuleApplyStage.ACK_SUCCESS,
                null,
                snapshot.content().routes().size(),
                snapshot.content().operations().size(),
                rules.providerServices().size(),
                chunks.size(),
                true,
                version == 0,
                clock.instant()
        );
    }

    private String safeError(RuntimeException failure) {
        String message = failure.getMessage();
        if (message == null) {
            return failure.getClass().getSimpleName();
        }
        return message.length() <= 512 ? message : message.substring(0, 512);
    }
}
