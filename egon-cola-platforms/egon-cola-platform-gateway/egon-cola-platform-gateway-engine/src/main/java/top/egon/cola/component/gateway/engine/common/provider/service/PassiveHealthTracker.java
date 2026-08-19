package top.egon.cola.component.gateway.engine.common.provider.service;

import top.egon.cola.component.gateway.engine.common.provider.domain.PassiveHealthPolicy;
import top.egon.cola.component.gateway.engine.common.provider.domain.ProviderCallOutcome;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中文说明：{@code PassiveHealthTracker} 是类型，位于当前 Gateway 模块的相关包中，负责Passive健康Tracker相关的职责与边界。
 * English summary: {@code PassiveHealthTracker} is a type in the current Gateway module; it owns the passive health tracker-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class PassiveHealthTracker
        implements ProviderCallOutcomeRecorder {

    /**
     * 中文说明：保存 策略 对应的状态、依赖或配置值；字段类型为 {@code PassiveHealthPolicy}，由 {@code PassiveHealthTracker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by policy; its type is {@code PassiveHealthPolicy}, and {@code PassiveHealthTracker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code PassiveHealthTracker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthTracker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final PassiveHealthPolicy policy;

    /**
     * 中文说明：保存 clock 对应的状态、依赖或配置值；字段类型为 {@code Clock}，由 {@code PassiveHealthTracker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by clock; its type is {@code Clock}, and {@code PassiveHealthTracker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code PassiveHealthTracker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthTracker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Clock clock;

    /**
     * 中文说明：保存 states 对应的状态、依赖或配置值；字段类型为 {@code Map<String, InstanceState>}，由 {@code PassiveHealthTracker} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by states; its type is {@code Map<String, InstanceState>}, and {@code PassiveHealthTracker} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code PassiveHealthTracker} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthTracker}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, InstanceState> states = new ConcurrentHashMap<>();

    /**
     * 中文说明：创建 {@code PassiveHealthTracker} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code PassiveHealthTracker} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param policy 参数 策略；parameter policy。
     * @param clock 参数 clock；parameter clock。
     */
    public PassiveHealthTracker(PassiveHealthPolicy policy, Clock clock) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 中文说明：执行 eligible 操作；该方法是 {@code PassiveHealthTracker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the eligible operation; this method is the invocation entry point on {@code PassiveHealthTracker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code PassiveHealthTracker.eligible(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param runtimeIdentity 参数 运行时身份；parameter runtime identity。
     * @return 返回 eligible 的处理结果；returns the result of the operation.
     */
    public boolean eligible(String runtimeIdentity) {
        InstanceState state = states.get(required(runtimeIdentity));
        return state == null || state.eligible(clock.instant());
    }

    /**
     * 中文说明：执行 record 操作；该方法是 {@code PassiveHealthTracker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the record operation; this method is the invocation entry point on {@code PassiveHealthTracker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code PassiveHealthTracker.record(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param runtimeIdentity 参数 运行时身份；parameter runtime identity。
     * @param outcome 参数 outcome；parameter outcome。
     */
    @Override
    public void record(
            String runtimeIdentity,
            ProviderCallOutcome outcome) {
        Objects.requireNonNull(outcome, "outcome");
        InstanceState state = states.computeIfAbsent(
                required(runtimeIdentity),
                ignored -> new InstanceState()
        );
        state.record(outcome, clock.instant(), policy);
    }

    /**
     * 中文说明：执行 snapshot 操作；该方法是 {@code PassiveHealthTracker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the snapshot operation; this method is the invocation entry point on {@code PassiveHealthTracker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code PassiveHealthTracker.snapshot(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param runtimeIdentity 参数 运行时身份；parameter runtime identity。
     * @return 返回 snapshot 的处理结果；returns the result of the operation.
     */
    public PassiveHealthSnapshot snapshot(String runtimeIdentity) {
        InstanceState state = states.get(required(runtimeIdentity));
        return state == null
                ? PassiveHealthSnapshot.healthy()
                : state.snapshot(clock.instant());
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code PassiveHealthTracker} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code PassiveHealthTracker} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code PassiveHealthTracker.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("runtimeIdentity is required");
        }
        return value;
    }

    /**
     * 中文说明：{@code InstanceState} 是类型，位于当前 Gateway 模块的相关包中，负责InstanceState相关的职责与边界。
     * English summary: {@code InstanceState} is a type in the current Gateway module; it owns the instance state-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    private static final class InstanceState {

        /**
         * 中文说明：保存 samples 对应的状态、依赖或配置值；字段类型为 {@code Deque<Sample>}，由 {@code PassiveHealthTracker.InstanceState} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by samples; its type is {@code Deque<Sample>}, and {@code PassiveHealthTracker.InstanceState} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code PassiveHealthTracker.InstanceState} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthTracker.InstanceState}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Deque<Sample> samples = new ArrayDeque<>();

        /**
         * 中文说明：保存 consecutiveFailures 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code PassiveHealthTracker.InstanceState} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by consecutive failures; its type is {@code int}, and {@code PassiveHealthTracker.InstanceState} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code PassiveHealthTracker.InstanceState} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthTracker.InstanceState}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int consecutiveFailures;

        /**
         * 中文说明：保存 ejectionLevel 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code PassiveHealthTracker.InstanceState} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by ejection level; its type is {@code int}, and {@code PassiveHealthTracker.InstanceState} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code PassiveHealthTracker.InstanceState} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthTracker.InstanceState}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int ejectionLevel;

        /**
         * 中文说明：保存 ejectedUntil 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code PassiveHealthTracker.InstanceState} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by ejected until; its type is {@code Instant}, and {@code PassiveHealthTracker.InstanceState} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code PassiveHealthTracker.InstanceState} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthTracker.InstanceState}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Instant ejectedUntil;

        /**
         * 中文说明：执行 eligible 操作；该方法是 {@code PassiveHealthTracker.InstanceState} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the eligible operation; this method is the invocation entry point on {@code PassiveHealthTracker.InstanceState} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code PassiveHealthTracker.InstanceState.eligible(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param now 参数 now；parameter now。
         * @return 返回 eligible 的处理结果；returns the result of the operation.
         */
        private synchronized boolean eligible(Instant now) {
            return ejectedUntil == null || !now.isBefore(ejectedUntil);
        }

        /**
         * 中文说明：执行 record 操作；该方法是 {@code PassiveHealthTracker.InstanceState} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the record operation; this method is the invocation entry point on {@code PassiveHealthTracker.InstanceState} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code PassiveHealthTracker.InstanceState.record(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param outcome 参数 outcome；parameter outcome。
         * @param now 参数 now；parameter now。
         * @param policy 参数 策略；parameter policy。
         */
        private synchronized void record(
                ProviderCallOutcome outcome,
                Instant now,
                PassiveHealthPolicy policy) {
            evictOldSamples(now.minus(policy.window()));
            if (outcome == ProviderCallOutcome.BUSINESS_REJECTION
                    || outcome == ProviderCallOutcome.CANCELLED) {
                return;
            }
            if (outcome == ProviderCallOutcome.SUCCESS) {
                consecutiveFailures = 0;
                ejectionLevel = 0;
                ejectedUntil = null;
                samples.addLast(new Sample(now, false));
                return;
            }
            consecutiveFailures++;
            samples.addLast(new Sample(now, true));
            long failures = samples.stream().filter(Sample::failed).count();
            boolean consecutiveExceeded = consecutiveFailures
                    >= policy.consecutiveFailureThreshold();
            boolean rateExceeded = samples.size() >= policy.minimumSamples()
                    && (double) failures / samples.size()
                    >= policy.failureRateThreshold();
            if (consecutiveExceeded || rateExceeded) {
                eject(now, policy);
            }
        }

        /**
         * 中文说明：执行 evictOldSamples 操作；该方法是 {@code PassiveHealthTracker.InstanceState} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the evict old samples operation; this method is the invocation entry point on {@code PassiveHealthTracker.InstanceState} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code PassiveHealthTracker.InstanceState.evictOldSamples(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param earliest 参数 earliest；parameter earliest。
         */
        private void evictOldSamples(Instant earliest) {
            while (!samples.isEmpty()
                    && samples.getFirst().occurredAt().isBefore(earliest)) {
                samples.removeFirst();
            }
        }

        /**
         * 中文说明：执行 eject 操作；该方法是 {@code PassiveHealthTracker.InstanceState} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the eject operation; this method is the invocation entry point on {@code PassiveHealthTracker.InstanceState} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code PassiveHealthTracker.InstanceState.eject(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param now 参数 now；parameter now。
         * @param policy 参数 策略；parameter policy。
         */
        private void eject(Instant now, PassiveHealthPolicy policy) {
            long multiplier = 1L << Math.min(ejectionLevel, 20);
            Duration requested;
            try {
                requested = policy.baseEjectionDuration()
                        .multipliedBy(multiplier);
            } catch (ArithmeticException overflow) {
                requested = policy.maximumEjectionDuration();
            }
            Duration actual = requested.compareTo(
                    policy.maximumEjectionDuration()
            ) > 0 ? policy.maximumEjectionDuration() : requested;
            ejectedUntil = now.plus(actual);
            ejectionLevel++;
            consecutiveFailures = 0;
        }

        /**
         * 中文说明：执行 snapshot 操作；该方法是 {@code PassiveHealthTracker.InstanceState} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the snapshot operation; this method is the invocation entry point on {@code PassiveHealthTracker.InstanceState} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code PassiveHealthTracker.InstanceState.snapshot(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param now 参数 now；parameter now。
         * @return 返回 snapshot 的处理结果；returns the result of the operation.
         */
        private synchronized PassiveHealthSnapshot snapshot(Instant now) {
            return new PassiveHealthSnapshot(
                    eligible(now),
                    consecutiveFailures,
                    samples.size(),
                    ejectedUntil
            );
        }
    }

    /**
     * 中文说明：{@code Sample} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Sample相关的职责与边界。
     * English summary: {@code Sample} is an immutable data carrier in the current Gateway module; it owns the sample-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param occurredAt 参数 occurredAt；parameter occurred at。
     * @param failed 参数 failed；parameter failed。
     */
    private record Sample(
    /**
     * 中文说明：保存 occurredAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code PassiveHealthTracker.Sample} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by occurred at; its type is {@code Instant}, and {@code PassiveHealthTracker.Sample} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code PassiveHealthTracker.Sample} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthTracker.Sample}; do not couple callers to its representation when the owning type exposes an API.
     */
    Instant occurredAt,
    /**
     * 中文说明：保存 failed 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code PassiveHealthTracker.Sample} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by failed; its type is {@code boolean}, and {@code PassiveHealthTracker.Sample} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code PassiveHealthTracker.Sample} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthTracker.Sample}; do not couple callers to its representation when the owning type exposes an API.
     */
    boolean failed) {
    }

    /**
     * 中文说明：{@code PassiveHealthSnapshot} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Passive健康Snapshot相关的职责与边界。
     * English summary: {@code PassiveHealthSnapshot} is an immutable data carrier in the current Gateway module; it owns the passive health snapshot-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param eligible 参数 eligible；parameter eligible。
     * @param consecutiveFailures 参数 consecutiveFailures；parameter consecutive failures。
     * @param sampleCount 参数 sampleCount；parameter sample count。
     * @param ejectedUntil 参数 ejectedUntil；parameter ejected until。
     */
    public record PassiveHealthSnapshot(
            /**
             * 中文说明：保存 eligible 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code PassiveHealthTracker.PassiveHealthSnapshot} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by eligible; its type is {@code boolean}, and {@code PassiveHealthTracker.PassiveHealthSnapshot} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code PassiveHealthTracker.PassiveHealthSnapshot} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthTracker.PassiveHealthSnapshot}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean eligible,
            /**
             * 中文说明：保存 consecutiveFailures 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code PassiveHealthTracker.PassiveHealthSnapshot} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by consecutive failures; its type is {@code int}, and {@code PassiveHealthTracker.PassiveHealthSnapshot} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code PassiveHealthTracker.PassiveHealthSnapshot} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthTracker.PassiveHealthSnapshot}; do not couple callers to its representation when the owning type exposes an API.
             */
            int consecutiveFailures,
            /**
             * 中文说明：保存 sampleCount 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code PassiveHealthTracker.PassiveHealthSnapshot} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by sample count; its type is {@code int}, and {@code PassiveHealthTracker.PassiveHealthSnapshot} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code PassiveHealthTracker.PassiveHealthSnapshot} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthTracker.PassiveHealthSnapshot}; do not couple callers to its representation when the owning type exposes an API.
             */
            int sampleCount,
            /**
             * 中文说明：保存 ejectedUntil 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code PassiveHealthTracker.PassiveHealthSnapshot} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by ejected until; its type is {@code Instant}, and {@code PassiveHealthTracker.PassiveHealthSnapshot} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code PassiveHealthTracker.PassiveHealthSnapshot} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code PassiveHealthTracker.PassiveHealthSnapshot}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant ejectedUntil
    ) {

        /**
         * 中文说明：执行 healthy 操作；该方法是 {@code PassiveHealthTracker.PassiveHealthSnapshot} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the healthy operation; this method is the invocation entry point on {@code PassiveHealthTracker.PassiveHealthSnapshot} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code PassiveHealthTracker.PassiveHealthSnapshot.healthy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 healthy 的处理结果；returns the result of the operation.
         */
        private static PassiveHealthSnapshot healthy() {
            return new PassiveHealthSnapshot(true, 0, 0, null);
        }
    }
}
