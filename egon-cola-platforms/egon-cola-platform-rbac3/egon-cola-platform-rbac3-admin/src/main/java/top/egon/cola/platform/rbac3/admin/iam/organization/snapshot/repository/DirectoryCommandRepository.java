package top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.repository;

import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.dto.DirectorySnapshotCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.vo.DirectorySyncVO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.dto.UserStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.dto.CreateTenantCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.dto.TenantStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.vo.TenantVO;

/**
 * 目录写模型仓储契约。
 * Repository contract for directory write models.
 */
public interface DirectoryCommandRepository {
    DirectorySyncVO submit(String tenantId, DirectorySnapshotCommandDTO command);
    TenantVO createTenant(CreateTenantCommandDTO command, String actorId);
    TenantVO changeTenantStatus(String tenantId, TenantStatusCommandDTO command, String actorId);
    UserDirectoryVO changeUserStatus(
            String tenantId, String userId, UserStatusCommandDTO command, String actorId);
}
