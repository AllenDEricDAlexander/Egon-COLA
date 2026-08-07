package top.egon.cola.component.gateway.contract.rule;

import java.util.Locale;

/**
 * 发布规则可声明的 provider 实例选择算法。
 *
 * <p>枚举名称与 Engine 的负载均衡注册表保持一致，{@link #supported()} 用于标识当前运行时
 * 是否已经具备执行能力。
 */
public enum LoadBalanceStrategy {

    ROUND_ROBIN,

    /** 平滑的加权轮询，避免普通加权轮询产生集中请求。 */
    SMOOTH_WEIGHTED_ROUND_ROBIN,

    RANDOM,

    /** 优先选择当前未完成请求数最少的实例。 */
    LEAST_IN_FLIGHT,

    /**
     * 按请求属性哈希选择实例，使相同键尽量保持会话亲和性。
     *
     * <p>当前可在规则中声明，但若 Engine 尚未支持，发布阶段会拒绝该规则，避免运行时静默降级。
     */
    CONSISTENT_HASH;

    /** 返回当前 Engine 是否能够执行该算法。 */
    public boolean supported() {
        return this != CONSISTENT_HASH;
    }

    public static LoadBalanceStrategy fromWire(String value, LoadBalanceStrategy fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return fallback;
        }
    }
}
