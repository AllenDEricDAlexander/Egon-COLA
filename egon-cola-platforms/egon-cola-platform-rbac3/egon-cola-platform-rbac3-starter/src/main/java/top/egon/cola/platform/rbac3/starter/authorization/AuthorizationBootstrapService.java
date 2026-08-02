package top.egon.cola.platform.rbac3.starter.authorization;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Builds the browser bootstrap view from the current bound authorization context. */
public final class AuthorizationBootstrapService {

    private final AuthorizationService.RuntimeContextSource contextSource;

    public AuthorizationBootstrapService(
            AuthorizationService.RuntimeContextSource contextSource) {
        this.contextSource = Objects.requireNonNull(contextSource, "contextSource");
    }

    public BootstrapView current() {
        var context = contextSource.load();
        var identity = context.identity();
        var snapshot = context.snapshot();
        return new BootstrapView(
                identity.subject(), identity.tenantId(), identity.sessionId(),
                snapshot.rbac3UserId(), snapshot.systemCode(),
                snapshot.permissions().stream().sorted().toList(),
                snapshot.activeRoleIds().stream().sorted().toList(),
                snapshot.authVersion(), snapshot.contextVersion(),
                snapshot.policyVersion(), snapshot.generatedAt(), snapshot.expiresAt());
    }

    public record BootstrapView(
            String identitySub,
            String tenantId,
            String sessionId,
            String rbac3UserId,
            String systemCode,
            List<String> permissions,
            List<String> activeRoleIds,
            long authVersion,
            long contextVersion,
            long policyVersion,
            Instant generatedAt,
            Instant expiresAt) {

        public BootstrapView {
            permissions = List.copyOf(permissions);
            activeRoleIds = List.copyOf(activeRoleIds);
        }
    }
}
