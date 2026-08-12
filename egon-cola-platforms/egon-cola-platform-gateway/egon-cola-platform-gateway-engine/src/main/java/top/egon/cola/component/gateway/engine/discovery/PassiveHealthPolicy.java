package top.egon.cola.component.gateway.engine.discovery;

import java.time.Duration;
import java.util.Objects;

/**
 * 中文说明：{@code PassiveHealthPolicy} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Passive健康策略相关的职责与边界。
 * English summary: {@code PassiveHealthPolicy} is an immutable data carrier in the current Gateway module; it owns the passive health policy-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param consecutiveFailureThreshold 参数 consecutiveFailureThreshold；parameter consecutive failure threshold。
 * @param minimumSamples 参数 minimumSamples；parameter minimum samples。
 * @param failureRateThreshold 参数 failureRateThreshold；parameter failure rate threshold。
 * @param window 参数 window；parameter window。
 * @param baseEjectionDuration 参数 baseEjectionDuration；parameter base ejection duration。
 * @param maximumEjectionDuration 参数 maximumEjectionDuration；parameter maximum ejection duration。
 */
public record PassiveHealthPolicy(
        /**
         * 中文说明：保存 consecutiveFailureThreshold 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code PassiveHealthPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by consecutive failure threshold; its type is {@code int}, and {@code PassiveHealthPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code PassiveHealthPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        int consecutiveFailureThreshold,
        /**
         * 中文说明：保存 minimumSamples 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code PassiveHealthPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by minimum samples; its type is {@code int}, and {@code PassiveHealthPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code PassiveHealthPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        int minimumSamples,
        /**
         * 中文说明：保存 failureRateThreshold 对应的状态、依赖或配置值；字段类型为 {@code double}，由 {@code PassiveHealthPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by failure rate threshold; its type is {@code double}, and {@code PassiveHealthPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code PassiveHealthPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        double failureRateThreshold,
        /**
         * 中文说明：保存 window 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code PassiveHealthPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by window; its type is {@code Duration}, and {@code PassiveHealthPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code PassiveHealthPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration window,
        /**
         * 中文说明：保存 baseEjectionDuration 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code PassiveHealthPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by base ejection duration; its type is {@code Duration}, and {@code PassiveHealthPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code PassiveHealthPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration baseEjectionDuration,
        /**
         * 中文说明：保存 maximumEjectionDuration 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code PassiveHealthPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by maximum ejection duration; its type is {@code Duration}, and {@code PassiveHealthPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code PassiveHealthPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration maximumEjectionDuration
) {

    /**
     * 中文说明：创建 {@code PassiveHealthPolicy} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code PassiveHealthPolicy} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param consecutiveFailureThreshold 参数 consecutiveFailureThreshold；parameter consecutive failure threshold。
     * @param minimumSamples 参数 minimumSamples；parameter minimum samples。
     * @param failureRateThreshold 参数 failureRateThreshold；parameter failure rate threshold。
     * @param window 参数 window；parameter window。
     * @param baseEjectionDuration 参数 baseEjectionDuration；parameter base ejection duration。
     * @param maximumEjectionDuration 参数 maximumEjectionDuration；parameter maximum ejection duration。
     */
    public PassiveHealthPolicy {
        if (consecutiveFailureThreshold < 1 || minimumSamples < 1) {
            throw new IllegalArgumentException(
                    "passive health thresholds must be positive"
            );
        }
        if (failureRateThreshold <= 0 || failureRateThreshold > 1) {
            throw new IllegalArgumentException(
                    "failureRateThreshold must be in (0, 1]"
            );
        }
        window = positive(window, "window");
        baseEjectionDuration = positive(
                baseEjectionDuration,
                "baseEjectionDuration"
        );
        maximumEjectionDuration = positive(
                maximumEjectionDuration,
                "maximumEjectionDuration"
        );
        if (maximumEjectionDuration.compareTo(baseEjectionDuration) < 0) {
            throw new IllegalArgumentException(
                    "maximum ejection duration must not be shorter than base"
            );
        }
    }

    /**
     * 中文说明：执行 defaults 操作；该方法是 {@code PassiveHealthPolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the defaults operation; this method is the invocation entry point on {@code PassiveHealthPolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code PassiveHealthPolicy.defaults(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 defaults 的处理结果；returns the result of the operation.
     */
    public static PassiveHealthPolicy defaults() {
        return new PassiveHealthPolicy(
                3,
                20,
                0.5,
                Duration.ofSeconds(30),
                Duration.ofSeconds(5),
                Duration.ofMinutes(1)
        );
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code PassiveHealthPolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code PassiveHealthPolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code PassiveHealthPolicy.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 positive 的处理结果；returns the result of the operation.
     */
    private static Duration positive(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
