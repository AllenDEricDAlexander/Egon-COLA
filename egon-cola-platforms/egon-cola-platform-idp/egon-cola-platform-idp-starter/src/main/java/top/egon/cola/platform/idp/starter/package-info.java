/**
 * 为普通 Servlet 应用提供统一 IdP 资源服务器接入能力。
 * 该模块验证外部 IdP 已签发的访问令牌，读取用户实时状态并形成统一身份上下文；
 * 它不是 OAuth2 授权服务器，不负责客户端准入、令牌签发或业务权限决策。
 *
 * <p>Provides unified IdP resource-server integration for regular Servlet applications. The module
 * validates access tokens already issued by the external IdP, reads current user state, and builds
 * a unified identity context. It is not an OAuth2 authorization server and does not own client
 * onboarding, token issuance, or business authorization decisions.</p>
 */
package top.egon.cola.platform.idp.starter;
