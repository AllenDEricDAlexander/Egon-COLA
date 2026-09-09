package top.egon.cola.component.yuheng.openapi.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the catalogue taxonomy owned by a published HTTP controller.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EgonApiCatalog {

    String businessDomainCode();

    String businessDomainName() default "";

    String entityDomainCode();

    String entityDomainName() default "";

    String interfaceGroupCode();
}
