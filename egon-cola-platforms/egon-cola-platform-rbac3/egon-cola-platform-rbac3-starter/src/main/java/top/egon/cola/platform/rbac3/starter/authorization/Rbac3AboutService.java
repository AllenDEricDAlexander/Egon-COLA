package top.egon.cola.platform.rbac3.starter.authorization;

import top.egon.cola.platform.rbac3.contract.auth.Rbac3AboutView;
import top.egon.cola.platform.rbac3.contract.authorization.ActiveRoleDescriptor;
import top.egon.cola.platform.rbac3.starter.security.CurrentRbac3User;
import top.egon.cola.platform.rbac3.starter.security.Rbac3UserDetails;

import java.util.Objects;

/** Builds the current-user authorization context without querying the resource catalog. */
public final class Rbac3AboutService {

    private final CurrentRbac3User currentUser;

    public Rbac3AboutService(CurrentRbac3User currentUser) {
        this.currentUser = Objects.requireNonNull(currentUser, "currentUser");
    }

    public Rbac3AboutView current() {
        Rbac3UserDetails details = currentUser.require();
        String applicationCode = details.activeRoles().stream()
                .map(ActiveRoleDescriptor::applicationCode)
                .findFirst()
                .orElse(details.snapshot().systemCode());
        return new Rbac3AboutView(
                new Rbac3AboutView.User(
                        details.identitySub(), details.tenantId(), "ACTIVE"),
                applicationCode,
                details.activeRoles(),
                details.permissions(),
                details.fieldPolicies(),
                details.snapshot().landingRouteCode(),
                details.authVersion(),
                details.policyVersion());
    }
}
