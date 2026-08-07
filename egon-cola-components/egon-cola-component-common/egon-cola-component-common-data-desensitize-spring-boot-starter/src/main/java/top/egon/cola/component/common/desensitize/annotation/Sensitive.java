package top.egon.cola.component.common.desensitize.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {

    SensitiveType type();

    SensitiveScene[] scenes() default {
            SensitiveScene.RESPONSE,
            SensitiveScene.LOG
    };
}
