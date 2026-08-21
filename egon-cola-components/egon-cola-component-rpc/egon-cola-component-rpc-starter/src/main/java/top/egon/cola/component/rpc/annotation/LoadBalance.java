package top.egon.cola.component.rpc.annotation;

/**
 * Load balancing policy selectable from a declaration site.
 *
 * <p>Constant names are kept identical to
 * {@code top.egon.cola.component.gateway.engine.common.provider.domain.LoadBalancerType} so that a
 * declaration can be mapped onto the engine policy by name alone, without a
 * translation table that would silently drift as either side gains a strategy.
 *
 * <p>{@link #INHERIT} exists because an annotation attribute always has a value:
 * an unset attribute is indistinguishable from one explicitly written out with the
 * same value. Without a dedicated sentinel, a reference that deliberately pins
 * {@code ROUND_ROBIN} would read exactly like one that said nothing, so the
 * resolver could not tell which layer of the chain (method, service, consumer
 * default, engine default) actually owns the decision. Every declaration site
 * therefore defaults to {@code INHERIT}, meaning "defer to the next layer".
 */
public enum LoadBalance {

    /**
     * No opinion at this declaration site; the enclosing layer decides.
     */
    INHERIT,

    ROUND_ROBIN,

    SMOOTH_WEIGHTED_ROUND_ROBIN,

    RANDOM,

    WEIGHTED_RANDOM,

    CONSISTENT_HASH,

    LEAST_IN_FLIGHT
}
