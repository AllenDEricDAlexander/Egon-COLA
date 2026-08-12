package top.egon.cola.component.gateway.engine.traffic;

/**
 * 中文说明：{@code RateLimitDecision} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责RateLimitDecision相关的职责与边界。
 * English summary: {@code RateLimitDecision} is an immutable data carrier in the current Gateway module; it owns the rate limit decision-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param allowed 参数 allowed；parameter allowed。
 * @param remaining 参数 remaining；parameter remaining。
 * @param retryAfterMillis 参数 重试AfterMillis；parameter retry after millis。
 * @param resetAtEpochMillis 参数 resetAtEpochMillis；parameter reset at epoch millis。
 * @param localFallback 参数 localFallback；parameter local fallback。
 * @param backendUnavailable 参数 backendUnavailable；parameter backend unavailable。
 */
public record RateLimitDecision(
        /**
         * 中文说明：保存 allowed 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code RateLimitDecision} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by allowed; its type is {@code boolean}, and {@code RateLimitDecision} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RateLimitDecision} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RateLimitDecision}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean allowed,
        /**
         * 中文说明：保存 remaining 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code RateLimitDecision} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by remaining; its type is {@code long}, and {@code RateLimitDecision} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RateLimitDecision} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RateLimitDecision}; do not couple callers to its representation when the owning type exposes an API.
         */
        long remaining,
        /**
         * 中文说明：保存 重试AfterMillis 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code RateLimitDecision} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by retry after millis; its type is {@code long}, and {@code RateLimitDecision} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RateLimitDecision} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RateLimitDecision}; do not couple callers to its representation when the owning type exposes an API.
         */
        long retryAfterMillis,
        /**
         * 中文说明：保存 resetAtEpochMillis 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code RateLimitDecision} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by reset at epoch millis; its type is {@code long}, and {@code RateLimitDecision} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RateLimitDecision} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RateLimitDecision}; do not couple callers to its representation when the owning type exposes an API.
         */
        long resetAtEpochMillis,
        /**
         * 中文说明：保存 localFallback 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code RateLimitDecision} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by local fallback; its type is {@code boolean}, and {@code RateLimitDecision} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RateLimitDecision} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RateLimitDecision}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean localFallback,
        /**
         * 中文说明：保存 backendUnavailable 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code RateLimitDecision} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by backend unavailable; its type is {@code boolean}, and {@code RateLimitDecision} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RateLimitDecision} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RateLimitDecision}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean backendUnavailable
) {
}
