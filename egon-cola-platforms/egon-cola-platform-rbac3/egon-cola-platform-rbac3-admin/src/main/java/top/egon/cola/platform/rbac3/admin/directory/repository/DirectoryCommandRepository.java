package top.egon.cola.platform.rbac3.admin.directory.repository;

import top.egon.cola.platform.rbac3.admin.directory.domain.dto.DirectorySnapshotCommandDTO;
import top.egon.cola.platform.rbac3.admin.directory.domain.vo.DirectorySyncVO;
import top.egon.cola.platform.rbac3.admin.identity.domain.dto.UserStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.UserDirectoryVO;
import top.egon.cola.platform.rbac3.admin.tenant.domain.dto.CreateTenantCommandDTO;
import top.egon.cola.platform.rbac3.admin.tenant.domain.dto.TenantStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.tenant.domain.vo.TenantVO;

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
