package top.egon.cola.component.rpc.annotation;

import top.egon.cola.component.rpc.consumer.reference.RpcReferenceMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a proxy for an RPC contract into the annotated field and states how
 * <em>this</em> caller wants the remote treated.
 *
 * <p>Settings here belong to the call site, not to the service: two callers of the
 * same contract legitimately want different deadlines and different behaviour on
 * failure, and neither should have to modify the shared contract to get it.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EgonRpcReference {

    /**
     * Transport path selected by this injection point. The default is direct
     * Provider discovery; Gateway proxy calls must opt in explicitly.
     */
    RpcReferenceMode mode() default RpcReferenceMode.DIRECT;

    /**
     * Direct Provider business identity. These values are required when
     * {@link #mode()} is {@link RpcReferenceMode#DIRECT} and must be empty for
     * Gateway proxy references.
     */
    String bizCode() default "";

    /**
     * Direct Provider application identity. These values are required when
     * {@link #mode()} is {@link RpcReferenceMode#DIRECT} and must be empty for
     * Gateway proxy references.
     */
    String appCode() default "";

    /**
     * Direct Provider environment override. Empty means the consumer process
     * environment; it is not valid for Gateway proxy references.
     */
    String env() default "";

    /**
     * Call deadline in milliseconds for this injection point. Negative means
     * unset, deferring to the consumer-side default.
     *
     * <p>This value can only <em>shrink</em> the deadline, never extend it: the
     * effective timeout is the smaller of this value and the consumer-side
     * default, so setting a larger number here has no effect. The consumer
     * default is a ceiling deliberately, since it is what bounds thread and
     * connection occupancy for the whole process; letting an individual field
     * raise it would let one call site degrade every other.
     */
    long timeoutMs() default -1;

    /**
     * Retry attempts after the initial call. Negative means unset; zero is
     * reserved for "explicitly do not retry", which a caller may need to state
     * even where the contract permits retries.
     */
    int retries() default -1;

    /**
     * Load balancing policy for calls made through this injection point.
     *
     * @see LoadBalance#INHERIT
     */
    LoadBalance loadBalance() default LoadBalance.INHERIT;

    /**
     * Selects a specific provider group. Empty means "take the group the contract
     * declares", so a caller pins a group only when it genuinely needs a
     * different one from the contract's own.
     */
    String group() default "";

    /**
     * Selects a specific contract version. Empty means "take the version the
     * contract declares", keeping callers on the contract's version unless they
     * deliberately pin an older or newer one.
     */
    String version() default "";

    /**
     * Bean name supplying a degraded local result, consulted when
     * {@link #failStrategy()} resolves to {@link FailStrategy#LOCAL_FALLBACK}.
     * Named by string rather than by type so the fallback need not be visible to
     * the contract module.
     */
    String fallbackBean() default "";

    /**
     * Named {@link top.egon.cola.component.rpc.consumer.loadbalance.RpcLoadBalanceKeyResolver}
     * used only when the effective policy is {@link LoadBalance#CONSISTENT_HASH}.
     */
    String loadBalanceKeyResolver() default "";

    /**
     * What this caller should do once retries are exhausted.
     *
     * @see FailStrategy#INHERIT
     */
    FailStrategy failStrategy() default FailStrategy.INHERIT;
}
