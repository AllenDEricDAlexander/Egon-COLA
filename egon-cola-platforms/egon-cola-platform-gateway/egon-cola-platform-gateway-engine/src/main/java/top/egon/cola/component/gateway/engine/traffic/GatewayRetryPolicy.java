package top.egon.cola.component.gateway.engine.traffic;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：{@code GatewayRetryPolicy} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关重试策略相关的职责与边界。
 * English summary: {@code GatewayRetryPolicy} is an immutable data carrier in the current Gateway module; it owns the gateway retry policy-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param enabled 参数 enabled；parameter enabled。
 * @param maxAttempts 参数 maxAttempts；parameter max attempts。
 * @param initialBackoff 参数 initialBackoff；parameter initial backoff。
 * @param maximumBackoff 参数 maximumBackoff；parameter maximum backoff。
 * @param multiplier 参数 multiplier；parameter multiplier。
 * @param minimumAttemptBudget 参数 minimumAttemptBudget；parameter minimum attempt budget。
 * @param retryableHttpStatuses 参数 retryableHttpStatuses；parameter retryable http statuses。
 * @param retryableRpcStatuses 参数 retryableRpcStatuses；parameter retryable rpc statuses。
 */
public record GatewayRetryPolicy(
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayRetryPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code GatewayRetryPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRetryPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRetryPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean enabled,
        /**
         * 中文说明：保存 maxAttempts 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayRetryPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max attempts; its type is {@code int}, and {@code GatewayRetryPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRetryPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRetryPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        int maxAttempts,
        /**
         * 中文说明：保存 initialBackoff 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayRetryPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by initial backoff; its type is {@code Duration}, and {@code GatewayRetryPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRetryPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRetryPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration initialBackoff,
        /**
         * 中文说明：保存 maximumBackoff 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayRetryPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by maximum backoff; its type is {@code Duration}, and {@code GatewayRetryPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRetryPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRetryPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration maximumBackoff,
        /**
         * 中文说明：保存 multiplier 对应的状态、依赖或配置值；字段类型为 {@code double}，由 {@code GatewayRetryPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by multiplier; its type is {@code double}, and {@code GatewayRetryPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRetryPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRetryPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        double multiplier,
        /**
         * 中文说明：保存 minimumAttemptBudget 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayRetryPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by minimum attempt budget; its type is {@code Duration}, and {@code GatewayRetryPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRetryPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRetryPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration minimumAttemptBudget,
        /**
         * 中文说明：保存 retryableHttpStatuses 对应的状态、依赖或配置值；字段类型为 {@code Set<Integer>}，由 {@code GatewayRetryPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by retryable http statuses; its type is {@code Set<Integer>}, and {@code GatewayRetryPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRetryPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRetryPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<Integer> retryableHttpStatuses,
        /**
         * 中文说明：保存 retryableRpcStatuses 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code GatewayRetryPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by retryable rpc statuses; its type is {@code Set<String>}, and {@code GatewayRetryPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayRetryPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayRetryPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> retryableRpcStatuses
) {

    /**
     * 中文说明：创建 {@code GatewayRetryPolicy} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayRetryPolicy} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param enabled 参数 enabled；parameter enabled。
     * @param maxAttempts 参数 maxAttempts；parameter max attempts。
     * @param initialBackoff 参数 initialBackoff；parameter initial backoff。
     * @param maximumBackoff 参数 maximumBackoff；parameter maximum backoff。
     * @param multiplier 参数 multiplier；parameter multiplier。
     * @param minimumAttemptBudget 参数 minimumAttemptBudget；parameter minimum attempt budget。
     * @param retryableHttpStatuses 参数 retryableHttpStatuses；parameter retryable http statuses。
     * @param retryableRpcStatuses 参数 retryableRpcStatuses；parameter retryable rpc statuses。
     */
    public GatewayRetryPolicy {
        if (maxAttempts < 1 || multiplier < 1) {
            throw new IllegalArgumentException("invalid retry bounds");
        }
        initialBackoff = nonNegative(initialBackoff, "initialBackoff");
        maximumBackoff = nonNegative(maximumBackoff, "maximumBackoff");
        minimumAttemptBudget = positive(
                minimumAttemptBudget,
                "minimumAttemptBudget"
        );
        retryableHttpStatuses = Set.copyOf(Objects.requireNonNull(
                retryableHttpStatuses,
                "retryableHttpStatuses"
        ));
        retryableRpcStatuses = Set.copyOf(Objects.requireNonNull(
                retryableRpcStatuses,
                "retryableRpcStatuses"
        ));
        if (retryableHttpStatuses.stream()
                .anyMatch(status -> status < 500 || status > 599)) {
            throw new IllegalArgumentException(
                    "retryable HTTP statuses must be 5xx"
            );
        }
        if (maximumBackoff.compareTo(initialBackoff) < 0) {
            throw new IllegalArgumentException(
                    "maximumBackoff must not be shorter than initial"
            );
        }
        if (!enabled && maxAttempts != 1) {
            throw new IllegalArgumentException(
                    "disabled retry policy must use one attempt"
            );
        }
    }

    /**
     * 中文说明：执行 disabled 操作；该方法是 {@code GatewayRetryPolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the disabled operation; this method is the invocation entry point on {@code GatewayRetryPolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRetryPolicy.disabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 disabled 的处理结果；returns the result of the operation.
     */
    public static GatewayRetryPolicy disabled() {
        return new GatewayRetryPolicy(
                false,
                1,
                Duration.ZERO,
                Duration.ZERO,
                1,
                Duration.ofMillis(1),
                Set.of(),
                Set.of()
        );
    }

    /**
     * 中文说明：执行 retryableHttpStatus 操作；该方法是 {@code GatewayRetryPolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the retryable http status operation; this method is the invocation entry point on {@code GatewayRetryPolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRetryPolicy.retryableHttpStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @return 返回 retryableHttpStatus 的处理结果；returns the result of the operation.
     */
    public boolean retryableHttpStatus(int status) {
        return retryableHttpStatuses.contains(status);
    }

    /**
     * 中文说明：执行 retryableRpcStatus 操作；该方法是 {@code GatewayRetryPolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the retryable rpc status operation; this method is the invocation entry point on {@code GatewayRetryPolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRetryPolicy.retryableRpcStatus(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param status 参数 status；parameter status。
     * @return 返回 retryableRpcStatus 的处理结果；returns the result of the operation.
     */
    public boolean retryableRpcStatus(String status) {
        return retryableRpcStatuses.contains(status);
    }

    /**
     * 中文说明：执行 backoff 操作；该方法是 {@code GatewayRetryPolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the backoff operation; this method is the invocation entry point on {@code GatewayRetryPolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRetryPolicy.backoff(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param completedAttempts 参数 completedAttempts；parameter completed attempts。
     * @return 返回 backoff 的处理结果；returns the result of the operation.
     */
    public Duration backoff(int completedAttempts) {
        double factor = Math.pow(multiplier, Math.max(0, completedAttempts - 1));
        long requested;
        try {
            requested = Math.multiplyExact(
                    initialBackoff.toNanos(),
                    Math.max(1L, Math.round(factor))
            );
        } catch (ArithmeticException overflow) {
            requested = maximumBackoff.toNanos();
        }
        return Duration.ofNanos(Math.min(
                requested,
                maximumBackoff.toNanos()
        ));
    }

    /**
     * 中文说明：执行 nonNegative 操作；该方法是 {@code GatewayRetryPolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the non negative operation; this method is the invocation entry point on {@code GatewayRetryPolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRetryPolicy.nonNegative(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 nonNegative 的处理结果；returns the result of the operation.
     */
    private static Duration nonNegative(Duration value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isNegative()) {
            throw new IllegalArgumentException(
                    field + " must not be negative"
            );
        }
        return value;
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code GatewayRetryPolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code GatewayRetryPolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayRetryPolicy.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 positive 的处理结果；returns the result of the operation.
     */
    private static Duration positive(Duration value, String field) {
        nonNegative(value, field);
        if (value.isZero()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
