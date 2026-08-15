package top.egon.cola.component.rpc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将 RPC 契约字段注入为通过服务目录发现 Provider 的直连代理。
 *
 * <p>Injects an RPC contract field as a direct Provider proxy discovered
 * through the service directory.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EgonRpcDirectReference {

    String bizCode();

    String appCode();

    String env() default "";

    String group() default "";

    String version() default "";

    long timeoutMs() default -1;
}
