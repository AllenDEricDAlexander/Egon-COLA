/**
 * 实现 Servlet 请求中的 Bearer 凭据处理与 IdP 身份验证。
 * 该包校验 JWT 及用户实时状态，把身份定位和令牌审计字段写入 Spring Security 上下文，
 * 不加载用户资料，也不授予业务权限。
 *
 * <p>Implements Bearer credential handling and IdP identity verification for Servlet requests. It
 * validates JWTs and current user state and places identity-location and token-audit fields into the
 * Spring Security context. It neither loads user profile data nor grants business authorities.</p>
 */
package top.egon.cola.platform.idp.starter.security;
