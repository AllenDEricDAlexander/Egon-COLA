package top.egon.cola.component.gateway.engine.rule;

import top.egon.cola.component.ddc.api.refresh.DdcConfigApplier;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleChunkRef;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中文说明：{@code GatewayRuleChunkStore} 是存储组件，位于当前 Gateway 模块的相关包中，负责网关规则Chunk存储相关的职责与边界。
 * English summary: {@code GatewayRuleChunkStore} is a gateway rule chunk store store in the current Gateway module; it owns the gateway rule chunk store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayRuleChunkStore implements DdcConfigApplier {

    /**
     * 中文说明：保存 chunks 对应的状态、依赖或配置值；字段类型为 {@code Map<String, byte[]>}，由 {@code GatewayRuleChunkStore} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by chunks; its type is {@code Map<String, byte[]>}, and {@code GatewayRuleChunkStore} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayRuleChunkStore} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRuleChunkStore}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, byte[]> chunks = new ConcurrentHashMap<>();

    /**
     * 中文说明：执行 apply 操作；该方法是 {@code GatewayRuleChunkStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the apply operation; this method is the invocation entry point on {@code GatewayRuleChunkStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkStore.apply(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param key 参数 键；parameter key。
     * @param value 参数 值；parameter value。
     * @param version 参数 version；parameter version。
     */
    @Override
    public void apply(String key, String value, long version) {
        if (!key.startsWith("gateway.rules.chunk.")) {
            throw new IllegalArgumentException("unexpected rule chunk key");
        }
        try {
            chunks.put(key, Base64.getDecoder().decode(value));
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_CHUNK_CHECKSUM_MISMATCH",
                    invalid
            );
        }
    }

    /**
     * 中文说明：执行 assemble 操作；该方法是 {@code GatewayRuleChunkStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the assemble operation; this method is the invocation entry point on {@code GatewayRuleChunkStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkStore.assemble(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activation 参数 activation；parameter activation。
     * @return 返回 assemble 的处理结果；returns the result of the operation.
     */
    public byte[] assemble(GatewayRuleActivation activation) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(
                activation.totalSize()
        );
        for (int expectedIndex = 0;
             expectedIndex < activation.chunks().size();
             expectedIndex++) {
            GatewayRuleChunkRef reference = activation.chunks()
                    .get(expectedIndex);
            if (reference.index() != expectedIndex) {
                throw new IllegalArgumentException(
                        "GATEWAY_RULE_CHUNK_MISSING: non-contiguous index"
                );
            }
            byte[] value = chunks.get(reference.configKey());
            if (value == null || value.length != reference.size()) {
                throw new IllegalArgumentException(
                        "GATEWAY_RULE_CHUNK_MISSING: "
                                + reference.configKey()
                );
            }
            if (!GatewayRuleJsonCodec.sha256(value)
                    .equals(reference.sha256())) {
                throw new IllegalArgumentException(
                        "GATEWAY_RULE_CHUNK_CHECKSUM_MISMATCH"
                );
            }
            output.writeBytes(value);
        }
        byte[] assembled = output.toByteArray();
        if (assembled.length != activation.totalSize()) {
            throw new IllegalArgumentException(
                    "GATEWAY_RULE_CHUNK_MISSING: total size mismatch"
            );
        }
        return assembled;
    }

    /**
     * 中文说明：执行 size 操作；该方法是 {@code GatewayRuleChunkStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the size operation; this method is the invocation entry point on {@code GatewayRuleChunkStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkStore.size(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 size 的处理结果；returns the result of the operation.
     */
    public int size() {
        return chunks.size();
    }

    /**
     * 中文说明：执行 remove发布 操作；该方法是 {@code GatewayRuleChunkStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the remove release operation; this method is the invocation entry point on {@code GatewayRuleChunkStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRuleChunkStore.removeRelease(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param releaseId 参数 发布Id；parameter release id。
     * @return 返回 remove发布 的处理结果；returns the result of the operation.
     */
    public int removeRelease(String releaseId) {
        if (releaseId == null || releaseId.isBlank()) {
            throw new IllegalArgumentException("releaseId is required");
        }
        String prefix = "gateway.rules.chunk." + releaseId + ".";
        int removed = 0;
        for (Map.Entry<String, byte[]> entry : chunks.entrySet()) {
            if (entry.getKey().startsWith(prefix)
                    && chunks.remove(entry.getKey(), entry.getValue())) {
                removed++;
            }
        }
        return removed;
    }
}
