package top.egon.cola.component.rpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as an RPC contract and carries the identity every caller and
 * every registry entry is keyed by.
 *
 * <p>This is the single source of service-level metadata. {@link EgonRpcProvider}
 * only says which bean implements a contract; nothing descriptive belongs there,
 * because a contract may have several implementations and they must not be able
 * to disagree about what the service is.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EgonRpcService {

    Class<?> grpcClass();

    String group() default "default";

    String version() default "1.0.0";

    /**
     * Catalogue-facing name for this service. Empty means "derive it", so that
     * renaming the Java interface does not silently rename a published service.
     */
    String serviceName() default "";

    /**
     * Relative capacity of instances serving this contract, consumed by the
     * weighted balancing policies. Expressed as a share rather than an absolute
     * rate so instances of differing size can be compared without knowing the
     * cluster's total capacity.
     */
    int weight() default 100;

    /**
     * Free-form labels used to slice the catalogue, for example by domain or by
     * compliance scope.
     */
    String[] tags() default {};

    /**
     * Default call deadline in milliseconds for methods of this contract.
     * Negative means unset, deferring to the consumer-side default.
     */
    long timeoutMs() default -1;

    /**
     * Default retry attempts after the initial call. Negative means unset;
     * zero is reserved for "explicitly do not retry".
     */
    int retries() default -1;

    /**
     * Load balancing policy applied to calls against this contract.
     *
     * @see LoadBalance#INHERIT
     */
    LoadBalance loadBalance() default LoadBalance.INHERIT;

    /**
     * Marks the whole contract as scheduled for removal so tooling can warn
     * callers while the contract still serves traffic.
     */
    boolean deprecated() default false;
}
