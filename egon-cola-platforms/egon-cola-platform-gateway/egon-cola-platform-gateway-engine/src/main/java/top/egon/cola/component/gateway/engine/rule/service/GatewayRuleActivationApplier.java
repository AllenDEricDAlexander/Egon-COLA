package top.egon.cola.component.gateway.engine.rule.service;

import top.egon.cola.component.gateway.engine.rule.adapter.json.GatewayRuleJsonCodec;
import top.egon.cola.component.gateway.engine.rule.domain.CompiledGatewayRules;
import top.egon.cola.component.gateway.engine.rule.domain.GatewayRuleApplyStage;
import top.egon.cola.component.gateway.engine.rule.domain.GatewayRuleRuntimeStatus;
import top.egon.cola.component.gateway.engine.rule.repository.GatewayRuleChunkStore;
import top.egon.cola.component.gateway.engine.rule.repository.GatewayRuleLkgRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.ddc.api.refresh.DdcConfigApplier;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivationMode;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.engine.common.provider.service.ProviderDirectory;
import top.egon.cola.component.gateway.engine.common.observability.domain.GatewayTelemetry;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 中文说明：{@code GatewayRuleActivationApplier} 是类型，位于当前 Gateway 模块的相关包中，负责网关规则ActivationApplier相关的职责与边界。
 * English summary: {@code GatewayRuleActivationApplier} is a type in the current Gateway module; it owns the gateway rule activation applier-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayRuleActivationApplier implements DdcConfigApplier {

    /**
     * 中文说明：表示 LOGGER 这一固定值；它属于 {@code GatewayRuleActivationApplier} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value logger; it is a state, type, or protocol value of {@code GatewayRuleActivationApplier} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleActivationApplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleActivationApplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(
            GatewayRuleActivationApplier.class
    );

    /**
     * 中文说明：表示 ACTIVECONFIG键 这一固定值；它属于 {@code GatewayRuleActivationApplier} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value active config key; it is a state, type, or protocol value of {@code GatewayRuleActivationApplier} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleActivationApplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleActivationApplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final String ACTIVE_CONFIG_KEY = "gateway.rules.active";

    /**
     * 中文说明：保存 codec 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleJsonCodec}，由 {@code GatewayRuleActivationApplier} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by codec; its type is {@code GatewayRuleJsonCodec}, and {@code GatewayRuleActivationApplier} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleActivationApplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleActivationApplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRuleJsonCodec codec;

    /**
     * 中文说明：保存 compiler 对应的状态、依赖或配置值；字段类型为 {@code EngineGatewayRuleCompiler}，由 {@code GatewayRuleActivationApplier} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by compiler; its type is {@code EngineGatewayRuleCompiler}, and {@code GatewayRuleActivationApplier} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleActivationApplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleActivationApplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final EngineGatewayRuleCompiler compiler;

    /**
     * 中文说明：保存 chunks 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleChunkStore}，由 {@code GatewayRuleActivationApplier} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by chunks; its type is {@code GatewayRuleChunkStore}, and {@code GatewayRuleActivationApplier} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleActivationApplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleActivationApplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRuleChunkStore chunks;

    /**
     * 中文说明：保存 提供方Directory 对应的状态、依赖或配置值；字段类型为 {@code ProviderDirectory}，由 {@code GatewayRuleActivationApplier} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by provider directory; its type is {@code ProviderDirectory}, and {@code GatewayRuleActivationApplier} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleActivationApplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleActivationApplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ProviderDirectory providerDirectory;

    /**
     * 中文说明：保存 lkgRepository 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleLkgRepository}，由 {@code GatewayRuleActivationApplier} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by lkg repository; its type is {@code GatewayRuleLkgRepository}, and {@code GatewayRuleActivationApplier} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleActivationApplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleActivationApplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayRuleLkgRepository lkgRepository;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code GatewayRuleActivationApplier} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code GatewayRuleActivationApplier} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleActivationApplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleActivationApplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 遥测 对应的状态、依赖或配置值；字段类型为 {@code GatewayTelemetry}，由 {@code GatewayRuleActivationApplier} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by telemetry; its type is {@code GatewayTelemetry}, and {@code GatewayRuleActivationApplier} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleActivationApplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleActivationApplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final GatewayTelemetry telemetry;

    /**
     * 中文说明：保存 active 对应的状态、依赖或配置值；字段类型为 {@code AtomicReference<CompiledGatewayRules>}，由 {@code GatewayRuleActivationApplier} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by active; its type is {@code AtomicReference<CompiledGatewayRules>}, and {@code GatewayRuleActivationApplier} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleActivationApplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleActivationApplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicReference<CompiledGatewayRules> active =
            new AtomicReference<>();

    /**
     * 中文说明：保存 status 对应的状态、依赖或配置值；字段类型为 {@code AtomicReference<GatewayRuleRuntimeStatus>}，由 {@code GatewayRuleActivationApplier} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by status; its type is {@code AtomicReference<GatewayRuleRuntimeStatus>}, and {@code GatewayRuleActivationApplier} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleActivationApplier} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleActivationApplier}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final AtomicReference<GatewayRuleRuntimeStatus> status =
            new AtomicReference<>(GatewayRuleRuntimeStatus.empty());

    /**
     * 中文说明：创建 {@code GatewayRuleActivationApplier} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayRuleActivationApplier} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param codec 参数 codec；parameter codec。
     * @param compiler 参数 compiler；parameter compiler。
     * @param chunks 参数 chunks；parameter chunks。
     * @param providerDirectory 参数 提供方Directory；parameter provider directory。
     * @param lkgRepository 参数 lkgRepository；parameter lkg repository。
     * @param clock 参数 clock；parameter clock。
     */
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

    /**
     * 中文说明：创建 {@code GatewayRuleActivationApplier} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayRuleActivationApplier} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param codec 参数 codec；parameter codec。
     * @param compiler 参数 compiler；parameter compiler。
     * @param chunks 参数 chunks；parameter chunks。
     * @param providerDirectory 参数 提供方Directory；parameter provider directory。
     * @param lkgRepository 参数 lkgRepository；parameter lkg repository。
     * @param clock 参数 clock；parameter clock。
     * @param telemetry 参数 遥测；parameter telemetry。
     */
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

    /**
     * 中文说明：执行 apply 操作；该方法是 {@code GatewayRuleActivationApplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the apply operation; this method is the invocation entry point on {@code GatewayRuleActivationApplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleActivationApplier.apply(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param key 参数 键；parameter key。
     * @param value 参数 值；parameter value。
     * @param version 参数 version；parameter version。
     */
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
            LOGGER.warn(
                    "Gateway rule application failed for key={} version={}",
                    key,
                    version,
                    failure
            );
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

    /**
     * 中文说明：执行 priority 操作；该方法是 {@code GatewayRuleActivationApplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the priority operation; this method is the invocation entry point on {@code GatewayRuleActivationApplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleActivationApplier.priority(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 priority 的处理结果；returns the result of the operation.
     */
    @Override
    public int priority() {
        return 100;
    }

    /**
     * 中文说明：执行 restoreLkg 操作；该方法是 {@code GatewayRuleActivationApplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the restore lkg operation; this method is the invocation entry point on {@code GatewayRuleActivationApplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleActivationApplier.restoreLkg(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 restoreLkg 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 active 操作；该方法是 {@code GatewayRuleActivationApplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the active operation; this method is the invocation entry point on {@code GatewayRuleActivationApplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleActivationApplier.active(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 active 的处理结果；returns the result of the operation.
     */
    public CompiledGatewayRules active() {
        return active.get();
    }

    /**
     * 中文说明：执行 status 操作；该方法是 {@code GatewayRuleActivationApplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the status operation; this method is the invocation entry point on {@code GatewayRuleActivationApplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleActivationApplier.status(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 status 的处理结果；returns the result of the operation.
     */
    public GatewayRuleRuntimeStatus status() {
        return status.get();
    }

    /**
     * 中文说明：执行 snapshotJson 操作；该方法是 {@code GatewayRuleActivationApplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the snapshot json operation; this method is the invocation entry point on {@code GatewayRuleActivationApplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleActivationApplier.snapshotJson(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activation 参数 activation；parameter activation。
     * @return 返回 snapshotJson 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 verifyActivation 操作；该方法是 {@code GatewayRuleActivationApplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the verify activation operation; this method is the invocation entry point on {@code GatewayRuleActivationApplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleActivationApplier.verifyActivation(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activation 参数 activation；parameter activation。
     * @param snapshot 参数 snapshot；parameter snapshot。
     * @param snapshotJson 参数 snapshotJson；parameter snapshot json。
     */
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

    /**
     * 中文说明：执行 difference 操作；该方法是 {@code GatewayRuleActivationApplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the difference operation; this method is the invocation entry point on {@code GatewayRuleActivationApplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleActivationApplier.difference(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param left 参数 left；parameter left。
     * @param right 参数 right；parameter right。
     * @return 返回 difference 的处理结果；returns the result of the operation.
     */
    private Set<ProviderServiceKey> difference(
            Set<ProviderServiceKey> left,
            Set<ProviderServiceKey> right) {
        Set<ProviderServiceKey> result = new LinkedHashSet<>(left);
        result.removeAll(right);
        return Set.copyOf(result);
    }

    /**
     * 中文说明：执行 updateStage 操作；该方法是 {@code GatewayRuleActivationApplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the update stage operation; this method is the invocation entry point on {@code GatewayRuleActivationApplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleActivationApplier.updateStage(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param stage 参数 stage；parameter stage。
     * @param error 参数 error；parameter error。
     */
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

    /**
     * 中文说明：执行 successStatus 操作；该方法是 {@code GatewayRuleActivationApplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the success status operation; this method is the invocation entry point on {@code GatewayRuleActivationApplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleActivationApplier.successStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param rules 参数 rules；parameter rules。
     * @param version 参数 version；parameter version。
     * @return 返回 successStatus 的处理结果；returns the result of the operation.
     */
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

    /**
     * 中文说明：执行 safeError 操作；该方法是 {@code GatewayRuleActivationApplier} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the safe error operation; this method is the invocation entry point on {@code GatewayRuleActivationApplier} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleActivationApplier.safeError(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param failure 参数 failure；parameter failure。
     * @return 返回 safeError 的处理结果；returns the result of the operation.
     */
    private String safeError(RuntimeException failure) {
        String message = failure.getMessage();
        if (message == null) {
            return failure.getClass().getSimpleName();
        }
        return message.length() <= 512 ? message : message.substring(0, 512);
    }
}
