package top.egon.cola.component.ddc.management.model;

import java.util.Locale;

/**
 * 单个服务实例的健康状态，由主动探测或熔断器转换等被动信号观测得到。 /
 * Health of a single service instance, as observed by active probing or by passive
 * signals such as circuit-breaker transitions.
 *
 * <p>该状态有意比 {@link DdcInstanceStatus} 更丰富，后者只表示租约是否存活。实例即使持有完全有效的租约，
 * 仍可能无法处理流量；负载均衡器应根据 {@code InstanceHealthState} 过滤实例。 /
 * This is deliberately richer than {@link DdcInstanceStatus}, which only reports
 * whether a lease is alive. An instance can hold a perfectly valid lease and still be
 * unable to serve traffic; {@code InstanceHealthState} is what load balancers filter on.
 *
 * <p><strong>{@link #UNKNOWN} 被刻意设计为可选择。</strong>尚未探测实例的网关，或从不报告健康状态的旧版
 * 提供者，不应被移出实例池，否则网关升级会立即排空所有旧实例。参见 {@link #selectable()}。 /
 * <strong>{@link #UNKNOWN} is selectable on purpose.</strong> A gateway that has not
 * yet probed an instance — or an older provider that never reports health — must not be
 * removed from the pool, otherwise upgrading the gateway would drain every legacy
 * instance at once. See {@link #selectable()}.
 */
public enum InstanceHealthState {

    /** 探测通过，使用完整权重。 / Probes pass; full weight. */
    UP,

    /** 可达但性能受损（慢调用或部分失败），使用降低后的权重。 / Reachable but impaired; serves at reduced weight. */
    DEGRADED,

    /** 探测失败，在恢复前排除出选择范围。 / Probes fail; excluded from selection until recovery. */
    DOWN,

    /** 已被管理操作排空，排除出选择范围且不会自动恢复。 / Administratively drained; excluded and never auto-recovered. */
    OUT_OF_SERVICE,

    /** 从未探测，或由不支持健康上报的旧版提供者报告。 / Never probed, or reported by a provider predating health reporting. */
    UNKNOWN;

    /** 应用于 {@link ServiceInstanceMeta#weight()} 的降级权重百分比。 / Percentage multiplier applied to the configured weight. */
    private static final int DEGRADED_WEIGHT_PERCENT = 50;

    /**
     * 将兼容的线格式健康状态别名归一化为客户端状态。 /
     * Normalizes compatible wire-format health aliases to a client state.
     *
     * @param value 元数据中的健康状态文本 / health-state text from metadata
     * @return 归一化状态；空值或未知值返回 {@link #UNKNOWN} / normalized state; {@link #UNKNOWN} for blank or unknown values
     */
    public static InstanceHealthState fromWire(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "UP", "HEALTHY", "ONLINE" -> UP;
            case "DEGRADED", "WARN" -> DEGRADED;
            case "DOWN", "UNHEALTHY", "OFFLINE" -> DOWN;
            case "OUT_OF_SERVICE", "DRAINING", "DRAINED" -> OUT_OF_SERVICE;
            default -> UNKNOWN;
        };
    }

    /**
     * 判断 {@code value} 是否为已识别的线格式。 / Determines whether {@code value} is a recognized wire form.
     *
     * <p>{@link #fromWire} 会把所有无法识别的值映射为 {@link #UNKNOWN}，这适用于读取但不适用于写入校验；
     * 本方法可区分“调用方明确传入 UNKNOWN”和“调用方传入无法解析的内容”。 /
     * {@link #fromWire} maps anything unrecognized to {@link #UNKNOWN}, which is right for
     * reads but useless for validating a write — this distinguishes "the caller said UNKNOWN"
     * from "the caller said something we could not parse".
     *
     * @param value 待检查的线格式值 / wire-format value to inspect
     * @return 值为已识别状态时返回 {@code true} / {@code true} when the value denotes a recognized state
     */
    public static boolean isKnownWireValue(String value) {
        return value != null && !value.isBlank()
                && (fromWire(value) != UNKNOWN || "UNKNOWN".equalsIgnoreCase(value.trim()));
    }

    /**
     * 判断负载均衡器是否可将流量路由到此状态的实例。 /
     * Determines whether a load balancer may route to an instance in this state.
     *
     * <p>调用方仍需提供全不健康回退：若所有候选实例都不可选择，尝试一个可能失效的实例通常比直接丢弃
     * 全部流量更合适。 / Callers still need an all-unhealthy fallback: if every candidate is unselectable,
     * dropping all traffic is worse than trying a probably-dead instance.
     *
     * @return 此状态允许参与路由选择时返回 {@code true} / {@code true} when this state may participate in routing
     */
    public boolean selectable() {
        return this == UP || this == DEGRADED || this == UNKNOWN;
    }

    /**
     * 返回此健康状态应保留的配置权重百分比。 /
     * Returns the percentage of configured weight retained by this health state.
     *
     * @return 范围为 0 到 100 的权重百分比 / weight percentage from 0 through 100
     */
    public int weightPercent() {
        return switch (this) {
            case UP, UNKNOWN -> 100;
            case DEGRADED -> DEGRADED_WEIGHT_PERCENT;
            case DOWN, OUT_OF_SERVICE -> 0;
        };
    }
}
