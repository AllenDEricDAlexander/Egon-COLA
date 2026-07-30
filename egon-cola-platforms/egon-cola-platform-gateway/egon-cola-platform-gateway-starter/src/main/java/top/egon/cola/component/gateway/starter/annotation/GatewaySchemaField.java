package top.egon.cola.component.gateway.starter.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface GatewaySchemaField {

    String path();

    String description();
}
