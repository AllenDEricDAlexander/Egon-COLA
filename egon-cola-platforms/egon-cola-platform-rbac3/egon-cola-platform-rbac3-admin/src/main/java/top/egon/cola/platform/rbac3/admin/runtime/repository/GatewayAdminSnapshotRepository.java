package top.egon.cola.platform.rbac3.admin.runtime.repository;

import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayAdminSnapshotVO;

/** Gateway Admin 控制面快照查询端口。 Gateway Admin control-plane snapshot port. */
@FunctionalInterface
public interface GatewayAdminSnapshotRepository {

    /** @return 当前 Gateway Admin 快照；current Gateway Admin snapshot */
    GatewayAdminSnapshotVO snapshot();
}
