package top.egon.cola.component.rpc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Protocol-neutral metadata shared by RPC and HTTP declarations.
 *
 * <p>The descriptive fields (ownership, tags, lifecycle) and the call-shaping
 * fields (timeout, retries, load balancing) mean the same thing whether a service
 * is reached over gRPC or over HTTP. Defining them once here, rather than copying
 * them into each protocol's annotation, is what stops the two surfaces from
 * drifting: a field added for RPC alone would quietly leave the HTTP catalogue
 * with a narrower view of the same service.
 *
 * <p>Applicable to a type or a method so that a method may narrow what its
 * declaring type states. {@code @Inherited} so a metadata decision made on a base
 * declaration is not silently lost by a subclass.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface EgonServiceMeta {

    /**
     * One-line human description used wherever a catalogue lists this element and
     * has no room for prose.
     */
    String summary() default "";

    /**
     * Long-form explanation for readers deciding whether this is the right
     * element to call, kept apart from {@link #summary()} so listings stay terse.
     */
    String description() default "";

    /**
     * Team or individual accountable for this element, so an operator reading a
     * failure has a route to an owner without consulting a separate registry.
     */
    String owner() default "";

    /**
     * Free-form labels used to slice the catalogue, for example by domain or by
     * compliance scope.
     */
    String[] tags() default {};

    /**
     * Marks the element as scheduled for removal so tooling can warn callers
     * while the element still works.
     */
    boolean deprecated() default false;

    /**
     * Release in which this element first appeared, letting a consumer decide
     * whether it may depend on it yet.
     */
    String since() default "";

    /**
     * Call deadline in milliseconds. Negative means unset, deferring to the
     * enclosing layer; zero is not used as the sentinel because it is a
     * meaningful (if degenerate) deadline.
     */
    long timeoutMs() default -1;

    /**
     * Retry attempts after the initial call. Negative means unset, deferring to
     * the enclosing layer; zero is reserved for "explicitly do not retry", which
     * is a different statement from saying nothing.
     */
    int retries() default -1;

    /**
     * Load balancing policy for calls to this element.
     *
     * @see LoadBalance#INHERIT
     */
    LoadBalance loadBalance() default LoadBalance.INHERIT;
}
