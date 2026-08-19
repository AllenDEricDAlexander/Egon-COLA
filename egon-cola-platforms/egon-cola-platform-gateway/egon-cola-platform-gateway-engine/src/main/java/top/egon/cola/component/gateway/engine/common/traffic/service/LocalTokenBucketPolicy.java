package top.egon.cola.component.gateway.engine.common.traffic.service;

import java.time.Duration;
import java.util.Objects;

/**
 * 中文说明：{@code LocalTokenBucketPolicy} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责LocalTokenBucket策略相关的职责与边界。
 * English summary: {@code LocalTokenBucketPolicy} is an immutable data carrier in the current Gateway module; it owns the local token bucket policy-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param policyId 参数 策略Id；parameter policy id。
 * @param stateEpoch 参数 stateEpoch；parameter state epoch。
 * @param capacity 参数 capacity；parameter capacity。
 * @param refillTokens 参数 refillTokens；parameter refill tokens。
 * @param refillPeriod 参数 refillPeriod；parameter refill period。
 * @param initialTokens 参数 initialTokens；parameter initial tokens。
 * @param maximumKeys 参数 maximumKeys；parameter maximum keys。
 * @param idleTtl 参数 idleTtl；parameter idle ttl。
 */
public record LocalTokenBucketPolicy(
        /**
         * 中文说明：保存 策略Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code LocalTokenBucketPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policy id; its type is {@code String}, and {@code LocalTokenBucketPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code LocalTokenBucketPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LocalTokenBucketPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        String policyId,
        /**
         * 中文说明：保存 stateEpoch 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code LocalTokenBucketPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by state epoch; its type is {@code long}, and {@code LocalTokenBucketPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code LocalTokenBucketPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LocalTokenBucketPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        long stateEpoch,
        /**
         * 中文说明：保存 capacity 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code LocalTokenBucketPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by capacity; its type is {@code long}, and {@code LocalTokenBucketPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code LocalTokenBucketPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LocalTokenBucketPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        long capacity,
        /**
         * 中文说明：保存 refillTokens 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code LocalTokenBucketPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by refill tokens; its type is {@code long}, and {@code LocalTokenBucketPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code LocalTokenBucketPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LocalTokenBucketPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        long refillTokens,
        /**
         * 中文说明：保存 refillPeriod 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code LocalTokenBucketPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by refill period; its type is {@code Duration}, and {@code LocalTokenBucketPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code LocalTokenBucketPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LocalTokenBucketPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration refillPeriod,
        /**
         * 中文说明：保存 initialTokens 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code LocalTokenBucketPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by initial tokens; its type is {@code long}, and {@code LocalTokenBucketPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code LocalTokenBucketPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LocalTokenBucketPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        long initialTokens,
        /**
         * 中文说明：保存 maximumKeys 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code LocalTokenBucketPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by maximum keys; its type is {@code int}, and {@code LocalTokenBucketPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code LocalTokenBucketPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LocalTokenBucketPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        int maximumKeys,
        /**
         * 中文说明：保存 idleTtl 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code LocalTokenBucketPolicy} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by idle ttl; its type is {@code Duration}, and {@code LocalTokenBucketPolicy} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code LocalTokenBucketPolicy} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code LocalTokenBucketPolicy}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration idleTtl
) {

    /**
     * 中文说明：创建 {@code LocalTokenBucketPolicy} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code LocalTokenBucketPolicy} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param policyId 参数 策略Id；parameter policy id。
     * @param stateEpoch 参数 stateEpoch；parameter state epoch。
     * @param capacity 参数 capacity；parameter capacity。
     * @param refillTokens 参数 refillTokens；parameter refill tokens。
     * @param refillPeriod 参数 refillPeriod；parameter refill period。
     * @param initialTokens 参数 initialTokens；parameter initial tokens。
     * @param maximumKeys 参数 maximumKeys；parameter maximum keys。
     * @param idleTtl 参数 idleTtl；parameter idle ttl。
     */
    public LocalTokenBucketPolicy {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("policyId is required");
        }
        if (stateEpoch < 0
                || capacity < 1
                || refillTokens < 1
                || initialTokens < 0
                || initialTokens > capacity
                || maximumKeys < 1) {
            throw new IllegalArgumentException(
                    "invalid local token bucket bounds"
            );
        }
        refillPeriod = positive(refillPeriod, "refillPeriod");
        idleTtl = positive(idleTtl, "idleTtl");
    }

    /**
     * 中文说明：执行 state键 操作；该方法是 {@code LocalTokenBucketPolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the state key operation; this method is the invocation entry point on {@code LocalTokenBucketPolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LocalTokenBucketPolicy.stateKey(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param keyHash 参数 键Hash；parameter key hash。
     * @return 返回 state键 的处理结果；returns the result of the operation.
     */
    String stateKey(String keyHash) {
        return policyId + ":" + stateEpoch + ":" + keyHash;
    }

    /**
     * 中文说明：执行 positive 操作；该方法是 {@code LocalTokenBucketPolicy} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the positive operation; this method is the invocation entry point on {@code LocalTokenBucketPolicy} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LocalTokenBucketPolicy.positive(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
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
