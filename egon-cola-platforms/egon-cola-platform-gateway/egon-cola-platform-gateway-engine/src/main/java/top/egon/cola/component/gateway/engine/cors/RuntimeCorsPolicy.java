package top.egon.cola.component.gateway.engine.cors;

import java.util.Set;

/**
 * 中文说明：{@code RuntimeCorsPolicy} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责运行时Cors策略相关的职责与边界。
 * English summary: {@code RuntimeCorsPolicy} is an immutable data carrier in the current Gateway module; it owns the runtime cors policy-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param policyId 参数 策略Id；parameter policy id。
 * @param allowedOrigins 参数 allowedOrigins；parameter allowed origins。
 * @param allowedMethods 参数 allowedMethods；parameter allowed methods。
 * @param allowedHeaders 参数 allowedHeaders；parameter allowed headers。
 * @param exposedHeaders 参数 exposedHeaders；parameter exposed headers。
 * @param allowCredentials 参数 allowCredentials；parameter allow credentials。
 * @param maxAgeSeconds 参数 maxAgeSeconds；parameter max age seconds。
 * @param enabled 参数 enabled；parameter enabled。
 */
public record RuntimeCorsPolicy(
        /**
         * 中文说明：保存 策略Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuntimeCorsPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policy id; its type is {@code String}, and {@code RuntimeCorsPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeCorsPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeCorsPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        String policyId,
        /**
         * 中文说明：保存 allowedOrigins 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code RuntimeCorsPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by allowed origins; its type is {@code Set<String>}, and {@code RuntimeCorsPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeCorsPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeCorsPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> allowedOrigins,
        /**
         * 中文说明：保存 allowedMethods 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code RuntimeCorsPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by allowed methods; its type is {@code Set<String>}, and {@code RuntimeCorsPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeCorsPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeCorsPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> allowedMethods,
        /**
         * 中文说明：保存 allowedHeaders 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code RuntimeCorsPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by allowed headers; its type is {@code Set<String>}, and {@code RuntimeCorsPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeCorsPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeCorsPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> allowedHeaders,
        /**
         * 中文说明：保存 exposedHeaders 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code RuntimeCorsPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by exposed headers; its type is {@code Set<String>}, and {@code RuntimeCorsPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeCorsPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeCorsPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> exposedHeaders,
        /**
         * 中文说明：保存 allowCredentials 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code RuntimeCorsPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by allow credentials; its type is {@code boolean}, and {@code RuntimeCorsPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeCorsPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeCorsPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean allowCredentials,
        /**
         * 中文说明：保存 maxAgeSeconds 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code RuntimeCorsPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max age seconds; its type is {@code long}, and {@code RuntimeCorsPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeCorsPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeCorsPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        long maxAgeSeconds,
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code RuntimeCorsPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code RuntimeCorsPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeCorsPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeCorsPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean enabled
) {

    /**
     * 中文说明：创建 {@code RuntimeCorsPolicy} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RuntimeCorsPolicy} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param policyId 参数 策略Id；parameter policy id。
     * @param allowedOrigins 参数 allowedOrigins；parameter allowed origins。
     * @param allowedMethods 参数 allowedMethods；parameter allowed methods。
     * @param allowedHeaders 参数 allowedHeaders；parameter allowed headers。
     * @param exposedHeaders 参数 exposedHeaders；parameter exposed headers。
     * @param allowCredentials 参数 allowCredentials；parameter allow credentials。
     * @param maxAgeSeconds 参数 maxAgeSeconds；parameter max age seconds。
     * @param enabled 参数 enabled；parameter enabled。
     */
    public RuntimeCorsPolicy {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("policyId is required");
        }
        allowedOrigins = Set.copyOf(allowedOrigins);
        allowedMethods = Set.copyOf(allowedMethods);
        allowedHeaders = Set.copyOf(allowedHeaders);
        exposedHeaders = Set.copyOf(exposedHeaders);
        if (allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException(
                    "CORS allowedOrigins must not be empty"
            );
        }
        if (allowedMethods.isEmpty()) {
            throw new IllegalArgumentException(
                    "CORS allowedMethods must not be empty"
            );
        }
        if (allowCredentials && allowedOrigins.contains("*")) {
            throw new IllegalArgumentException(
                    "credentialed CORS must not use wildcard origin"
            );
        }
        if (maxAgeSeconds < 0) {
            throw new IllegalArgumentException(
                    "CORS maxAgeSeconds must not be negative"
            );
        }
    }
}
