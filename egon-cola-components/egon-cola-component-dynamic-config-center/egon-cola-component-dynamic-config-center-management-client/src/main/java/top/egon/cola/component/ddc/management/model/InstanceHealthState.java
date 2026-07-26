package top.egon.cola.component.ddc.management.model;

import java.util.Locale;

/**
 * Health of a single service instance, as observed by active probing or by passive
 * signals such as circuit-breaker transitions.
 *
 * <p>This is deliberately richer than {@link DdcInstanceStatus}, which only reports
 * whether a lease is alive. An instance can hold a perfectly valid lease and still be
 * unable to serve traffic; {@code InstanceHealthState} is what load balancers filter on.
 *
 * <p><strong>{@link #UNKNOWN} is selectable on purpose.</strong> A gateway that has not
 * yet probed an instance — or an older provider that never reports health — must not be
 * removed from the pool, otherwise upgrading the gateway would drain every legacy
 * instance at once. See {@link #selectable()}.
 */
public enum InstanceHealthState {

    /** Probes pass; full weight. */
    UP,

    /** Reachable but impaired (slow calls, partial failures); serves at reduced weight. */
    DEGRADED,

    /** Probes fail; excluded from selection until it recovers. */
    DOWN,

    /** Administratively drained. Excluded from selection and never auto-recovered. */
    OUT_OF_SERVICE,

    /** Never probed, or reported by a provider that predates health reporting. */
    UNKNOWN;

    /** Weight multiplier applied to {@link ServiceInstanceMeta#weight()} in percent. */
    private static final int DEGRADED_WEIGHT_PERCENT = 50;

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
     * Whether {@code value} is a recognised wire form.
     *
     * <p>{@link #fromWire} maps anything unrecognised to {@link #UNKNOWN}, which is right for
     * reads but useless for validating a write — this distinguishes "the caller said UNKNOWN"
     * from "the caller said something we could not parse".
     */
    public static boolean isKnownWireValue(String value) {
        return value != null && !value.isBlank()
                && (fromWire(value) != UNKNOWN || "UNKNOWN".equalsIgnoreCase(value.trim()));
    }

    /**
     * Whether a load balancer may route to an instance in this state.
     *
     * <p>Callers still need an all-unhealthy fallback: if every candidate is unselectable,
     * dropping all traffic is worse than trying a probably-dead instance.
     */
    public boolean selectable() {
        return this == UP || this == DEGRADED || this == UNKNOWN;
    }

    /** Percentage of the configured weight an instance in this state should receive. */
    public int weightPercent() {
        return switch (this) {
            case UP, UNKNOWN -> 100;
            case DEGRADED -> DEGRADED_WEIGHT_PERCENT;
            case DOWN, OUT_OF_SERVICE -> 0;
        };
    }
}
