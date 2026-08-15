package top.egon.cola.platform.rbac3.admin.runtime.domain.dto;

import top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo.ActivationFactsVO;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationResolution;

import java.time.Instant;

/**
 * Inputs required to create the user authorization snapshot.
 */
public record ProjectionCommandDTO(
        String tenantId,
        String identitySub,
        String userId,
        long authVersion,
        long policyVersion,
        Instant expiresAt,
        RoleActivationResolution resolution,
        ActivationFactsVO facts,
        Instant generatedAt) {

    public ProjectionCommandDTO {
        if (tenantId == null || tenantId.isBlank()
                || identitySub == null || identitySub.isBlank()
                || userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("tenant, identity subject and user are required");
        }
        if (authVersion < 0 || policyVersion < 0) {
            throw new IllegalArgumentException("authorization versions must not be negative");
        }
    }
}
