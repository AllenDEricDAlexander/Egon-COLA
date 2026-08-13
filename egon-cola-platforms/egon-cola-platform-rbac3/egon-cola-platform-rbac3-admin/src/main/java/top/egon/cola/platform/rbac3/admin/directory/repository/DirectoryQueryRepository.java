package top.egon.cola.platform.rbac3.admin.directory.repository;

import top.egon.cola.platform.rbac3.admin.directory.domain.vo.DirectoryPageVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.DirectorySnapshotVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.OrgUnitVO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.PositionVO;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.rbac3.admin.tenant.domain.vo.TenantVO;

import java.util.List;

/**
 * 目录读模型仓储契约。
 * Repository contract for directory read models.
 */
public interface DirectoryQueryRepository {
    UserDirectoryVO findUser(String tenantId, String userId);
    TenantVO findTenant(String tenantId);
    DirectoryPageVO<TenantVO> findTenants(String query, String status, int page, int size);
    DirectoryPageVO<UserDirectoryVO> findUsers(
            String tenantId, String query, String status, String orgUnitId,
            String positionId, int page, int size);
    List<OrgUnitVO> findOrgUnits(String tenantId, String parentId, String type, String status);
    List<PositionVO> findPositions(String tenantId, String orgUnitId, String status);
    DirectorySnapshotVO findSnapshot(String tenantId, String snapshotId);
}
