package top.egon.cola.component.gateway.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an HTTP controller as a catalogued service, giving it the same
 * identity an RPC contract gets from {@code @EgonRpcService}.
 *
 * <p>Without this, an HTTP service is only a bag of loose endpoints: there is no
 * type-level place to say which service they belong to, which group and version
 * they carry, or how much traffic an instance should take. Mirroring the RPC
 * annotation's fields is what lets one catalogue describe both protocols.
 *
 * <p>Deliberately type-level only. There is no matching method-level annotation
 * and none should be added: Spring MVC's {@code @RequestMapping} family already
 * supplies the mechanical facts of an endpoint (path, HTTP method, parameters,
 * content types), and {@link GatewayOperation} already supplies the descriptive
 * ones (name, summary, owner, tags, external accessibility). A third annotation
 * could only restate what one of those two owns, and would become a second place
 * for the same fact to be written down — and therefore to be written down
 * differently.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface EgonHttpService {

    /**
     * Catalogue-facing name for this service. Empty means "derive it", so that
     * renaming the controller class does not silently rename a published service.
     */
    String serviceName() default "";

    /**
     * Logical partition this service is published into. Matches the RPC default
     * so a service reachable over both protocols lands in one place unless
     * someone says otherwise.
     */
    String group() default "default";

    /**
     * Contract version callers pin against. Matches the RPC default so the two
     * protocol surfaces of one service start out aligned.
     */
    String version() default "1.0.0";

    /**
     * Path prefix shared by this service's endpoints, recorded so the catalogue
     * can present absolute paths. Empty means the endpoints' own mappings are
     * already absolute.
     */
    String basePath() default "";

    /**
     * Relative capacity of instances serving this service, consumed by the
     * weighted balancing policies. Expressed as a share rather than an absolute
     * rate so instances of differing size can be compared without knowing the
     * cluster's total capacity.
     */
    int weight() default 100;
}
