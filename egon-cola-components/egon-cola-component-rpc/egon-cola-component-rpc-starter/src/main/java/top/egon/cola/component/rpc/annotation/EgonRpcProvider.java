package top.egon.cola.component.rpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a bean as an implementation to be exported over RPC.
 *
 * <p>Its sole responsibility is bean discovery: it answers "which beans should be
 * scanned", nothing more. It is intentionally field-free, and should stay that
 * way. All service metadata — identity, timeouts, weight, load balancing,
 * lifecycle — belongs on the {@link EgonRpcService} contract, because a contract
 * may have more than one implementation and callers key off the contract. Any
 * field added here could be set differently per implementation, which would make
 * the published description of a service depend on which bean happened to be
 * scanned first.
 *
 * <p>An annotated bean must implement at least one {@link EgonRpcService}
 * interface; a bean that does not is rejected at startup rather than being
 * silently exported with no callable methods.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EgonRpcProvider {
}
