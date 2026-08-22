package top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.repository;

import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.dto.DirectorySnapshotCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.organization.snapshot.domain.vo.DirectorySyncVO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.dto.UserStatusCommandDTO;
import top.egon.cola.platform.rbac3.admin.iam.user.domain.vo.UserDirectoryVO;

/** 目录写模型仓储契约，不拥有租户目录或身份会员关系。 */
public interface DirectoryCommandRepository {

    DirectorySyncVO submit(String tenantId, DirectorySnapshotCommandDTO command);

    UserDirectoryVO changeUserStatus(
            String tenantId, String userId, UserStatusCommandDTO command, String actorId);
}
