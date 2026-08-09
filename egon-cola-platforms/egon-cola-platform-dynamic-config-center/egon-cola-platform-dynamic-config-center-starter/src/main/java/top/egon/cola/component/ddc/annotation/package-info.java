/**
 * DDC 配置注入与刷新标记注解，只包含应用代码可直接使用的声明式注解。
 * 本包不实现绑定、刷新或 Spring 生命周期；这些职责由 {@code service} 和 {@code autoconfigure} 承担。
 *
 * <p>Declarative annotations for DDC configuration injection and refresh. This package contains
 * only annotations used by applications; binding, refresh, and Spring lifecycle logic belongs
 * to {@code service} and {@code autoconfigure}.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.annotation;

import org.springframework.lang.NonNullApi;
