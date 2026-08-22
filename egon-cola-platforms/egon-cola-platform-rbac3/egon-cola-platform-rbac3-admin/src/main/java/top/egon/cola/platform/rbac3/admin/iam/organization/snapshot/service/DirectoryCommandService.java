package top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.service;

import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.dto.DirectorySnapshotCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.vo.DirectorySyncVO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.dto.UserStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.UserDirectoryVO;

/** 目录写服务，仅保留快照同步与租户范围内的用户状态变更。 */
public interface DirectoryCommandService {

    DirectorySyncVO submit(String tenantId, DirectorySnapshotCommandDTO command);

    UserDirectoryVO changeUserStatus(
            String tenantId, String userId, UserStatusCommandDTO command, String actorId);
}
