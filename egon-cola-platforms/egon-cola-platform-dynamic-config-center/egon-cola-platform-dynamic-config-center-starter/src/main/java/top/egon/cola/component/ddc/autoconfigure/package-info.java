/**
 * DDC Starter 内部的 Spring Boot 条件化自动装配，显式创建客户端、服务、监听器、状态和基础设施 Bean。
 * 本包不是独立模块，不保存领域模型或业务规则；它只依赖具体实现并将其装配到公共端口。
 *
 * <p>Conditional Spring Boot automatic configuration inside the DDC Starter, explicitly wiring
 * clients, services, listeners, state, and infrastructure beans. It is not a separate module and
 * contains neither domain models nor business rules.</p>
 */
@NonNullApi
package top.egon.cola.component.ddc.autoconfigure;

import org.springframework.lang.NonNullApi;
