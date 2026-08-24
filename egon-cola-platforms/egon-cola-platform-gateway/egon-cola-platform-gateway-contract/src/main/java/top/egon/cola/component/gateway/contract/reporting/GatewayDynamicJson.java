package top.egon.cola.component.gateway.contract.reporting;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a reporting-contract field whose value intentionally contains arbitrary JSON.
 *
 * <p>用于标记上报契约中明确承载任意 JSON 的字段边界。
 */
@Target({
        ElementType.FIELD,
        ElementType.RECORD_COMPONENT,
        ElementType.METHOD,
        ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GatewayDynamicJson {
}
