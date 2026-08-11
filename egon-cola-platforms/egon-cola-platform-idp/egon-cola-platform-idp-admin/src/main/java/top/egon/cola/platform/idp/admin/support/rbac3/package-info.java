/**
 * 适配 RBAC3 的 USER Resource 入口决策和用户租户成员关系。
 *
 * <p>Adapts RBAC3 USER Resource entry decisions and user tenant memberships.</p>
 *
 * <p>IdP 使用自身签发的短期 SERVICE Token 认证到 RBAC3 的内部 HTTP 调用，但服务间
 * Resource、租户和 Scope 授权始终由 IdP 维护和判定；RBAC3 不保存或判断 SERVICE 权限。</p>
 *
 * <p>IdP authenticates its internal HTTP calls to RBAC3 with its own short-lived SERVICE token,
 * while inter-service Resource, tenant, and scope authorization remains owned and decided by IdP.
 * RBAC3 stores and decides no SERVICE permission.</p>
 */
package top.egon.cola.platform.idp.admin.support.rbac3;
