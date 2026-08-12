package top.egon.cola.component.gateway.admin.rule;

import top.egon.cola.component.gateway.contract.rule.GatewayRuleActivation;
import top.egon.cola.component.gateway.contract.rule.GatewayRuleSnapshot;

import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code CompiledGatewayRelease} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Compiled网关发布相关的职责与边界。
 * English summary: {@code CompiledGatewayRelease} is an immutable data carrier in the current Gateway module; it owns the compiled gateway release-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param snapshot 参数 snapshot；parameter snapshot。
 * @param snapshotJson 参数 snapshotJson；parameter snapshot json。
 * @param activation 参数 activation；parameter activation。
 * @param activationJson 参数 activationJson；parameter activation json。
 * @param chunkValues 参数 chunkValues；parameter chunk values。
 */
public record CompiledGatewayRelease(
        /**
         * 中文说明：保存 snapshot 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleSnapshot}，由 {@code CompiledGatewayRelease} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by snapshot; its type is {@code GatewayRuleSnapshot}, and {@code CompiledGatewayRelease} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledGatewayRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledGatewayRelease}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayRuleSnapshot snapshot,
        /**
         * 中文说明：保存 snapshotJson 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code CompiledGatewayRelease} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by snapshot json; its type is {@code String}, and {@code CompiledGatewayRelease} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledGatewayRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledGatewayRelease}; do not couple callers to its representation when the owning type exposes an API.
         */
        String snapshotJson,
        /**
         * 中文说明：保存 activation 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleActivation}，由 {@code CompiledGatewayRelease} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by activation; its type is {@code GatewayRuleActivation}, and {@code CompiledGatewayRelease} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledGatewayRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledGatewayRelease}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayRuleActivation activation,
        /**
         * 中文说明：保存 activationJson 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code CompiledGatewayRelease} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by activation json; its type is {@code String}, and {@code CompiledGatewayRelease} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledGatewayRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledGatewayRelease}; do not couple callers to its representation when the owning type exposes an API.
         */
        String activationJson,
        /**
         * 中文说明：保存 chunkValues 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code CompiledGatewayRelease} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by chunk values; its type is {@code Map<String, String>}, and {@code CompiledGatewayRelease} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code CompiledGatewayRelease} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code CompiledGatewayRelease}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, String> chunkValues
) {

    /**
     * 中文说明：创建 {@code CompiledGatewayRelease} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code CompiledGatewayRelease} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param snapshot 参数 snapshot；parameter snapshot。
     * @param snapshotJson 参数 snapshotJson；parameter snapshot json。
     * @param activation 参数 activation；parameter activation。
     * @param activationJson 参数 activationJson；parameter activation json。
     * @param chunkValues 参数 chunkValues；parameter chunk values。
     */
    public CompiledGatewayRelease {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        snapshotJson = Objects.requireNonNull(snapshotJson, "snapshotJson");
        activation = Objects.requireNonNull(activation, "activation");
        activationJson = Objects.requireNonNull(
                activationJson,
                "activationJson"
        );
        chunkValues = Map.copyOf(Objects.requireNonNull(
                chunkValues,
                "chunkValues"
        ));
    }
}
