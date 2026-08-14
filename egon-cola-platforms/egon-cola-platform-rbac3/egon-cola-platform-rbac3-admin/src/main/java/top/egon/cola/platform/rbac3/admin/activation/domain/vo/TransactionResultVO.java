package top.egon.cola.platform.rbac3.admin.activation.domain.vo;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Result of a user active-role replacement.
 */
public record TransactionResultVO(
        ResolvedActivationVO resolved,
        boolean changed,
        String mutationId,
        Map<String, Set<String>> rootsByApplication,
        long authVersion,
        long policyVersion,
        String snapshotChecksum,
        Instant expiresAt) {
}
