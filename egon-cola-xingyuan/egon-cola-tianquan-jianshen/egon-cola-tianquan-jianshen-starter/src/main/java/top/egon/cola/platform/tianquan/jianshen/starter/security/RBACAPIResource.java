package top.egon.cola.platform.tianquan.jianshen.starter.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the API resource identity and the permission required to invoke it.
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RBACAPIResource {

    String code();

    String permission();

    String name();
}
