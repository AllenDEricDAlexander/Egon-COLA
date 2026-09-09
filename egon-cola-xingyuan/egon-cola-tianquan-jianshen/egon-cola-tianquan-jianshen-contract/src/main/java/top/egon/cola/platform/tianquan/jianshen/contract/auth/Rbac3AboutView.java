package top.egon.cola.platform.tianquan.jianshen.contract.auth;

import top.egon.cola.platform.tianquan.jianshen.contract.authorization.ActiveRoleDescriptor;
import top.egon.cola.platform.tianquan.jianshen.contract.authorization.FieldPolicyDecision;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/** Minimal current-user authorization context; it deliberately contains no resource tree. */
public record Rbac3AboutView(
        User user,
        String currentApplicationCode,
        List<ActiveRoleDescriptor> activeRoles,
        Set<String> permissions,
        Set<String> resourceCodes,
        Map<String, FieldPolicyDecision> fieldPolicies,
        String landingRouteCode,
        long authVersion,
        long policyVersion) {

    public Rbac3AboutView {
        user = Objects.requireNonNull(user, "user");
        currentApplicationCode = optional(currentApplicationCode, "currentApplicationCode");
        activeRoles = List.copyOf(Objects.requireNonNull(activeRoles, "activeRoles"));
        permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions"));
        resourceCodes = immutableSortedSet(resourceCodes, "resourceCodes");
        fieldPolicies = Map.copyOf(Objects.requireNonNull(fieldPolicies, "fieldPolicies"));
        landingRouteCode = optional(landingRouteCode, "landingRouteCode");
        nonNegative(authVersion, "authVersion");
        nonNegative(policyVersion, "policyVersion");
    }

    public record User(String subject, String tenantId, String status) {
        public User {
            subject = required(subject, "user.subject");
            tenantId = required(tenantId, "user.tenantId");
            status = required(status, "user.status");
        }
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    private static String optional(String value, String name) {
        return value == null ? null : required(value, name);
    }

    private static void nonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }

    private static Set<String> immutableSortedSet(
            Set<String> values,
            String name) {
        Objects.requireNonNull(values, name);
        TreeSet<String> sorted = new TreeSet<>();
        for (String value : values) {
            sorted.add(required(value, name));
        }
        return Collections.unmodifiableSet(sorted);
    }
}
