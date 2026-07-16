package top.egon.cola.component.bytecode.api.observation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EgonObserved {

    String value() default "";

    String[] tags() default {};

    long slowThresholdMillis() default -1L;
}
