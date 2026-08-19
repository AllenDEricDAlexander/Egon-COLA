package top.egon.cola.component.gateway.engine.common.traffic.service;

import top.egon.cola.component.gateway.engine.common.traffic.domain.ProviderCallClassification;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 中文说明：{@code GatewayCircuitBreakerRegistry} 是类型，位于当前 Gateway 模块的相关包中，负责网关CircuitBreaker注册表相关的职责与边界。
 * English summary: {@code GatewayCircuitBreakerRegistry} is a type in the current Gateway module; it owns the gateway circuit breaker registry-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayCircuitBreakerRegistry {

    /**
     * 中文说明：保存 breakers 对应的状态、依赖或配置值；字段类型为 {@code Map<String, CircuitBreaker>}，由 {@code GatewayCircuitBreakerRegistry} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by breakers; its type is {@code Map<String, CircuitBreaker>}, and {@code GatewayCircuitBreakerRegistry} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCircuitBreakerRegistry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCircuitBreakerRegistry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, CircuitBreaker> breakers =
            new ConcurrentHashMap<>();

    /**
     * 中文说明：执行 tryAcquire 操作；该方法是 {@code GatewayCircuitBreakerRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the try acquire operation; this method is the invocation entry point on {@code GatewayCircuitBreakerRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCircuitBreakerRegistry.tryAcquire(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policyId 参数 策略Id；parameter policy id。
     * @param stateEpoch 参数 stateEpoch；parameter state epoch。
     * @param policyVersion 参数 策略Version；parameter policy version。
     * @param providerRuntimeIdentity 参数 提供方运行时身份；parameter provider runtime identity。
     * @param policy 参数 策略；parameter policy。
     * @return 返回 tryAcquire 的处理结果；returns the result of the operation.
     */
    public CallPermission tryAcquire(
            String policyId,
            long stateEpoch,
            long policyVersion,
            String providerRuntimeIdentity,
            CircuitPolicy policy) {
        String key = String.join(
                ":",
                policyId,
                Long.toString(stateEpoch),
                Long.toString(policyVersion),
                providerRuntimeIdentity
        );
        CircuitBreaker breaker = breakers.computeIfAbsent(
                key,
                ignored -> CircuitBreaker.of(
                        key,
                        CircuitBreakerConfig.custom()
                                .failureRateThreshold(
                                        policy.failureRateThreshold()
                                )
                                .slidingWindowSize(policy.slidingWindowSize())
                                .minimumNumberOfCalls(
                                        policy.minimumNumberOfCalls()
                                )
                                .waitDurationInOpenState(
                                        policy.openDuration()
                                )
                                .permittedNumberOfCallsInHalfOpenState(
                                        policy.halfOpenPermits()
                                )
                                .build()
                )
        );
        if (!breaker.tryAcquirePermission()) {
            return CallPermission.rejected();
        }
        return new CallPermission(breaker);
    }

    /**
     * 中文说明：执行 available 操作；该方法是 {@code GatewayCircuitBreakerRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the available operation; this method is the invocation entry point on {@code GatewayCircuitBreakerRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCircuitBreakerRegistry.available(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policyId 参数 策略Id；parameter policy id。
     * @param stateEpoch 参数 stateEpoch；parameter state epoch。
     * @param policyVersion 参数 策略Version；parameter policy version。
     * @param providerRuntimeIdentity 参数 提供方运行时身份；parameter provider runtime identity。
     * @return 返回 available 的处理结果；returns the result of the operation.
     */
    public boolean available(
            String policyId,
            long stateEpoch,
            long policyVersion,
            String providerRuntimeIdentity) {
        CircuitBreaker breaker = breakers.get(String.join(
                ":",
                policyId,
                Long.toString(stateEpoch),
                Long.toString(policyVersion),
                providerRuntimeIdentity
        ));
        return breaker == null
                || breaker.getState() != CircuitBreaker.State.OPEN;
    }

    /**
     * 中文说明：{@code CircuitPolicy} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Circuit策略相关的职责与边界。
     * English summary: {@code CircuitPolicy} is an immutable data carrier in the current Gateway module; it owns the circuit policy-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param failureRateThreshold 参数 failureRateThreshold；parameter failure rate threshold。
     * @param slidingWindowSize 参数 slidingWindowSize；parameter sliding window size。
     * @param minimumNumberOfCalls 参数 minimumNumberOfCalls；parameter minimum number of calls。
     * @param openDuration 参数 openDuration；parameter open duration。
     * @param halfOpenPermits 参数 halfOpenPermits；parameter half open permits。
     */
    public record CircuitPolicy(
            /**
             * 中文说明：保存 failureRateThreshold 对应的状态、依赖或配置值；字段类型为 {@code float}，由 {@code GatewayCircuitBreakerRegistry.CircuitPolicy} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by failure rate threshold; its type is {@code float}, and {@code GatewayCircuitBreakerRegistry.CircuitPolicy} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCircuitBreakerRegistry.CircuitPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCircuitBreakerRegistry.CircuitPolicy}; do not couple callers to its representation when the owning type exposes an API.
             */
            float failureRateThreshold,
            /**
             * 中文说明：保存 slidingWindowSize 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayCircuitBreakerRegistry.CircuitPolicy} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by sliding window size; its type is {@code int}, and {@code GatewayCircuitBreakerRegistry.CircuitPolicy} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCircuitBreakerRegistry.CircuitPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCircuitBreakerRegistry.CircuitPolicy}; do not couple callers to its representation when the owning type exposes an API.
             */
            int slidingWindowSize,
            /**
             * 中文说明：保存 minimumNumberOfCalls 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayCircuitBreakerRegistry.CircuitPolicy} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by minimum number of calls; its type is {@code int}, and {@code GatewayCircuitBreakerRegistry.CircuitPolicy} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCircuitBreakerRegistry.CircuitPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCircuitBreakerRegistry.CircuitPolicy}; do not couple callers to its representation when the owning type exposes an API.
             */
            int minimumNumberOfCalls,
            /**
             * 中文说明：保存 openDuration 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayCircuitBreakerRegistry.CircuitPolicy} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by open duration; its type is {@code Duration}, and {@code GatewayCircuitBreakerRegistry.CircuitPolicy} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCircuitBreakerRegistry.CircuitPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCircuitBreakerRegistry.CircuitPolicy}; do not couple callers to its representation when the owning type exposes an API.
             */
            Duration openDuration,
            /**
             * 中文说明：保存 halfOpenPermits 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayCircuitBreakerRegistry.CircuitPolicy} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by half open permits; its type is {@code int}, and {@code GatewayCircuitBreakerRegistry.CircuitPolicy} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayCircuitBreakerRegistry.CircuitPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCircuitBreakerRegistry.CircuitPolicy}; do not couple callers to its representation when the owning type exposes an API.
             */
            int halfOpenPermits
    ) {

        /**
         * 中文说明：创建 {@code GatewayCircuitBreakerRegistry.CircuitPolicy} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayCircuitBreakerRegistry.CircuitPolicy} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param failureRateThreshold 参数 failureRateThreshold；parameter failure rate threshold。
         * @param slidingWindowSize 参数 slidingWindowSize；parameter sliding window size。
         * @param minimumNumberOfCalls 参数 minimumNumberOfCalls；parameter minimum number of calls。
         * @param openDuration 参数 openDuration；parameter open duration。
         * @param halfOpenPermits 参数 halfOpenPermits；parameter half open permits。
         */
        public CircuitPolicy {
            if (failureRateThreshold <= 0
                    || failureRateThreshold > 100
                    || slidingWindowSize < 1
                    || minimumNumberOfCalls < 1
                    || minimumNumberOfCalls > slidingWindowSize
                    || halfOpenPermits < 1
                    || openDuration == null
                    || openDuration.isZero()
                    || openDuration.isNegative()) {
                throw new IllegalArgumentException("invalid circuit policy");
            }
        }
    }

    /**
     * 中文说明：{@code CallPermission} 是类型，位于当前 Gateway 模块的相关包中，负责调用Permission相关的职责与边界。
     * English summary: {@code CallPermission} is a type in the current Gateway module; it owns the call permission-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static final class CallPermission implements AutoCloseable {

        /**
         * 中文说明：保存 breaker 对应的状态、依赖或配置值；字段类型为 {@code CircuitBreaker}，由 {@code GatewayCircuitBreakerRegistry.CallPermission} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by breaker; its type is {@code CircuitBreaker}, and {@code GatewayCircuitBreakerRegistry.CallPermission} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayCircuitBreakerRegistry.CallPermission} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCircuitBreakerRegistry.CallPermission}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final CircuitBreaker breaker;

        /**
         * 中文说明：保存 startedNanos 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayCircuitBreakerRegistry.CallPermission} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by started nanos; its type is {@code long}, and {@code GatewayCircuitBreakerRegistry.CallPermission} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayCircuitBreakerRegistry.CallPermission} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCircuitBreakerRegistry.CallPermission}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final long startedNanos;

        /**
         * 中文说明：保存 acquired 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayCircuitBreakerRegistry.CallPermission} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by acquired; its type is {@code boolean}, and {@code GatewayCircuitBreakerRegistry.CallPermission} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayCircuitBreakerRegistry.CallPermission} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCircuitBreakerRegistry.CallPermission}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final boolean acquired;

        /**
         * 中文说明：保存 completed 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code GatewayCircuitBreakerRegistry.CallPermission} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by completed; its type is {@code AtomicBoolean}, and {@code GatewayCircuitBreakerRegistry.CallPermission} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayCircuitBreakerRegistry.CallPermission} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCircuitBreakerRegistry.CallPermission}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicBoolean completed = new AtomicBoolean();

        /**
         * 中文说明：创建 {@code GatewayCircuitBreakerRegistry.CallPermission} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayCircuitBreakerRegistry.CallPermission} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param breaker 参数 breaker；parameter breaker。
         */
        private CallPermission(CircuitBreaker breaker) {
            this.breaker = breaker;
            startedNanos = System.nanoTime();
            acquired = true;
        }

        /**
         * 中文说明：创建 {@code GatewayCircuitBreakerRegistry.CallPermission} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayCircuitBreakerRegistry.CallPermission} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         */
        private CallPermission() {
            breaker = null;
            startedNanos = 0;
            acquired = false;
        }

        /**
         * 中文说明：执行 rejected 操作；该方法是 {@code GatewayCircuitBreakerRegistry.CallPermission} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the rejected operation; this method is the invocation entry point on {@code GatewayCircuitBreakerRegistry.CallPermission} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayCircuitBreakerRegistry.CallPermission.rejected(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 rejected 的处理结果；returns the result of the operation.
         */
        static CallPermission rejected() {
            return new CallPermission();
        }

        /**
         * 中文说明：执行 acquired 操作；该方法是 {@code GatewayCircuitBreakerRegistry.CallPermission} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the acquired operation; this method is the invocation entry point on {@code GatewayCircuitBreakerRegistry.CallPermission} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayCircuitBreakerRegistry.CallPermission.acquired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 acquired 的处理结果；returns the result of the operation.
         */
        public boolean acquired() {
            return acquired;
        }

        /**
         * 中文说明：执行 complete 操作；该方法是 {@code GatewayCircuitBreakerRegistry.CallPermission} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the complete operation; this method is the invocation entry point on {@code GatewayCircuitBreakerRegistry.CallPermission} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayCircuitBreakerRegistry.CallPermission.complete(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param classification 参数 classification；parameter classification。
         */
        public void complete(ProviderCallClassification classification) {
            if (!acquired || !completed.compareAndSet(false, true)) {
                return;
            }
            long duration = Math.max(0, System.nanoTime() - startedNanos);
            switch (classification) {
                case RETRYABLE_FAILURE -> breaker.onError(
                        duration,
                        TimeUnit.NANOSECONDS,
                        new IllegalStateException("retryable provider failure")
                );
                case SUCCESS, BUSINESS_FAILURE -> breaker.onSuccess(
                        duration,
                        TimeUnit.NANOSECONDS
                );
                case CANCELLED -> breaker.releasePermission();
            }
        }

        /**
         * 中文说明：执行 close 操作；该方法是 {@code GatewayCircuitBreakerRegistry.CallPermission} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the close operation; this method is the invocation entry point on {@code GatewayCircuitBreakerRegistry.CallPermission} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayCircuitBreakerRegistry.CallPermission.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        @Override
        public void close() {
            complete(ProviderCallClassification.CANCELLED);
        }
    }
}
