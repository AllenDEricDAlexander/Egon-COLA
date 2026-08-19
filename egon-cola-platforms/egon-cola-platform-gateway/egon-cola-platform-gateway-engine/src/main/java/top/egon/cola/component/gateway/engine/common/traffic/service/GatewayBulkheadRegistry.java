package top.egon.cola.component.gateway.engine.common.traffic.service;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 中文说明：{@code GatewayBulkheadRegistry} 是类型，位于当前 Gateway 模块的相关包中，负责网关Bulkhead注册表相关的职责与边界。
 * English summary: {@code GatewayBulkheadRegistry} is a type in the current Gateway module; it owns the gateway bulkhead registry-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayBulkheadRegistry {

    /**
     * 中文说明：保存 bulkheads 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Bulkhead>}，由 {@code GatewayBulkheadRegistry} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by bulkheads; its type is {@code Map<String, Bulkhead>}, and {@code GatewayBulkheadRegistry} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayBulkheadRegistry} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayBulkheadRegistry}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final Map<String, Bulkhead> bulkheads = new ConcurrentHashMap<>();

    /**
     * 中文说明：执行 tryAcquire 操作；该方法是 {@code GatewayBulkheadRegistry} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the try acquire operation; this method is the invocation entry point on {@code GatewayBulkheadRegistry} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBulkheadRegistry.tryAcquire(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policyId 参数 策略Id；parameter policy id。
     * @param stateEpoch 参数 stateEpoch；parameter state epoch。
     * @param policyVersion 参数 策略Version；parameter policy version。
     * @param dimension 参数 dimension；parameter dimension。
     * @param maxConcurrent 参数 maxConcurrent；parameter max concurrent。
     * @return 返回 tryAcquire 的处理结果；returns the result of the operation.
     */
    public Permit tryAcquire(
            String policyId,
            long stateEpoch,
            long policyVersion,
            String dimension,
            int maxConcurrent) {
        if (maxConcurrent < 1) {
            throw new IllegalArgumentException(
                    "maxConcurrent must be positive"
            );
        }
        String key = String.join(
                ":",
                policyId,
                Long.toString(stateEpoch),
                Long.toString(policyVersion),
                dimension
        );
        Bulkhead bulkhead = bulkheads.computeIfAbsent(
                key,
                ignored -> Bulkhead.of(
                        key,
                        BulkheadConfig.custom()
                                .maxConcurrentCalls(maxConcurrent)
                                .maxWaitDuration(Duration.ZERO)
                                .build()
                )
        );
        if (!bulkhead.tryAcquirePermission()) {
            return Permit.rejected();
        }
        return new Permit(true, bulkhead::onComplete);
    }

    /**
     * 中文说明：{@code Permit} 是类型，位于当前 Gateway 模块的相关包中，负责Permit相关的职责与边界。
     * English summary: {@code Permit} is a type in the current Gateway module; it owns the permit-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static final class Permit implements AutoCloseable {

        /**
         * 中文说明：保存 acquired 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayBulkheadRegistry.Permit} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by acquired; its type is {@code boolean}, and {@code GatewayBulkheadRegistry.Permit} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayBulkheadRegistry.Permit} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayBulkheadRegistry.Permit}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final boolean acquired;

        /**
         * 中文说明：保存 发布 对应的状态、依赖或配置值；字段类型为 {@code Runnable}，由 {@code GatewayBulkheadRegistry.Permit} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by release; its type is {@code Runnable}, and {@code GatewayBulkheadRegistry.Permit} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayBulkheadRegistry.Permit} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayBulkheadRegistry.Permit}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final Runnable release;

        /**
         * 中文说明：保存 closed 对应的状态、依赖或配置值；字段类型为 {@code AtomicBoolean}，由 {@code GatewayBulkheadRegistry.Permit} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by closed; its type is {@code AtomicBoolean}, and {@code GatewayBulkheadRegistry.Permit} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayBulkheadRegistry.Permit} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayBulkheadRegistry.Permit}; do not couple callers to its representation when the owning type exposes an API.
         */
        private final AtomicBoolean closed = new AtomicBoolean();

        /**
         * 中文说明：创建 {@code GatewayBulkheadRegistry.Permit} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayBulkheadRegistry.Permit} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param acquired 参数 acquired；parameter acquired。
         * @param release 参数 发布；parameter release。
         */
        private Permit(boolean acquired, Runnable release) {
            this.acquired = acquired;
            this.release = release;
        }

        /**
         * 中文说明：执行 rejected 操作；该方法是 {@code GatewayBulkheadRegistry.Permit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the rejected operation; this method is the invocation entry point on {@code GatewayBulkheadRegistry.Permit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayBulkheadRegistry.Permit.rejected(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 rejected 的处理结果；returns the result of the operation.
         */
        static Permit rejected() {
            return new Permit(false, () -> {
            });
        }

        /**
         * 中文说明：执行 acquired 操作；该方法是 {@code GatewayBulkheadRegistry.Permit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the acquired operation; this method is the invocation entry point on {@code GatewayBulkheadRegistry.Permit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayBulkheadRegistry.Permit.acquired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 acquired 的处理结果；returns the result of the operation.
         */
        public boolean acquired() {
            return acquired;
        }

        /**
         * 中文说明：执行 close 操作；该方法是 {@code GatewayBulkheadRegistry.Permit} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the close operation; this method is the invocation entry point on {@code GatewayBulkheadRegistry.Permit} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayBulkheadRegistry.Permit.close(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         */
        @Override
        public void close() {
            if (acquired && closed.compareAndSet(false, true)) {
                release.run();
            }
        }
    }
}
