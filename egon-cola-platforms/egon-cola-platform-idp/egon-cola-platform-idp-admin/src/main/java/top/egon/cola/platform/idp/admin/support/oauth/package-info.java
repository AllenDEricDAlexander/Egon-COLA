/**
 * IdP 内部 OAuth 机器身份支持。
 *
 * <p>OAuth machine-identity support used internally by IdP.</p>
 *
 * <p>该包负责用 owner-only 私钥生成端点绑定 Client Assertion，并通过 IdP 自有的
 * Client Credentials 授权和签名链路获取短期 SERVICE Token；它不保存静态 Bearer Token，
 * 也不把服务权限交给 RBAC3。</p>
 *
 * <p>This package creates endpoint-bound Client Assertions from owner-only private keys and obtains
 * short-lived SERVICE tokens through IdP-owned Client Credentials authorization and signing. It
 * stores no static bearer token and delegates no service authorization to RBAC3.</p>
 */
package top.egon.cola.platform.idp.admin.support.oauth;
