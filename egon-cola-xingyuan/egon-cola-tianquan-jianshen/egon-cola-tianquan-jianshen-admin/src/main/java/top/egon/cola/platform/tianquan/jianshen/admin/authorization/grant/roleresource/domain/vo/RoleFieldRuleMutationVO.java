package top.egon.cola.platform.tianquan.jianshen.admin.authorization.grant.roleresource.domain.vo;

/** Immutable committed rule/version receipt; projection readiness is checked separately. */
public record RoleFieldRuleMutationVO(String roleId, long roleVersion, int ruleCount, int addedCount, int changedCount,
                                      int disabledCount, long authVersion, long policyVersion, String status) {
    public RoleFieldRuleMutationVO {
        if (roleId == null || !roleId.matches("[1-9][0-9]{0,18}") || roleVersion < 0 || ruleCount < 0 || ruleCount > 256
                || addedCount < 0 || changedCount < 0 || disabledCount < 0 || authVersion < 0 || policyVersion < 0
                || !"CONFIRMED".equals(status)) { throw new IllegalArgumentException("Role field-rule receipt is invalid"); }
        try { Long.parseLong(roleId); }
        catch (NumberFormatException invalid) { throw new IllegalArgumentException("Role field-rule ID is invalid", invalid); }
    }
}
