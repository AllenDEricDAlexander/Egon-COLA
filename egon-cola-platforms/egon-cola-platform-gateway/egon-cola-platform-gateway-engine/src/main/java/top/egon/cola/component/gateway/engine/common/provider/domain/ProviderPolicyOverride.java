package top.egon.cola.component.gateway.engine.common.provider.domain;

import java.util.Set;

/**
 * 中文说明：{@code ProviderPolicyOverride} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责提供方策略Override相关的职责与边界。
 * English summary: {@code ProviderPolicyOverride} is an immutable data carrier in the current Gateway module; it owns the provider policy override-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param enabled 参数 enabled；parameter enabled。
 * @param weight 参数 weight；parameter weight。
 * @param zone 参数 zone；parameter zone。
 * @param region 参数 region；parameter region。
 * @param tags 参数 tags；parameter tags。
 */
public record ProviderPolicyOverride(
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code Boolean}，由 {@code ProviderPolicyOverride} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code Boolean}, and {@code ProviderPolicyOverride} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderPolicyOverride} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderPolicyOverride}; do not couple callers to its representation when the owning type exposes an API.
         */
        Boolean enabled,
        /**
         * 中文说明：保存 weight 对应的状态、依赖或配置值；字段类型为 {@code Integer}，由 {@code ProviderPolicyOverride} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by weight; its type is {@code Integer}, and {@code ProviderPolicyOverride} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderPolicyOverride} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderPolicyOverride}; do not couple callers to its representation when the owning type exposes an API.
         */
        Integer weight,
        /**
         * 中文说明：保存 zone 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ProviderPolicyOverride} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by zone; its type is {@code String}, and {@code ProviderPolicyOverride} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderPolicyOverride} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderPolicyOverride}; do not couple callers to its representation when the owning type exposes an API.
         */
        String zone,
        /**
         * 中文说明：保存 region 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ProviderPolicyOverride} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by region; its type is {@code String}, and {@code ProviderPolicyOverride} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderPolicyOverride} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderPolicyOverride}; do not couple callers to its representation when the owning type exposes an API.
         */
        String region,
        /**
         * 中文说明：保存 tags 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code ProviderPolicyOverride} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tags; its type is {@code Set<String>}, and {@code ProviderPolicyOverride} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ProviderPolicyOverride} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ProviderPolicyOverride}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> tags
) {

    /**
     * 中文说明：创建 {@code ProviderPolicyOverride} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ProviderPolicyOverride} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param enabled 参数 enabled；parameter enabled。
     * @param weight 参数 weight；parameter weight。
     * @param zone 参数 zone；parameter zone。
     * @param region 参数 region；parameter region。
     * @param tags 参数 tags；parameter tags。
     */
    public ProviderPolicyOverride {
        if (weight != null && (weight < 0 || weight > 10000)) {
            throw new IllegalArgumentException(
                    "override weight must be between 0 and 10000"
            );
        }
        zone = normalized(zone);
        region = normalized(region);
        tags = tags == null
                ? null
                : tags.stream()
                .map(ProviderPolicyOverride::normalizedRequired)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /**
     * 中文说明：执行 none 操作；该方法是 {@code ProviderPolicyOverride} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the none operation; this method is the invocation entry point on {@code ProviderPolicyOverride} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderPolicyOverride.none(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 none 的处理结果；returns the result of the operation.
     */
    public static ProviderPolicyOverride none() {
        return new ProviderPolicyOverride(null, null, null, null, null);
    }

    /**
     * 中文说明：执行 normalized 操作；该方法是 {@code ProviderPolicyOverride} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the normalized operation; this method is the invocation entry point on {@code ProviderPolicyOverride} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderPolicyOverride.normalized(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 normalized 的处理结果；returns the result of the operation.
     */
    private static String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 中文说明：执行 normalizedRequired 操作；该方法是 {@code ProviderPolicyOverride} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the normalized required operation; this method is the invocation entry point on {@code ProviderPolicyOverride} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ProviderPolicyOverride.normalizedRequired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 normalizedRequired 的处理结果；returns the result of the operation.
     */
    private static String normalizedRequired(String value) {
        String normalized = normalized(value);
        if (normalized == null) {
            throw new IllegalArgumentException("provider tag is required");
        }
        return normalized;
    }
}
