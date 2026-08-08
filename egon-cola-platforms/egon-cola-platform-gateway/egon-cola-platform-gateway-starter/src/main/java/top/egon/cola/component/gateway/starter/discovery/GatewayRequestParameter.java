package top.egon.cola.component.gateway.starter.discovery;

import com.fasterxml.jackson.databind.JavaType;
import top.egon.cola.component.gateway.starter.annotation.GatewayRequestLocation;

import java.lang.reflect.AnnotatedElement;

/**
 * Describes one HTTP-bound handler parameter.
 * 中文说明：记录请求位置、外部名称、必填性、展开状态及用于生成 Schema 的类型信息。
 *
 * @param location request location
 * @param name external parameter name, or an empty string for root values
 * @param required whether the parameter is required
 * @param expanded whether an object is expanded into query properties
 * @param javaType resolved Java type
 * @param annotatedElement source element carrying schema annotations
 * @param defaultValue textual binding default, or {@code null}
 */
public record GatewayRequestParameter(
        GatewayRequestLocation location,
        String name,
        boolean required,
        boolean expanded,
        JavaType javaType,
        AnnotatedElement annotatedElement,
        String defaultValue
) {
}
