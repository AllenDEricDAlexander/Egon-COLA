package top.egon.cola.component.gateway.engine.discovery;

import top.egon.cola.component.gateway.core.provider.ProviderHealthState;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中文说明：{@code ActiveHealthTracker} 是类型，位于当前 Gateway 模块的相关包中，负责Active健康Tracker相关的职责与边界。
 * English summary: {@code ActiveHealthTracker} is a type in the current Gateway module; it owns the active health tracker-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class ActiveHealthTracker {

    /**
     * 中文说明：保存 failureThreshold 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code ActiveHealthTracker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by failure threshold; its type is {@code int}, and {@code ActiveHealthTracker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ActiveHealthTracker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthTracker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final int failureThreshold;

    /**
     * 中文说明：保存 successThreshold 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code ActiveHealthTracker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by success threshold; its type is {@code int}, and {@code ActiveHealthTracker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ActiveHealthTracker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthTracker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final int successThreshold;

    /**
     * 中文说明：保存 states 对应的状态、依赖或配置值；字段类型为 {@code Map<String, State>}，由 {@code ActiveHealthTracker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by states; its type is {@code Map<String, State>}, and {@code ActiveHealthTracker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code ActiveHealthTracker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthTracker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, State> states = new ConcurrentHashMap<>();

    /**
     * 中文说明：创建 {@code ActiveHealthTracker} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code ActiveHealthTracker} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param failureThreshold 参数 failureThreshold；parameter failure threshold。
     * @param successThreshold 参数 successThreshold；parameter success threshold。
     */
    public ActiveHealthTracker(
            int failureThreshold,
            int successThreshold) {
        if (failureThreshold < 1 || successThreshold < 1) {
            throw new IllegalArgumentException(
                    "active health thresholds must be positive"
            );
        }
        this.failureThreshold = failureThreshold;
        this.successThreshold = successThreshold;
    }

    /**
     * 中文说明：执行 record 操作；该方法是 {@code ActiveHealthTracker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the record operation; this method is the invocation entry point on {@code ActiveHealthTracker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ActiveHealthTracker.record(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param runtimeIdentity 参数 运行时身份；parameter runtime identity。
     * @param successful 参数 successful；parameter successful。
     */
    public void record(String runtimeIdentity, boolean successful) {
        states.computeIfAbsent(
                required(runtimeIdentity),
                ignored -> new State()
        ).record(successful, failureThreshold, successThreshold);
    }

    /**
     * 中文说明：执行 eligible 操作；该方法是 {@code ActiveHealthTracker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the eligible operation; this method is the invocation entry point on {@code ActiveHealthTracker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ActiveHealthTracker.eligible(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param runtimeIdentity 参数 运行时身份；parameter runtime identity。
     * @return 返回 eligible 的处理结果；returns the result of the operation.
     */
    public boolean eligible(String runtimeIdentity) {
        return snapshot(runtimeIdentity).state()
                != ProviderHealthState.UNHEALTHY;
    }

    /**
     * 中文说明：执行 snapshot 操作；该方法是 {@code ActiveHealthTracker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the snapshot operation; this method is the invocation entry point on {@code ActiveHealthTracker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ActiveHealthTracker.snapshot(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param runtimeIdentity 参数 运行时身份；parameter runtime identity。
     * @return 返回 snapshot 的处理结果；returns the result of the operation.
     */
    public Snapshot snapshot(String runtimeIdentity) {
        State state = states.get(required(runtimeIdentity));
        return state == null ? Snapshot.unknown() : state.snapshot();
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code ActiveHealthTracker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code ActiveHealthTracker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code ActiveHealthTracker.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "runtimeIdentity is required"
            );
        }
        return value;
    }

    /**
     * 中文说明：{@code State} 是类型，位于当前 Gateway 模块的相关包中，负责State相关的职责与边界。
     * English summary: {@code State} is a type in the current Gateway module; it owns the state-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class State {

        /**
         * 中文说明：保存 state 对应的状态、依赖或配置值；字段类型为 {@code ProviderHealthState}，由 {@code ActiveHealthTracker.State} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by state; its type is {@code ProviderHealthState}, and {@code ActiveHealthTracker.State} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthTracker.State} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthTracker.State}; do not couple callers to its representation when the owning type exposes an API.
         */
        private ProviderHealthState state = ProviderHealthState.UNKNOWN;

        /**
         * 中文说明：保存 consecutiveFailures 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code ActiveHealthTracker.State} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by consecutive failures; its type is {@code int}, and {@code ActiveHealthTracker.State} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthTracker.State} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthTracker.State}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int consecutiveFailures;

        /**
         * 中文说明：保存 consecutiveSuccesses 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code ActiveHealthTracker.State} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by consecutive successes; its type is {@code int}, and {@code ActiveHealthTracker.State} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthTracker.State} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthTracker.State}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int consecutiveSuccesses;

        /**
         * 中文说明：保存 lastProbeAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code ActiveHealthTracker.State} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by last probe at; its type is {@code Instant}, and {@code ActiveHealthTracker.State} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code ActiveHealthTracker.State} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthTracker.State}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Instant lastProbeAt;

        /**
         * 中文说明：执行 record 操作；该方法是 {@code ActiveHealthTracker.State} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the record operation; this method is the invocation entry point on {@code ActiveHealthTracker.State} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ActiveHealthTracker.State.record(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param successful 参数 successful；parameter successful。
         * @param failureThreshold 参数 failureThreshold；parameter failure threshold。
         * @param successThreshold 参数 successThreshold；parameter success threshold。
         */
        private synchronized void record(
                boolean successful,
                int failureThreshold,
                int successThreshold) {
            lastProbeAt = Instant.now();
            if (successful) {
                consecutiveFailures = 0;
                consecutiveSuccesses++;
                if (consecutiveSuccesses >= successThreshold) {
                    state = ProviderHealthState.HEALTHY;
                }
            } else {
                consecutiveSuccesses = 0;
                consecutiveFailures++;
                if (consecutiveFailures >= failureThreshold) {
                    state = ProviderHealthState.UNHEALTHY;
                }
            }
        }

        /**
         * 中文说明：执行 snapshot 操作；该方法是 {@code ActiveHealthTracker.State} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the snapshot operation; this method is the invocation entry point on {@code ActiveHealthTracker.State} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ActiveHealthTracker.State.snapshot(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 snapshot 的处理结果；returns the result of the operation.
         */
        private synchronized Snapshot snapshot() {
            return new Snapshot(
                    state,
                    consecutiveFailures,
                    consecutiveSuccesses,
                    lastProbeAt
            );
        }
    }

    /**
     * 中文说明：{@code Snapshot} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Snapshot相关的职责与边界。
     * English summary: {@code Snapshot} is an immutable data carrier in the current Gateway module; it owns the snapshot-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param state 参数 state；parameter state。
     * @param consecutiveFailures 参数 consecutiveFailures；parameter consecutive failures。
     * @param consecutiveSuccesses 参数 consecutiveSuccesses；parameter consecutive successes。
     * @param lastProbeAt 参数 lastProbeAt；parameter last probe at。
     */
    public record Snapshot(
            /**
             * 中文说明：保存 state 对应的状态、依赖或配置值；字段类型为 {@code ProviderHealthState}，由 {@code ActiveHealthTracker.Snapshot} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by state; its type is {@code ProviderHealthState}, and {@code ActiveHealthTracker.Snapshot} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ActiveHealthTracker.Snapshot} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthTracker.Snapshot}; do not couple callers to its representation when the owning type exposes an API.
             */
            ProviderHealthState state,
            /**
             * 中文说明：保存 consecutiveFailures 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code ActiveHealthTracker.Snapshot} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by consecutive failures; its type is {@code int}, and {@code ActiveHealthTracker.Snapshot} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ActiveHealthTracker.Snapshot} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthTracker.Snapshot}; do not couple callers to its representation when the owning type exposes an API.
             */
            int consecutiveFailures,
            /**
             * 中文说明：保存 consecutiveSuccesses 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code ActiveHealthTracker.Snapshot} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by consecutive successes; its type is {@code int}, and {@code ActiveHealthTracker.Snapshot} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ActiveHealthTracker.Snapshot} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthTracker.Snapshot}; do not couple callers to its representation when the owning type exposes an API.
             */
            int consecutiveSuccesses,
            /**
             * 中文说明：保存 lastProbeAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code ActiveHealthTracker.Snapshot} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by last probe at; its type is {@code Instant}, and {@code ActiveHealthTracker.Snapshot} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code ActiveHealthTracker.Snapshot} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code ActiveHealthTracker.Snapshot}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant lastProbeAt
    ) {

        /**
         * 中文说明：执行 unknown 操作；该方法是 {@code ActiveHealthTracker.Snapshot} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the unknown operation; this method is the invocation entry point on {@code ActiveHealthTracker.Snapshot} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code ActiveHealthTracker.Snapshot.unknown(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 unknown 的处理结果；returns the result of the operation.
         */
        private static Snapshot unknown() {
            return new Snapshot(
                    ProviderHealthState.UNKNOWN,
                    0,
                    0,
                    null
            );
        }
    }
}
