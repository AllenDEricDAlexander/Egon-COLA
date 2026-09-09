package top.egon.cola.platform.tianquan.jianshen.admin.authorization.policy.management.domain.vo;

/**
 * 可管理的租户成员标识；用户名及个人资料归 IdP 所有，不从 RBAC 用户表读取。
 * Manageable tenant membership identifiers; IdP owns usernames and profile data.
 *
 * @param userId RBAC 租户成员 ID；RBAC tenant membership ID.
 * @param identitySub IdP 身份标识；IdP subject identifier.
 */
public record ManagedUserVO(String userId, String identitySub) {
}
