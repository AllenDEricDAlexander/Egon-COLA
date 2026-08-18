package top.egon.cola.platform.rbac3.starter.field;

import top.egon.cola.component.common.desensitize.annotation.SensitiveType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the stable RBAC field resource used by response serialization and the web field guard.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface RBACFieldResource {

    String code();

    String name();

    String resourceCode();

    String permission();

    /**
     * Fallback strategy for a MASKED_READ decision when the policy does not carry one.
     * Existing applications use {@link SensitiveType#FULL} as the fail-safe default.
     */
    SensitiveType maskingStrategy() default SensitiveType.FULL;
}
