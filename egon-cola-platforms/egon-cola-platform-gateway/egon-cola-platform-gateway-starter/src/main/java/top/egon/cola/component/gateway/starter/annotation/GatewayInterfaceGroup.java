package top.egon.cola.component.gateway.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayInterfaceGroup {

    String businessDomainCode();

    String businessDomainName();

    String entityDomainCode();

    String entityDomainName();

    String code();

    String name();

    String description() default "";

    String mcpServerCode() default "";
}
