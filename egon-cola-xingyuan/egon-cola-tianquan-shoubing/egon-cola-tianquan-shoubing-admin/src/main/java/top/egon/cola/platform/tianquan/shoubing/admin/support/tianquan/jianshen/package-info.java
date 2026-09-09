/**
 * 适配 Tianquan-Jianshen 的 USER Resource 入口决策和用户租户成员关系。
 *
 * <p>Adapts Tianquan-Jianshen USER Resource entry decisions and user tenant memberships.</p>
 *
 * <p>Tianquan-Shoubing 使用自身签发的短期 SERVICE Token 认证到 Tianquan-Jianshen 的内部 HTTP 调用，但服务间
 * Resource、租户和 Scope 授权始终由 Tianquan-Shoubing 维护和判定；Tianquan-Jianshen 不保存或判断 SERVICE 权限。</p>
 *
 * <p>Tianquan-Shoubing authenticates its internal HTTP calls to Tianquan-Jianshen with its own short-lived SERVICE token,
 * while inter-service Resource, tenant, and scope authorization remains owned and decided by Tianquan-Shoubing.
 * Tianquan-Jianshen stores and decides no SERVICE permission.</p>
 */
package top.egon.cola.platform.tianquan.shoubing.admin.support.tianquan.jianshen;
