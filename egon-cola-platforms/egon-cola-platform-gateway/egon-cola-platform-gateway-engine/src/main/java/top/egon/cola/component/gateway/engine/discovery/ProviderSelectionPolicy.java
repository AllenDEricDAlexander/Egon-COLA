package top.egon.cola.component.gateway.engine.discovery;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：{@code ProviderSelectionPolicy} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责提供方Selection策略相关的职责与边界。
 * English summary: {@code ProviderSelectionPolicy} is an immutable data carrier in the current Gateway module; it owns the provider selection policy-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param serviceEnabled 参数 服务Enabled；parameter service enabled。
 * @param secureRequired 参数 secureRequired；parameter secure required。
 * @param requiredZone 参数 requiredZone；parameter required zone。
 * @param requiredRegion 参数 requiredRegion；parameter required region。
 * @param requiredTags 参数 requiredTags；parameter required tags。
 * @param serviceOverride 参数 服务Override；parameter service override。
 * @param instanceOverrides 参数 instanceOverrides；parameter instance overrides。
 */
public record ProviderSelectionPolicy(
        /**
         * 中文说明：保存 服务Enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code ProviderSelectionPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by service enabled; its type is {@code boolean}, and {@code ProviderSelectionPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderSelectionPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderSelectionPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean serviceEnabled,
        /**
         * 中文说明：保存 secureRequired 对应的状态、依赖或配置值；字段类型为 {@code Boolean}，由 {@code ProviderSelectionPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by secure required; its type is {@code Boolean}, and {@code ProviderSelectionPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderSelectionPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderSelectionPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Boolean secureRequired,
        /**
         * 中文说明：保存 requiredZone 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ProviderSelectionPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by required zone; its type is {@code String}, and {@code ProviderSelectionPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderSelectionPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderSelectionPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        String requiredZone,
        /**
         * 中文说明：保存 requiredRegion 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ProviderSelectionPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by required region; its type is {@code String}, and {@code ProviderSelectionPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderSelectionPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderSelectionPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        String requiredRegion,
        /**
         * 中文说明：保存 requiredTags 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code ProviderSelectionPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by required tags; its type is {@code Set<String>}, and {@code ProviderSelectionPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderSelectionPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderSelectionPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> requiredTags,
        /**
         * 中文说明：保存 服务Override 对应的状态、依赖或配置值；字段类型为 {@code ProviderPolicyOverride}，由 {@code ProviderSelectionPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by service override; its type is {@code ProviderPolicyOverride}, and {@code ProviderSelectionPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderSelectionPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderSelectionPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        ProviderPolicyOverride serviceOverride,
        /**
         * 中文说明：保存 instanceOverrides 对应的状态、依赖或配置值；字段类型为 {@code Map<String, ProviderPolicyOverride>}，由 {@code ProviderSelectionPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by instance overrides; its type is {@code Map<String, ProviderPolicyOverride>}, and {@code ProviderSelectionPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderSelectionPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderSelectionPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, ProviderPolicyOverride> instanceOverrides
) {

    /**
     * 中文说明：创建 {@code ProviderSelectionPolicy} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ProviderSelectionPolicy} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param serviceEnabled 参数 服务Enabled；parameter service enabled。
     * @param secureRequired 参数 secureRequired；parameter secure required。
     * @param requiredZone 参数 requiredZone；parameter required zone。
     * @param requiredRegion 参数 requiredRegion；parameter required region。
     * @param requiredTags 参数 requiredTags；parameter required tags。
     * @param serviceOverride 参数 服务Override；parameter service override。
     * @param instanceOverrides 参数 instanceOverrides；parameter instance overrides。
     */
    public ProviderSelectionPolicy {
        requiredZone = normalized(requiredZone);
        requiredRegion = normalized(requiredRegion);
        requiredTags = requiredTags == null
                ? Set.of()
                : Set.copyOf(requiredTags);
        serviceOverride = Objects.requireNonNull(
                serviceOverride,
                "serviceOverride"
        );
        Map<String, ProviderPolicyOverride> copy = new LinkedHashMap<>();
        Objects.requireNonNull(instanceOverrides, "instanceOverrides")
                .forEach((instanceId, override) -> copy.put(
                        required(instanceId),
                        Objects.requireNonNull(override, "instance override")
                ));
        instanceOverrides = Map.copyOf(copy);
    }

    /**
     * 中文说明：执行 defaults 操作；该方法是 {@code ProviderSelectionPolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the defaults operation; this method is the invocation entry point on {@code ProviderSelectionPolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderSelectionPolicy.defaults(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param secure 参数 secure；parameter secure。
     * @return 返回 defaults 的处理结果；returns the result of the operation.
     */
    public static ProviderSelectionPolicy defaults(boolean secure) {
        return new ProviderSelectionPolicy(
                true,
                secure,
                null,
                null,
                Set.of(),
                ProviderPolicyOverride.none(),
                Map.of()
        );
    }

    /**
     * 中文说明：执行 normalized 操作；该方法是 {@code ProviderSelectionPolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the normalized operation; this method is the invocation entry point on {@code ProviderSelectionPolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderSelectionPolicy.normalized(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 normalized 的处理结果；returns the result of the operation.
     */
    private static String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code ProviderSelectionPolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code ProviderSelectionPolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderSelectionPolicy.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value) {
        String normalized = normalized(value);
        if (normalized == null) {
            throw new IllegalArgumentException("instanceId is required");
        }
        return normalized;
    }
}
