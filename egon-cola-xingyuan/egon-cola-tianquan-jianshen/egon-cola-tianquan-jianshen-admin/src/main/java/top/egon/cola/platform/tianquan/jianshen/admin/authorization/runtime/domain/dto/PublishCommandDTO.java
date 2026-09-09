package top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.dto;

import top.egon.cola.platform.tianquan.jianshen.admin.authorization.runtime.domain.vo.UserSnapshotProjectionVO;

/**
 * Immutable user-scoped authorization publication command.
 */
public record PublishCommandDTO(
        String tenantId,
        String identitySub,
        String userId,
        long authVersion,
        long policyVersion,
        UserSnapshotProjectionVO projection) {
}
