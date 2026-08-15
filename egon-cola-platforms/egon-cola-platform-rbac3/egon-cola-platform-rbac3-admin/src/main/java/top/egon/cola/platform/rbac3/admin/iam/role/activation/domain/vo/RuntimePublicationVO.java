package top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo;

import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.UserSnapshotProjectionVO;

/**
 * User authorization projection to publish to the runtime cache.
 */
public record RuntimePublicationVO(
        String tenantId,
        String identitySub,
        String userId,
        long authVersion,
        long policyVersion,
        UserSnapshotProjectionVO projection) {
}
