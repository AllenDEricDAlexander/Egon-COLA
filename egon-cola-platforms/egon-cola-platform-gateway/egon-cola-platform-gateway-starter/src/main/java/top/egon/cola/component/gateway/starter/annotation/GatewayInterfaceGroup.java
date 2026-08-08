package top.egon.cola.component.gateway.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Groups the operations declared by an HTTP controller or RPC contract into one
 * catalogued gateway interface.
 *
 * <p>The business and entity domain attributes place the interface in the
 * catalogue taxonomy. The interface code identifies the group within that
 * taxonomy, while the optional MCP server code selects the server used when an
 * operation in the group is exposed through MCP.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayInterfaceGroup {

    /**
     * Code of the business domain that owns the interface group.
     *
     * @return the business domain code
     */
    String businessDomainCode();

    /**
     * Display name of the business domain that owns the interface group.
     *
     * @return the business domain name
     */
    String businessDomainName();

    /**
     * Code of the entity domain represented by the interface group.
     *
     * @return the entity domain code
     */
    String entityDomainCode();

    /**
     * Display name of the entity domain represented by the interface group.
     *
     * @return the entity domain name
     */
    String entityDomainName();

    /**
     * Stable code of the interface group within its domain taxonomy.
     *
     * @return the interface group code
     */
    String code();

    /**
     * Display name of the interface group.
     *
     * @return the interface group name
     */
    String name();

    /**
     * Human-readable description of the interface group's responsibility.
     *
     * @return the description, or an empty string when none is declared
     */
    String description() default "";

    /**
     * Code of the MCP server that receives operations from this group.
     *
     * <p>A non-blank value is required when any contained operation enables MCP
     * registration.
     *
     * @return the MCP server code, or an empty string when MCP is not used
     */
    String mcpServerCode() default "";
}
