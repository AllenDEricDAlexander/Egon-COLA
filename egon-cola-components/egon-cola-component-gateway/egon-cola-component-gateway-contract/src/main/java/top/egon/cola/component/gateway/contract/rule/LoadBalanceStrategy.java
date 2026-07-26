package top.egon.cola.component.gateway.contract.rule;

import java.util.Locale;

/**
 * Provider selection algorithms expressible in a published rule.
 *
 * <p>The first four constants correspond one-to-one with the algorithms the engine already
 * implements, and carry the same names so the mapping needs no translation table.
 * {@link #CONSISTENT_HASH} is declarable here ahead of engine support; see {@link #supported()}.
 */
public enum LoadBalanceStrategy {

    ROUND_ROBIN,

    /** Weighted selection that avoids the bursts plain weighted round-robin produces. */
    SMOOTH_WEIGHTED_ROUND_ROBIN,

    RANDOM,

    /** Prefers the instance with the fewest outstanding requests. */
    LEAST_IN_FLIGHT,

    /**
     * Routes by a hash of a request attribute, so the same key reaches the same instance.
     *
     * <p>Declarable but not yet implemented by the engine. A rule selecting it is rejected at
     * publication rather than silently degrading to another algorithm at call time, which would
     * break the session affinity the caller asked for without telling anyone.
     */
    CONSISTENT_HASH;

    /** Whether the engine can currently execute this strategy. */
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
