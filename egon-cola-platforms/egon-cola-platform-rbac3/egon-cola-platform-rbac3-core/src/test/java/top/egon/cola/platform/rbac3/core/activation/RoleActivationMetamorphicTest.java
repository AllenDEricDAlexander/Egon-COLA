package top.egon.cola.platform.rbac3.core.activation;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleEdge;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchyValidator;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleActivationMetamorphicTest {

    private static final Instant NOW = Instant.parse("2026-07-30T08:00:00Z");

    @Test
    void seededDagPermutationsKeepUniqueRootFullFamilyAndChecksum() {
        for (int seed = 1; seed <= 32; seed++) {
            Fixture fixture = fixture(seed, 40);
            RoleActivationResolution baseline = resolve(fixture, seed, false);
            RoleActivationResolution permuted = resolve(fixture, seed, true);

            assertEquals(Set.of("role-0"), baseline.activeRoleSet().rootIds());
            assertEquals(40, baseline.snapshot().effectiveRoleIds().size());
            assertEquals(baseline.snapshot().effectiveRoleIds(),
                    permuted.snapshot().effectiveRoleIds());
            assertEquals(baseline.snapshot().permissionCodes(),
                    permuted.snapshot().permissionCodes());
            assertEquals(baseline.snapshot().checksum(), permuted.snapshot().checksum());
        }
    }

    @Test
    void cycleAndDepthRemainRejectedForGeneratedGraphs() {
        List<RoleNode> nodes = IntStream.rangeClosed(0, 11)
                .mapToObj(index -> node("role-" + index))
                .toList();
        List<RoleEdge> tooDeep = IntStream.range(0, 11)
                .mapToObj(index -> new RoleEdge(
                        "role-" + index, "role-" + (index + 1)))
                .toList();

        assertEquals("ROLE_HIERARCHY_DEPTH_LIMIT_EXCEEDED",
                assertThrows(Rbac3RuleViolation.class,
                        () -> new RoleHierarchyValidator().validate(
                                new RoleHierarchy(nodes, tooDeep))).reasonCode());

        assertEquals("ROLE_HIERARCHY_CYCLE",
                assertThrows(Rbac3RuleViolation.class,
                        () -> new RoleHierarchyValidator().validate(
                                new RoleHierarchy(
                                        nodes.subList(0, 3),
                                        List.of(new RoleEdge("role-0", "role-1"),
                                                new RoleEdge("role-1", "role-2"),
                                                new RoleEdge("role-2", "role-0")))))
                        .reasonCode());
    }

    private RoleActivationResolution resolve(
            Fixture fixture,
            int seed,
            boolean shuffle
    ) {
        List<RoleNode> nodes = new ArrayList<>(fixture.nodes());
        List<RoleEdge> edges = new ArrayList<>(fixture.edges());
        List<AuthorizationRuleFacts.PermissionBinding> permissions =
                new ArrayList<>(fixture.permissions());
        if (shuffle) {
            Collections.shuffle(nodes, new Random(seed * 31L));
            Collections.shuffle(edges, new Random(seed * 37L));
            Collections.shuffle(permissions, new Random(seed * 41L));
        }
        RoleActivationInput input = new RoleActivationInput(
                "tenant", "user", "session", List.of("role-39"),
                List.of(new EligibleAssignmentFact(
                        "assignment", "user", "role-39",
                        EligibleAssignmentFact.Status.ACTIVE,
                        NOW.minusSeconds(60), null)),
                new RoleHierarchy(nodes, edges), List.of(),
                new AuthorizationRuleFacts(
                        permissions, List.of(), List.of(), List.of(), List.of()),
                3L, 5L, 7L, NOW);
        return new DefaultRoleActivationResolver().resolve(input);
    }

    private Fixture fixture(int seed, int size) {
        Random random = new Random(seed);
        List<RoleNode> nodes = IntStream.range(0, size)
                .mapToObj(index -> node("role-" + index))
                .toList();
        List<RoleEdge> edges = new ArrayList<>();
        for (int index = 1; index < size; index++) {
            int parent = (index - 1) / 4;
            edges.add(new RoleEdge("role-" + parent, "role-" + index));
            if (parent != 0 && index > 3 && random.nextBoolean()) {
                edges.add(new RoleEdge("role-0", "role-" + index));
            }
        }
        List<AuthorizationRuleFacts.PermissionBinding> permissions =
                IntStream.range(0, size)
                        .mapToObj(index -> new AuthorizationRuleFacts.PermissionBinding(
                                "role-" + index, "permission:" + index))
                        .toList();
        return new Fixture(nodes, List.copyOf(edges), permissions);
    }

    private static RoleNode node(String id) {
        return new RoleNode(id, "application", id.toUpperCase(), true,
                RoleNode.RiskLevel.LOW, false, null, 1_000);
    }

    private record Fixture(
            List<RoleNode> nodes,
            List<RoleEdge> edges,
            List<AuthorizationRuleFacts.PermissionBinding> permissions) {
    }
}
