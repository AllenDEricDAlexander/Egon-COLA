package top.egon.cola.component.rpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a contract method to a gRPC wire method and records the per-method
 * decisions that would be wrong to state once for the whole contract, since a
 * single contract routinely mixes cheap reads with expensive writes.
 *
 * <p>The supported Java unary shapes are {@code Response method(Request)} and
 * {@code CompletionStage<Response> method(Request)}. The response type must be
 * the exact generated Protobuf output for the bound wire method. The
 * {@link #idempotent()} value is descriptive metadata only: configured
 * availability retries remain a framework policy, while business code owns
 * duplicate safety (for example, by using a unique document number).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EgonRpcMethod {

    String name();

    boolean idempotent() default false;

    /**
     * Call deadline in milliseconds for this method. Negative means unset,
     * deferring to the contract and then to the consumer-side default.
     */
    long timeoutMs() default -1;

    /**
     * Retry attempts after the initial call. Negative means unset; zero is
     * reserved for "explicitly do not retry". It does not disable retries merely
     * because {@link #idempotent()} is {@code false}.
     */
    int retries() default -1;

    /**
     * Whether the method may be reached from outside the trust boundary. Defaults
     * to {@code false} so that exposure is always a deliberate act: a method
     * added without thinking about it stays internal.
     */
    boolean externalAccessible() default false;

    /**
     * One-line human description used wherever a catalogue lists this method and
     * has no room for prose.
     */
    String summary() default "";

    /**
     * Marks the method as scheduled for removal so tooling can warn callers while
     * the method still works.
     */
    boolean deprecated() default false;
}
