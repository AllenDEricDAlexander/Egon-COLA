package top.egon.cola.platform.rbac3.starter.authorization;

import top.egon.cola.platform.rbac3.contract.auth.BootstrapView;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds a bootstrap view from the current USER authorization projection.
 */
public final class AuthorizationBootstrapService {
    private final AuthorizationService.RuntimeContextSource contextSource;

    public AuthorizationBootstrapService(AuthorizationService.RuntimeContextSource contextSource) {
        this.contextSource = Objects.requireNonNull(contextSource, "contextSource");
    }

    public BootstrapView current() {
        var context = contextSource.load();
        var identity = context.identity();
        var snapshot = context.snapshot();
        return new BootstrapView(
                new BootstrapView.User(identity.subject(), identity.tenantId(), identity.subject(), "ACTIVE"),
                List.of(),
                snapshot.permissions(),
                List.of(), List.of(), List.of(), List.of(),
                Map.of(), null, null,
                snapshot.authVersion(), snapshot.policyVersion());
    }
}
