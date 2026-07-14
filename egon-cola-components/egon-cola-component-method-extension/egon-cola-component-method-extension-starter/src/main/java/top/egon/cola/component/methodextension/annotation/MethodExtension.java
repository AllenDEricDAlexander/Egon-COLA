package top.egon.cola.component.methodextension.annotation;

import top.egon.cola.component.methodextension.handler.MethodExtensionHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MethodExtension {

    Class<? extends MethodExtensionHandler> handler();

    String returnJson() default "";
}
