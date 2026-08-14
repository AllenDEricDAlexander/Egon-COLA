package top.egon.cola.platform.rbac3.admin.runtime.domain.vo;

import java.time.Instant;

/**
 * Runtime cache metadata for one user's authorization publication.
 */
public record RuntimeUserAuthorizationVO(
        String tenantId,
        String identitySub,
        String userId,
        String status,
        long authVersion,
        long policyVersion,
        Instant expiresAt) {
}
