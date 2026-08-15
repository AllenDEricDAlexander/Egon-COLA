package top.egon.cola.platform.rbac3.admin.iam.user.repository.jpa;

import top.egon.cola.platform.rbac3.admin.iam.user.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.po.TenantPO;

record MembershipRow(TenantPO tenant, UserPO user) {
}
