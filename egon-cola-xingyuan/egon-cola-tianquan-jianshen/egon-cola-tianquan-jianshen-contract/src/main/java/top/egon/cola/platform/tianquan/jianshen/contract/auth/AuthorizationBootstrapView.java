package top.egon.cola.platform.tianquan.jianshen.contract.auth;

import top.egon.cola.platform.tianquan.jianshen.contract.authorization.ActiveRoleDescriptor;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.FieldPolicyDecision;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Typed administration-web bootstrap response shared by platform applications. */
public record AuthorizationBootstrapView(
        User user,
        List<ActiveRoleDescriptor> activeRoleContexts,
        Set<String> permissions,
        List<String> apps,
        List<String> menus,
        List<String> routes,
        List<String> actions,
        Map<String, FieldPolicyDecision> fieldPolicies,
        String defaultApplicationCode,
        String defaultRoute,
        long authVersion,
        long policyVersion) {

    /** Converts the minimal RBAC3 about view to the stable web bootstrap shape. */
    public static AuthorizationBootstrapView from(Rbac3AboutView about) {
        Rbac3AboutView.User source = about.user();
        return new AuthorizationBootstrapView(
                new User(
                        source.subject(),
                        source.tenantId(),
                        source.subject(),
                        source.status()
                ),
                about.activeRoles(),
                about.permissions(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                about.fieldPolicies(),
                about.currentApplicationCode(),
                about.landingRouteCode(),
                about.authVersion(),
                about.policyVersion()
        );
    }

    /** Current user identity exposed to administration web clients. */
    public record User(
            String id,
            String tenantId,
            String identitySub,
            String status) {
    }
}
