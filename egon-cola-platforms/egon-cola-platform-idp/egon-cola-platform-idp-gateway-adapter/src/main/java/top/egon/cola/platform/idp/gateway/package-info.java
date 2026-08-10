/**
 * 在统一 IdP 与 Gateway 安全扩展机制之间提供适配层。
 * 模块复用 Starter 的 JWT 与用户实时状态校验规则，但通过 Gateway 原生 SPI 完成凭据提取、
 * 身份认证和可信身份传递，不引入 Servlet 过滤器。
 *
 * <p>Provides the adapter layer between the unified IdP and the Gateway security extension model.
 * The module reuses the Starter's JWT and current-user-state verification rules while using native
 * Gateway SPIs for credential extraction, authentication, and trusted-identity propagation without
 * introducing Servlet filters.</p>
 */
package top.egon.cola.platform.idp.gateway;
