/**
 * DDC 动态属性源与受保护本地配置键规则，维护远程配置进入 Spring Environment 的边界。
 * 本包不拉取配置、不解析 YAML，也不触发 Bean 刷新；{@code configdata} 和 {@code service.refresh} 使用这些环境能力。
 *
 * <p>Dynamic property-source support and protected local-key rules defining how remote DDC values
 * enter the Spring Environment. Retrieval, YAML parsing, and bean refresh are excluded;
 * {@code configdata} and {@code service.refresh} consume these facilities.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.environment;

import org.springframework.lang.NonNullApi;
