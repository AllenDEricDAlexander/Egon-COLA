package top.egon.cola.platform.rbac3.admin.identity.repository.jpa;

import top.egon.cola.platform.rbac3.admin.identity.domain.po.UserPO;
import top.egon.cola.platform.rbac3.admin.tenant.domain.po.TenantPO;

record MembershipRow(TenantPO tenant, UserPO user) {
}
