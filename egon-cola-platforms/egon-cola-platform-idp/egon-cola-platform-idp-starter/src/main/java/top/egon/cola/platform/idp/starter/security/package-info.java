/**
 * 实现 Servlet 请求中的 Bearer 凭据处理与 IdP 身份验证。
 * 该包校验 USER/SERVICE JWT 及对应运行态，把身份定位和令牌审计字段写入 Spring Security
 * 上下文，并为 SERVICE 提供 IdP Scope 本地判断；不加载用户资料或 RBAC3 服务权限。
 *
 * <p>Implements Bearer credential handling and IdP identity verification for Servlet requests. It
 * validates USER/SERVICE JWTs and their runtime state, places identity-location and token-audit
 * fields into the Spring Security context, and provides local IdP scope checks for SERVICE. It
 * loads neither user profile data nor RBAC3 service permissions.</p>
 */
package top.egon.cola.platform.idp.starter.security;
