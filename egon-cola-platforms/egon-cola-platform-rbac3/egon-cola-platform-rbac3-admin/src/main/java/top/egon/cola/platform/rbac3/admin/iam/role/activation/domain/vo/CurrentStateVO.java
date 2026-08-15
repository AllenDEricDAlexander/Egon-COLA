package top.egon.cola.platform.rbac3.admin.iam.role.activation.domain.vo;

import java.util.Map;
import java.util.Set;

/**
 * Current user active-role state returned by the transaction boundary.
 */
public record CurrentStateVO(
        Map<String, Set<String>> rootsByApplication,
        long authVersion,
        long policyVersion,
        String snapshotChecksum,
        boolean activationRequired) {
}
