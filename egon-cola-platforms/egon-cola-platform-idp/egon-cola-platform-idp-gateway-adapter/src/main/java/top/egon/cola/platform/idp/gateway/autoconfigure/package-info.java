/**
 * 负责 Gateway IdP 适配器的配置绑定与 Spring Boot 自动装配。
 * 该包按配置组装专用 Redis、共享验证器及 Gateway 安全 SPI 实现，并允许应用覆盖默认 Bean。
 *
 * <p>Owns configuration binding and Spring Boot auto-configuration for the Gateway IdP adapter. It
 * assembles the dedicated Redis client, shared verifier, and Gateway security SPI implementations
 * from settings while allowing applications to replace default beans.</p>
 */
package top.egon.cola.platform.idp.gateway.autoconfigure;
