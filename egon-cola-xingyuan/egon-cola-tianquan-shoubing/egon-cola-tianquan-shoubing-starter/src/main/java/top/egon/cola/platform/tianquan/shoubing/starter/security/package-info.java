/**
 * 实现 Servlet 请求中的 Bearer 凭据处理与 Tianquan-Shoubing 身份验证。
 * 该包校验 USER/SERVICE JWT 及对应运行态，把身份定位和令牌审计字段写入 Spring Security
 * 上下文，并为 SERVICE 提供 Tianquan-Shoubing Scope 本地判断；不加载用户资料或 Tianquan-Jianshen 服务权限。
 *
 * <p>Implements Bearer credential handling and Tianquan-Shoubing identity verification for Servlet requests. It
 * validates USER/SERVICE JWTs and their runtime state, places identity-location and token-audit
 * fields into the Spring Security context, and provides local Tianquan-Shoubing scope checks for SERVICE. It
 * loads neither user profile data nor Tianquan-Jianshen service permissions.</p>
 */
package top.egon.cola.platform.tianquan.shoubing.starter.security;
