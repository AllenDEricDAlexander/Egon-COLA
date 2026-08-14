package top.egon.cola.platform.rbac3.core.decision;

import top.egon.cola.platform.rbac3.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.rbac3.core.activation.ActivationAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class UserAuthorizationSnapshotBuilder {

    public static final int MAX_ROLE_FAMILY_PER_ROOT = 2048;
    public static final int MAX_USER_PERMISSIONS = 16_384;
    public static final int MAX_USER_RESOURCES = 50_000;
    public static final int MAX_SNAPSHOT_BYTES = 8 * 1024 * 1024;

    public ActivationAuthorizationSnapshot build(
            Set<String> rootRoleIds,
            RoleHierarchy hierarchy,
            AuthorizationRuleFacts facts,
            long authVersion,
            long nextAuthVersion,
            long policyVersion
    ) {
        var effectiveRoleIds = new TreeSet<String>();
        for (String rootRoleId : new TreeSet<>(rootRoleIds)) {
            Set<String> family = hierarchy.descendantsIncludingSelf(rootRoleId);
            if (family.size() > MAX_ROLE_FAMILY_PER_ROOT) {
                throw new Rbac3RuleViolation("ROLE_FAMILY_SIZE_LIMIT_EXCEEDED",
                        java.util.List.of(rootRoleId));
            }
            effectiveRoleIds.addAll(family);
        }

        Set<String> permissions = new PermissionSetMerger().merge(
                facts.permissionBindings(), effectiveRoleIds);
        if (permissions.size() > MAX_USER_PERMISSIONS) {
            throw new Rbac3RuleViolation("ROLE_FAMILY_SIZE_LIMIT_EXCEEDED");
        }
        Map<String, DataScopeMerger.NormalizedDataScope> scopes =
                new DataScopeMerger().merge(facts.dataScopeFacts(), effectiveRoleIds);
        Map<String, FieldAccessLevel> fieldPolicies = new FieldPolicyMerger().merge(
                facts.fieldRuleFacts(), facts.fieldDefinitions(), effectiveRoleIds);
        var resources = new TreeSet<String>();
        for (AuthorizationRuleFacts.ResourceFact resource : facts.resources()) {
            if (permissions.contains(resource.requiredPermissionCode())) {
                resources.add(resource.code());
            }
        }
        if (resources.size() > MAX_USER_RESOURCES) {
            throw new Rbac3RuleViolation("ROLE_FAMILY_SIZE_LIMIT_EXCEEDED");
        }
        String landingRoute = new LandingRouteSelector().select(
                facts.landingRoutes(), effectiveRoleIds, permissions).orElse(null);
        String canonical = canonical(rootRoleIds, effectiveRoleIds, permissions,
                scopes, fieldPolicies, resources, landingRoute,
                authVersion, nextAuthVersion, policyVersion);
        if (canonical.getBytes(StandardCharsets.UTF_8).length > MAX_SNAPSHOT_BYTES) {
            throw new Rbac3RuleViolation("ROLE_FAMILY_SIZE_LIMIT_EXCEEDED");
        }
        return new ActivationAuthorizationSnapshot(
                effectiveRoleIds, permissions, scopes, fieldPolicies, resources,
                landingRoute, nextAuthVersion, policyVersion,
                "sha256:" + sha256(canonical)
        );
    }

    private String canonical(
            Set<String> roots,
            Set<String> roles,
            Set<String> permissions,
            Map<String, DataScopeMerger.NormalizedDataScope> scopes,
            Map<String, FieldAccessLevel> fields,
            Set<String> resources,
            String landingRoute,
            long authVersion,
            long nextAuthVersion,
            long policyVersion
    ) {
        var normalizedScopes = new TreeMap<String, DataScopeMerger.NormalizedDataScope>(scopes);
        var normalizedFields = new TreeMap<String, FieldAccessLevel>(fields);
        return "roots=" + new TreeSet<>(roots)
                + "|roles=" + new TreeSet<>(roles)
                + "|permissions=" + new TreeSet<>(permissions)
                + "|scopes=" + normalizedScopes
                + "|fields=" + normalizedFields
                + "|resources=" + new TreeSet<>(resources)
                + "|landing=" + landingRoute
                + "|av=" + authVersion
                + "|nextAv=" + nextAuthVersion
                + "|pv=" + policyVersion;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
