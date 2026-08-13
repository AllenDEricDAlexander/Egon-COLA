package top.egon.cola.platform.rbac3.admin.runtime.repository;

import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.DdcProviderLeaseStatusVO;

/** DDC 提供者租约状态查询端口。 DDC provider-lease status query port. */
@FunctionalInterface
public interface ProviderLeaseStatusPort {

    /** @return 当前提供者租约状态；current provider-lease status */
    DdcProviderLeaseStatusVO status();
}
