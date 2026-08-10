/**
 * 负责 IdP Starter 的配置绑定与 Spring Boot 自动装配。
 * 该包按配置组合 JWT 解码、实时状态读取和 Servlet 过滤器，并允许应用提供同类型 Bean 覆盖默认实现。
 *
 * <p>Owns IdP Starter configuration binding and Spring Boot auto-configuration. It assembles JWT
 * decoding, current-state reading, and Servlet filtering from settings while allowing applications
 * to replace default beans with their own implementations.</p>
 */
package top.egon.cola.platform.idp.starter.autoconfigure;
