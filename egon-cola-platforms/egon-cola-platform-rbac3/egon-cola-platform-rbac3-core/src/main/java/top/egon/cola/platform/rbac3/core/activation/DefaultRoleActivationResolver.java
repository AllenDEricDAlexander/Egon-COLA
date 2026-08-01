package top.egon.cola.platform.rbac3.core.activation;

import top.egon.cola.platform.rbac3.core.decision.SessionAuthorizationSnapshotBuilder;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchyValidator;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class DefaultRoleActivationResolver implements RoleActivationResolver {

    public static final int MAX_ACTIVE_ROOTS = 32;

    private final RoleActivationCandidateResolver candidateResolver;
    private final UniqueActivationRootSpecification uniqueRoot;
    private final ApplicationRoleMutexSpecification mutex;
    private final SessionAuthorizationSnapshotBuilder snapshotBuilder;

    public DefaultRoleActivationResolver() {
        this(new RoleActivationCandidateResolver(),
                new UniqueActivationRootSpecification(),
                new ApplicationRoleMutexSpecification(),
                new SessionAuthorizationSnapshotBuilder());
    }

    public DefaultRoleActivationResolver(
            RoleActivationCandidateResolver candidateResolver,
            UniqueActivationRootSpecification uniqueRoot,
            ApplicationRoleMutexSpecification mutex,
            SessionAuthorizationSnapshotBuilder snapshotBuilder
    ) {
        this.candidateResolver = candidateResolver;
        this.uniqueRoot = uniqueRoot;
        this.mutex = mutex;
        this.snapshotBuilder = snapshotBuilder;
    }

    @Override
    public RoleActivationResolution resolve(RoleActivationInput input) {
        validateRequest(input.requestedRoleIds());
        new RoleHierarchyValidator().validate(input.hierarchy());
        Map<String, Set<String>> candidates = candidateResolver.resolve(
                input.assignments(), input.hierarchy(), input.databaseNow());

        var normalizedRoots = new TreeSet<String>();
        try {
            for (String requestedRoleId : input.requestedRoleIds()) {
                String root = uniqueRoot.requireUniqueRoot(
                        requestedRoleId, input.hierarchy());
                if (!candidates.containsKey(root)) {
                    throw new Rbac3RuleViolation(
                            "ROLE_ACTIVATION_ASSIGNMENT_REQUIRED",
                            java.util.List.of(requestedRoleId)
                    );
                }
                normalizedRoots.add(root);
            }
        } catch (IllegalArgumentException error) {
            throw new Rbac3RuleViolation("ROLE_ACTIVATION_SET_INVALID");
        }
        if (normalizedRoots.isEmpty() || normalizedRoots.size() > MAX_ACTIVE_ROOTS) {
            throw new Rbac3RuleViolation("ROLE_ACTIVATION_SET_INVALID");
        }

        var rootsByApplication = new TreeMap<String, Set<String>>();
        for (String root : normalizedRoots) {
            RoleNode node = input.hierarchy().requireNode(root);
            if (!node.active()) {
                throw new Rbac3RuleViolation("ROLE_ACTIVATION_SET_INVALID",
                        java.util.List.of(root));
            }
            rootsByApplication.computeIfAbsent(node.applicationId(), ignored -> new TreeSet<>())
                    .add(root);
        }
        mutex.requireAllowed(rootsByApplication, input.dsdSets());

        var snapshot = snapshotBuilder.build(normalizedRoots, input.hierarchy(),
                input.authorizationFacts(), input.authVersion(),
                input.sessionVersion() + 1, input.policyVersion());
        var normalizedApplicationRoots = new TreeMap<String, Set<String>>();
        rootsByApplication.forEach((application, roots) -> normalizedApplicationRoots.put(
                application, Collections.unmodifiableSet(new TreeSet<>(roots))));
        ActiveRoleSet activeRoleSet = new ActiveRoleSet(
                input.tenantId(), input.userId(), input.sessionId(),
                normalizedApplicationRoots, snapshot.checksum());

        var evidence = new TreeSet<String>();
        normalizedRoots.forEach(root -> evidence.addAll(candidates.get(root)));
        return new RoleActivationResolution(activeRoleSet, snapshot,
                new ArrayList<>(evidence));
    }

    private void validateRequest(java.util.List<String> requestedRoleIds) {
        if (requestedRoleIds.isEmpty() || requestedRoleIds.size() > MAX_ACTIVE_ROOTS) {
            throw new Rbac3RuleViolation("ROLE_ACTIVATION_SET_INVALID");
        }
        var distinct = new LinkedHashSet<String>();
        for (String roleId : requestedRoleIds) {
            if (roleId == null || roleId.isBlank() || !distinct.add(roleId.trim())) {
                throw new Rbac3RuleViolation("ROLE_ACTIVATION_SET_INVALID");
            }
        }
    }
}
