package top.egon.cola.platform.rbac3.core.activation;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultRoleActivationResolverTest {

    @Test
    void childAssignmentActivatesTopRootAndEntireFamily() {
        RoleActivationResolution result = new DefaultRoleActivationResolver().resolve(
                input(List.of("cashier"), List.of(assignment("a1", "cashier")), List.of())
        );

        assertEquals(Set.of("root"), result.activeRoleSet().rootIds());
        assertEquals(Set.of("root", "cashier", "report"),
                result.snapshot().effectiveRoleIds());
        assertEquals(Set.of("payment:read", "payment:report"),
                result.snapshot().permissionCodes());
    }

    @Test
    void rejectsAmbiguousRootsAndSameApplicationMutexAtomically() {
        RoleHierarchy ambiguous = new RoleHierarchy(
                List.of(node("r1", "app"), node("r2", "app"), node("c", "app")),
                List.of(new RoleEdge("r1", "c"), new RoleEdge("r2", "c"))
        );
        RoleActivationInput ambiguousInput = baseInput(
                List.of("c"), List.of(assignment("a", "c")), ambiguous, List.of()
        );
        assertEquals("ROLE_ACTIVATION_ROOT_AMBIGUOUS",
                assertThrows(Rbac3RuleViolation.class,
                        () -> new DefaultRoleActivationResolver().resolve(ambiguousInput))
                        .reasonCode());

        DsdSetFact mutex = new DsdSetFact("d1", "app", 1, Set.of("root", "other"));
        assertEquals("APP_ROLE_ACTIVATION_MUTEX_VIOLATION",
                assertThrows(Rbac3RuleViolation.class,
                        () -> new DefaultRoleActivationResolver().resolve(
                                input(List.of("root", "other"),
                                        List.of(assignment("a1", "root"),
                                                assignment("a2", "other")),
                                        List.of(mutex))))
                        .reasonCode());
    }

    @Test
    void supportsMultipleApplicationsAndStableChecksums() {
        RoleHierarchy hierarchy = new RoleHierarchy(
                List.of(node("finance-root", "finance"),
                        node("report-root", "reporting")),
                List.of()
        );
        List<EligibleAssignmentFact> assignments = List.of(
                assignment("a2", "report-root"), assignment("a1", "finance-root")
        );
        RoleActivationInput first = baseInput(
                List.of("report-root", "finance-root"), assignments, hierarchy, List.of()
        );
        RoleActivationInput second = baseInput(
                List.of("finance-root", "report-root"), assignments.reversed(), hierarchy, List.of()
        );

        RoleActivationResolution left = new DefaultRoleActivationResolver().resolve(first);
        RoleActivationResolution right = new DefaultRoleActivationResolver().resolve(second);
        assertEquals(left.snapshot().checksum(), right.snapshot().checksum());
        assertEquals(2, left.activeRoleSet().rootsByApplication().size());
    }

    @Test
    void rejectsEmptyDuplicateAndOversizedRootSets() {
        DefaultRoleActivationResolver resolver = new DefaultRoleActivationResolver();
        assertEquals("ROLE_ACTIVATION_SET_INVALID",
                assertThrows(Rbac3RuleViolation.class,
                        () -> resolver.resolve(input(List.of(), List.of(), List.of())))
                        .reasonCode());
        assertEquals("ROLE_ACTIVATION_SET_INVALID",
                assertThrows(Rbac3RuleViolation.class,
                        () -> resolver.resolve(input(List.of("root", "root"),
                                List.of(assignment("a", "root")), List.of())))
                        .reasonCode());
    }

    private RoleActivationInput input(
            List<String> requested,
            List<EligibleAssignmentFact> assignments,
            List<DsdSetFact> dsdSets
    ) {
        return baseInput(requested, assignments, hierarchy(), dsdSets);
    }

    private RoleActivationInput baseInput(
            List<String> requested,
            List<EligibleAssignmentFact> assignments,
            RoleHierarchy hierarchy,
            List<DsdSetFact> dsdSets
    ) {
        AuthorizationRuleFacts facts = new AuthorizationRuleFacts(
                List.of(
                        new AuthorizationRuleFacts.PermissionBinding("cashier", "payment:read"),
                        new AuthorizationRuleFacts.PermissionBinding("report", "payment:report")
                ), List.of(), List.of(), List.of(), List.of()
        );
        return new RoleActivationInput("tenant", "user", requested,
                assignments, hierarchy, dsdSets, facts, 7, 3,
                Instant.parse("2026-07-30T00:00:00Z"));
    }

    private RoleHierarchy hierarchy() {
        return new RoleHierarchy(
                List.of(node("root", "app"), node("cashier", "app"),
                        node("report", "app"), node("other", "app")),
                List.of(new RoleEdge("root", "cashier"),
                        new RoleEdge("root", "report"))
        );
    }

    private RoleNode node(String id, String applicationId) {
        return new RoleNode(id, applicationId, "ROLE_" + id.toUpperCase(), true,
                RoleNode.RiskLevel.LOW, false, null, 1000);
    }

    private EligibleAssignmentFact assignment(String id, String roleId) {
        return new EligibleAssignmentFact(id, "user", roleId,
                EligibleAssignmentFact.Status.ACTIVE,
                Instant.parse("2026-07-29T00:00:00Z"), null);
    }
}
