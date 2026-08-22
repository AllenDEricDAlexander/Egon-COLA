package top.egon.cola.platform.rbac3.admin.iam.organization.service;

import top.egon.cola.platform.rbac3.admin.iam.organization.domain.vo.DirectoryPageVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.vo.OrgUnitVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.vo.DirectorySnapshotVO;
import top.egon.cola.platform.rbac3.admin.iam.position.domain.vo.PositionVO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.UserDirectoryVO;

import java.util.List;

/** 目录查询服务，仅暴露租户范围内的用户、组织和快照查询。 */
public interface DirectoryQueryService {

    UserDirectoryVO findUser(String tenantId, String userId);

    DirectoryPageVO<UserDirectoryVO> findUsers(
            String tenantId, String query, String status, String orgUnitId,
            String positionId, int page, int size);

    List<OrgUnitVO> findOrgUnits(
            String tenantId, String parentId, String type, String status);

    List<PositionVO> findPositions(
            String tenantId, String orgUnitId, String status);

    DirectorySnapshotVO findSnapshot(String tenantId, String snapshotId);
}
