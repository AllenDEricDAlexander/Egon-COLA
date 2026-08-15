package top.egon.cola.platform.rbac3.admin.iam.business.repository;

import top.egon.cola.platform.rbac3.admin.iam.business.domain.command.ReplaceUserBusinessAccessesCommand;
import top.egon.cola.platform.rbac3.admin.iam.business.domain.vo.UserApplicationAccessVO;
import top.egon.cola.platform.rbac3.admin.iam.business.domain.vo.UserBusinessAccessVO;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Persistence port for RBAC-owned User Business authorization facts. */
public interface UserBusinessAccessRepository {

    List<UserBusinessAccessVO> accesses(Long tenantId, Long userId);

    List<UserBusinessAccessVO> replaceManualAccesses(
            Long tenantId,
            Long userId,
            List<ReplaceUserBusinessAccessesCommand.Item> desired,
            String actorId,
            Instant now);

    Set<String> effectiveBusinessIds(Long tenantId, Long userId, Instant at);

    List<UserApplicationAccessVO> applicationAccesses(
            Long tenantId,
            Long userId,
            Instant at);
}
