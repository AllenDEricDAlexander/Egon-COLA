package top.egon.cola.platform.rbac3.admin.activation.application;

import top.egon.cola.platform.rbac3.contract.activation.RoleActivationCandidate;
import top.egon.cola.platform.rbac3.contract.activation.RoleActivationCandidateView;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.activation.DsdSetFact;
import top.egon.cola.platform.rbac3.core.activation.EligibleAssignmentFact;
import top.egon.cola.platform.rbac3.core.activation.RoleActivationCandidateResolver;
import top.egon.cola.platform.rbac3.core.activation.UniqueActivationRootSpecification;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Converts assignment and hierarchy facts into deterministic activation candidates.
 */
public final class RoleActivationCandidateService {

    private final ActivationFactSource factSource;
    private final RoleActivationCandidateResolver candidateResolver;

    public RoleActivationCandidateService(ActivationFactSource factSource) {
        this(factSource, new RoleActivationCandidateResolver());
    }

    RoleActivationCandidateService(
            ActivationFactSource factSource,
            RoleActivationCandidateResolver candidateResolver
    ) {
        this.factSource = Objects.requireNonNull(factSource, "factSource");
        this.candidateResolver = Objects.requireNonNull(
                candidateResolver, "candidateResolver");
    }

    public RoleActivationCandidateView candidates(
            String tenantId,
            String userId,
            Instant databaseNow
    ) {
        ActivationFacts facts = factSource.load(tenantId, userId, databaseNow);
        Map<String, Set<String>> assignmentIdsByRoot = candidateResolver.resolve(
                facts.assignments(), facts.hierarchy(), databaseNow);
        var rootsByApplication = new TreeMap<String, List<RoleActivationCandidate>>();
        for (Map.Entry<String, Set<String>> entry : assignmentIdsByRoot.entrySet()) {
            String rootId = entry.getKey();
            RoleNode root = facts.hierarchy().requireNode(rootId);
            rootsByApplication.computeIfAbsent(
                    root.applicationId(), ignored -> new ArrayList<>()).add(
                    candidate(rootId, entry.getValue(), facts, databaseNow));
        }
        var applications = new ArrayList<RoleActivationCandidateView.ApplicationCandidates>();
        rootsByApplication.forEach((applicationId, candidates) -> {
            ApplicationFact application = facts.applications().get(applicationId);
            if (application == null) {
                throw new IllegalArgumentException(
                        "missing application fact: " + applicationId);
            }
            candidates.sort(Comparator.comparing(RoleActivationCandidate::rootRoleCode));
            applications.add(new RoleActivationCandidateView.ApplicationCandidates(
                    application.id(), application.code(), candidates));
        });
        applications.sort(Comparator.comparing(
                RoleActivationCandidateView.ApplicationCandidates::applicationCode));
        return new RoleActivationCandidateView(
                applications,
                facts.authVersion(),
                facts.policyVersion(),
                facts.directorySnapshotVersion(),
                List.of(),
                databaseNow);
    }

    private RoleActivationCandidate candidate(
            String rootId,
            Set<String> eligibleAssignmentIds,
            ActivationFacts facts,
            Instant databaseNow
    ) {
        Set<String> sourceRoleIds = new TreeSet<>();
        var rootSpecification = new UniqueActivationRootSpecification();
        for (EligibleAssignmentFact assignment : facts.assignments()) {
            if (assignment.eligibleAt(databaseNow)
                    && rootId.equals(rootSpecification.requireUniqueRoot(
                    assignment.roleId(), facts.hierarchy()))) {
                sourceRoleIds.add(assignment.roleId());
            }
        }
        Set<String> family = facts.hierarchy().descendantsIncludingSelf(rootId);
        RoleNode.RiskLevel risk = family.stream()
                .map(id -> facts.hierarchy().requireNode(id).riskLevel())
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(RoleNode.RiskLevel.LOW);
        List<String> mutexSetIds = facts.dsdSets().stream()
                .filter(set -> set.rootRoleIds().contains(rootId))
                .map(DsdSetFact::id)
                .sorted()
                .toList();
        RoleNode root = facts.hierarchy().requireNode(rootId);
        return new RoleActivationCandidate(
                rootId,
                root.code(),
                facts.roleDisplayNames().getOrDefault(rootId, root.code()),
                new ArrayList<>(sourceRoleIds),
                new ArrayList<>(new TreeSet<>(eligibleAssignmentIds)),
                mutexSetIds,
                risk.name(),
                requiredStrength(risk),
                root.landingRouteCode());
    }

    private String requiredStrength(RoleNode.RiskLevel risk) {
        return switch (risk) {
            case LOW, MEDIUM -> "PASSWORD";
            case HIGH -> "MFA";
            case CRITICAL -> "STRONG";
        };
    }

    @FunctionalInterface
    public interface ActivationFactSource {

        ActivationFacts load(String tenantId, String userId, Instant databaseNow);
    }

    public record ActivationFacts(
            String tenantId,
            String userId,
            RoleHierarchy hierarchy,
            List<EligibleAssignmentFact> assignments,
            List<DsdSetFact> dsdSets,
            AuthorizationRuleFacts authorizationFacts,
            long authVersion,
            long policyVersion,
            String directorySnapshotVersion,
            Map<String, ApplicationFact> applications,
            Map<String, String> roleDisplayNames
    ) {

        public ActivationFacts {
            Objects.requireNonNull(tenantId, "tenantId");
            Objects.requireNonNull(userId, "userId");
            Objects.requireNonNull(hierarchy, "hierarchy");
            assignments = List.copyOf(assignments);
            dsdSets = List.copyOf(dsdSets);
            Objects.requireNonNull(authorizationFacts, "authorizationFacts");
            Objects.requireNonNull(directorySnapshotVersion, "directorySnapshotVersion");
            applications = Map.copyOf(applications);
            roleDisplayNames = Map.copyOf(roleDisplayNames);
            if (authVersion < 0 || policyVersion < 0) {
                throw new IllegalArgumentException("versions must not be negative");
            }
        }
    }

    public record ApplicationFact(String id, String code, String displayName) {

        public ApplicationFact {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(displayName, "displayName");
        }
    }
}
