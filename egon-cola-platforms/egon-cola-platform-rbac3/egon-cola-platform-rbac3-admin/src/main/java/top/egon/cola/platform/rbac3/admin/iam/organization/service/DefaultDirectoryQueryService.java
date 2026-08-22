package top.egon.cola.platform.rbac3.admin.iam.organization.service;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.vo.DirectoryPageVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.vo.DirectorySnapshotVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.domain.vo.OrgUnitVO;
import top.egon.cola.platform.rbac3.admin.iam.position.domain.vo.PositionVO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.repository.DirectoryQueryRepository;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.UserDirectoryVO;

import java.util.List;

/** 目录查询服务的默认实现。 Default directory query service. */
@Service
@Primary
public class DefaultDirectoryQueryService implements DirectoryQueryService {
    private final DirectoryQueryRepository repository;
    public DefaultDirectoryQueryService(DirectoryQueryRepository repository) {
        this.repository = repository;
    }
    @Override public UserDirectoryVO findUser(String tenantId, String userId) {
        return repository.findUser(tenantId, userId);
    }
    @Override public DirectoryPageVO<UserDirectoryVO> findUsers(
            String tenantId, String query, String status, String orgUnitId,
            String positionId, int page, int size) {
        return repository.findUsers(tenantId, query, status, orgUnitId, positionId, page, size);
    }
    @Override public List<OrgUnitVO> findOrgUnits(
            String tenantId, String parentId, String type, String status) {
        return repository.findOrgUnits(tenantId, parentId, type, status);
    }
    @Override public List<PositionVO> findPositions(
            String tenantId, String orgUnitId, String status) {
        return repository.findPositions(tenantId, orgUnitId, status);
    }
    @Override public DirectorySnapshotVO findSnapshot(String tenantId, String snapshotId) {
        return repository.findSnapshot(tenantId, snapshotId);
    }
}
