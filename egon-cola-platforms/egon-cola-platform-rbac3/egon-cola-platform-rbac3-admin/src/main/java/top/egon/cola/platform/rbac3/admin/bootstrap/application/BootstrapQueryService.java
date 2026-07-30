package top.egon.cola.platform.rbac3.admin.bootstrap.application;

import top.egon.cola.platform.rbac3.contract.auth.BootstrapView;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.Objects;
import java.util.Optional;

/**
 * Exposes business bootstrap data only for a session with active roles.
 */
public final class BootstrapQueryService {

    private final BootstrapSnapshotSource snapshotSource;

    public BootstrapQueryService(BootstrapSnapshotSource snapshotSource) {
        this.snapshotSource = Objects.requireNonNull(snapshotSource, "snapshotSource");
    }

    public BootstrapView query(String tenantId, String userId, String sessionId) {
        return snapshotSource.find(tenantId, userId, sessionId)
                .filter(view -> !view.activeRoleContexts().isEmpty())
                .orElseThrow(() -> new Rbac3RuleViolation("ROLE_ACTIVATION_REQUIRED"));
    }

    @FunctionalInterface
    public interface BootstrapSnapshotSource {

        Optional<BootstrapView> find(String tenantId, String userId, String sessionId);
    }
}
