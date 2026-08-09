/**
 * DDC 配置应用与应用器注册公共端口，定义配置值进入应用运行态的扩展边界。
 * 本包不实现 YAML 解析、字段绑定或 Bean 重绑定；默认编排位于 {@code service.refresh}。
 *
 * <p>Public ports for applying DDC configuration and registering appliers. YAML parsing, field
 * binding, and bean rebinding are excluded and implemented by {@code service.refresh} and related
 * service packages.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.api.refresh;

import org.springframework.lang.NonNullApi;
