package top.egon.cola.component.gateway.engine.common.provider.domain;

import top.egon.cola.component.gateway.core.provider.ProviderInstance;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 中文说明：{@code ActiveHealthProbePolicy} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Active健康Probe策略相关的职责与边界。
 * English summary: {@code ActiveHealthProbePolicy} is an immutable data carrier in the current Gateway module; it owns the active health probe policy-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param enabled 参数 enabled；parameter enabled。
 * @param interval 参数 interval；parameter interval。
 * @param jitterRatio 参数 jitterRatio；parameter jitter ratio。
 * @param timeout 参数 超时；parameter timeout。
 * @param maximumConcurrency 参数 maximumConcurrency；parameter maximum concurrency。
 * @param failureThreshold 参数 failureThreshold；parameter failure threshold。
 * @param successThreshold 参数 successThreshold；parameter success threshold。
 * @param httpMethod 参数 http方法；parameter http method。
 * @param httpPath 参数 httpPath；parameter http path。
 * @param httpSuccessStatuses 参数 httpSuccessStatuses；parameter http success statuses。
 * @param rpcServiceName 参数 rpc服务Name；parameter rpc service name。
 * @param rpcConnectFallback 参数 rpcConnectFallback；parameter rpc connect fallback。
 */
public record ActiveHealthProbePolicy(
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code ActiveHealthProbePolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code ActiveHealthProbePolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthProbePolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthProbePolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean enabled,
        /**
         * 中文说明：保存 interval 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code ActiveHealthProbePolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by interval; its type is {@code Duration}, and {@code ActiveHealthProbePolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthProbePolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthProbePolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration interval,
        /**
         * 中文说明：保存 jitterRatio 对应的状态、依赖或配置值；字段类型为 {@code double}，由 {@code ActiveHealthProbePolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by jitter ratio; its type is {@code double}, and {@code ActiveHealthProbePolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthProbePolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthProbePolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        double jitterRatio,
        /**
         * 中文说明：保存 超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code ActiveHealthProbePolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by timeout; its type is {@code Duration}, and {@code ActiveHealthProbePolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthProbePolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthProbePolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration timeout,
        /**
         * 中文说明：保存 maximumConcurrency 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code ActiveHealthProbePolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by maximum concurrency; its type is {@code int}, and {@code ActiveHealthProbePolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthProbePolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthProbePolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        int maximumConcurrency,
        /**
         * 中文说明：保存 failureThreshold 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code ActiveHealthProbePolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by failure threshold; its type is {@code int}, and {@code ActiveHealthProbePolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthProbePolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthProbePolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        int failureThreshold,
        /**
         * 中文说明：保存 successThreshold 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code ActiveHealthProbePolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by success threshold; its type is {@code int}, and {@code ActiveHealthProbePolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthProbePolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthProbePolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        int successThreshold,
        /**
         * 中文说明：保存 http方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ActiveHealthProbePolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by http method; its type is {@code String}, and {@code ActiveHealthProbePolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthProbePolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthProbePolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        String httpMethod,
        /**
         * 中文说明：保存 httpPath 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ActiveHealthProbePolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by http path; its type is {@code String}, and {@code ActiveHealthProbePolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthProbePolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthProbePolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        String httpPath,
        /**
         * 中文说明：保存 httpSuccessStatuses 对应的状态、依赖或配置值；字段类型为 {@code Set<Integer>}，由 {@code ActiveHealthProbePolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by http success statuses; its type is {@code Set<Integer>}, and {@code ActiveHealthProbePolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthProbePolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthProbePolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<Integer> httpSuccessStatuses,
        /**
         * 中文说明：保存 rpc服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code ActiveHealthProbePolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by rpc service name; its type is {@code String}, and {@code ActiveHealthProbePolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthProbePolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthProbePolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        String rpcServiceName,
        /**
         * 中文说明：保存 rpcConnectFallback 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code ActiveHealthProbePolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by rpc connect fallback; its type is {@code boolean}, and {@code ActiveHealthProbePolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthProbePolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthProbePolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean rpcConnectFallback
) {

    /**
     * 中文说明：创建 {@code ActiveHealthProbePolicy} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ActiveHealthProbePolicy} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param enabled 参数 enabled；parameter enabled。
     * @param interval 参数 interval；parameter interval。
     * @param jitterRatio 参数 jitterRatio；parameter jitter ratio。
     * @param timeout 参数 超时；parameter timeout。
     * @param maximumConcurrency 参数 maximumConcurrency；parameter maximum concurrency。
     * @param failureThreshold 参数 failureThreshold；parameter failure threshold。
     * @param successThreshold 参数 successThreshold；parameter success threshold。
     * @param httpMethod 参数 http方法；parameter http method。
     * @param httpPath 参数 httpPath；parameter http path。
     * @param httpSuccessStatuses 参数 httpSuccessStatuses；parameter http success statuses。
     * @param rpcServiceName 参数 rpc服务Name；parameter rpc service name。
     * @param rpcConnectFallback 参数 rpcConnectFallback；parameter rpc connect fallback。
     */
    public ActiveHealthProbePolicy {
        if (interval == null || interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException(
                    "active health interval must be positive"
            );
        }
        if (jitterRatio < 0 || jitterRatio > 1) {
            throw new IllegalArgumentException(
                    "active health jitterRatio must be between 0 and 1"
            );
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "active health timeout must be positive"
            );
        }
        if (maximumConcurrency < 1
                || failureThreshold < 1
                || successThreshold < 1) {
            throw new IllegalArgumentException(
                    "active health limits and thresholds must be positive"
            );
        }
        httpMethod = required(httpMethod, "httpMethod")
                .toUpperCase(Locale.ROOT);
        httpPath = path(httpPath);
        httpSuccessStatuses = Set.copyOf(httpSuccessStatuses);
        if (httpSuccessStatuses.isEmpty()) {
            throw new IllegalArgumentException(
                    "httpSuccessStatuses must not be empty"
            );
        }
        if (httpSuccessStatuses.stream()
                .anyMatch(status -> status < 100 || status > 599)) {
            throw new IllegalArgumentException(
                    "httpSuccessStatuses must contain valid HTTP statuses"
            );
        }
        rpcServiceName = rpcServiceName == null
                ? ""
                : rpcServiceName.trim();
    }

    /**
     * 中文说明：执行 defaults 操作；该方法是 {@code ActiveHealthProbePolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the defaults operation; this method is the invocation entry point on {@code ActiveHealthProbePolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ActiveHealthProbePolicy.defaults(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 defaults 的处理结果；returns the result of the operation.
     */
    public static ActiveHealthProbePolicy defaults() {
        return new ActiveHealthProbePolicy(
                true,
                Duration.ofSeconds(10),
                0.2,
                Duration.ofSeconds(2),
                16,
                2,
                2,
                "GET",
                "/actuator/health",
                Set.of(200),
                "",
                true
        );
    }

    /**
     * 中文说明：执行 http方法 操作；该方法是 {@code ActiveHealthProbePolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the http method operation; this method is the invocation entry point on {@code ActiveHealthProbePolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ActiveHealthProbePolicy.httpMethod(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param instance 参数 instance；parameter instance。
     * @return 返回 http方法 的处理结果；returns the result of the operation.
     */
    public String httpMethod(ProviderInstance instance) {
        return instance.metadata().getOrDefault(
                "gateway.health.method",
                httpMethod
        ).toUpperCase(Locale.ROOT);
    }

    /**
     * 中文说明：执行 httpPath 操作；该方法是 {@code ActiveHealthProbePolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the http path operation; this method is the invocation entry point on {@code ActiveHealthProbePolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ActiveHealthProbePolicy.httpPath(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param instance 参数 instance；parameter instance。
     * @return 返回 httpPath 的处理结果；returns the result of the operation.
     */
    public String httpPath(ProviderInstance instance) {
        return path(instance.metadata().getOrDefault(
                "gateway.health.path",
                httpPath
        ));
    }

    /**
     * 中文说明：执行 httpSuccessStatuses 操作；该方法是 {@code ActiveHealthProbePolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the http success statuses operation; this method is the invocation entry point on {@code ActiveHealthProbePolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ActiveHealthProbePolicy.httpSuccessStatuses(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param instance 参数 instance；parameter instance。
     * @return 返回 httpSuccessStatuses 的处理结果；returns the result of the operation.
     */
    public Set<Integer> httpSuccessStatuses(ProviderInstance instance) {
        String configured = instance.metadata().get(
                "gateway.health.success-statuses"
        );
        if (configured == null || configured.isBlank()) {
            return httpSuccessStatuses;
        }
        Set<Integer> statuses = new LinkedHashSet<>();
        for (String value : configured.split(",")) {
            int status = Integer.parseInt(value.trim());
            if (status < 100 || status > 599) {
                throw new IllegalArgumentException(
                        "gateway health status must be a valid HTTP status"
                );
            }
            statuses.add(status);
        }
        return Set.copyOf(statuses);
    }

    /**
     * 中文说明：执行 rpc服务Name 操作；该方法是 {@code ActiveHealthProbePolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the rpc service name operation; this method is the invocation entry point on {@code ActiveHealthProbePolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ActiveHealthProbePolicy.rpcServiceName(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param instance 参数 instance；parameter instance。
     * @return 返回 rpc服务Name 的处理结果；returns the result of the operation.
     */
    public String rpcServiceName(ProviderInstance instance) {
        return instance.metadata().getOrDefault(
                "gateway.health.rpc-service",
                rpcServiceName
        );
    }

    /**
     * 中文说明：执行 path 操作；该方法是 {@code ActiveHealthProbePolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the path operation; this method is the invocation entry point on {@code ActiveHealthProbePolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ActiveHealthProbePolicy.path(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 path 的处理结果；returns the result of the operation.
     */
    private static String path(String value) {
        String result = required(value, "httpPath");
        if (!result.startsWith("/") || result.startsWith("//")) {
            throw new IllegalArgumentException(
                    "active health HTTP path must be absolute-path only"
            );
        }
        return result;
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code ActiveHealthProbePolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code ActiveHealthProbePolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ActiveHealthProbePolicy.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
