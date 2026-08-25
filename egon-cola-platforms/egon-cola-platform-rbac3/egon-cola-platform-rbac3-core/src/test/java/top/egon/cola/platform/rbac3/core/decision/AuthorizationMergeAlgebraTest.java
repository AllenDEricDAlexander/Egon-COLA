package top.egon.cola.platform.rbac3.core.decision;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;
import top.egon.cola.platform.rbac3.core.activation.ActivationAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleHierarchy;
import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AuthorizationMergeAlgebraTest {

    @Test
    void mergesPermissionScopeAndFieldPoliciesWithHardCaps() {
        assertEquals(Set.of("a", "b"), new PermissionSetMerger().merge(List.of(
                new AuthorizationRuleFacts.ResourceGrantBinding("r1", "b"),
                new AuthorizationRuleFacts.ResourceGrantBinding("r2", "a"),
                new AuthorizationRuleFacts.ResourceGrantBinding("r2", "b")
        ), Set.of("r1", "r2")));

        Map<String, DataScopeMerger.NormalizedDataScope> scopes =
                new DataScopeMerger().merge(List.of(
                        new AuthorizationRuleFacts.DataScopeFact(
                                "r1", "p", "DEPT", "d1", 8),
                        new AuthorizationRuleFacts.DataScopeFact(
                                "r2", "p", "NONE", null, 8)
                ), Set.of("r1", "r2"));
        assertEquals(Set.of("d1"), scopes.get("p").referencesByDimension().get("DEPT"));
        assertFalse(scopes.get("p").allInTenant());

        Map<String, FieldAccessLevel> fields = new FieldPolicyMerger().merge(
                List.of(
                        new AuthorizationRuleFacts.FieldRuleFact(
                                "r1", "payment", "bank", FieldAccessLevel.WRITE),
                        new AuthorizationRuleFacts.FieldRuleFact(
                                "r2", "payment", "bank", FieldAccessLevel.READ)
                ),
                List.of(new AuthorizationRuleFacts.FieldDefinitionFact(
                        "payment", "bank", FieldAccessLevel.MASKED_READ)),
                Set.of("r1", "r2")
        );
        assertEquals(FieldAccessLevel.MASKED_READ, fields.get("payment#bank"));
    }

    @Test
    void landingRouteSelectionIsStable() {
        String selected = new LandingRouteSelector().select(
                List.of(
                        new AuthorizationRuleFacts.LandingRouteFact("r1", "z-route", 10, "p"),
                        new AuthorizationRuleFacts.LandingRouteFact("r2", "a-route", 10, "p")
                ), Set.of("r1", "r2"), Set.of("p")
        ).orElseThrow();
        assertEquals("a-route", selected);
    }

    @Test
    void derivesResourceAndApiUnionWithoutGrantingAnUnmappedResource() {
        RoleNode role = new RoleNode("r1", "app", "ADMIN", true,
                RoleNode.RiskLevel.LOW, false, null, 0);
        AuthorizationRuleFacts facts = new AuthorizationRuleFacts(
                List.of(
                        new AuthorizationRuleFacts.ResourceGrantBinding(
                                "r1", "route-1", "users", "ROUTE", "users:read"),
                        new AuthorizationRuleFacts.ResourceGrantBinding(
                                "r1", "api-1", "users.list", "API", "users:list"),
                        new AuthorizationRuleFacts.ResourceGrantBinding(
                                "r1", "action-1", "users.disable", "ACTION", null)),
                List.of(), List.of(), List.of(),
                List.of(
                        new AuthorizationRuleFacts.ResourceFact(
                                "menu-1", "directory", "MENU", null, "internal:menu"),
                        new AuthorizationRuleFacts.ResourceFact(
                                "route-1", "users", "ROUTE", "directory", "users:read"),
                        new AuthorizationRuleFacts.ResourceFact(
                                "api-1", "users.list", "API", null, "users:list")),
                List.of());

        ActivationAuthorizationSnapshot snapshot = new UserAuthorizationSnapshotBuilder().build(
                Set.of("r1"), new RoleHierarchy(List.of(role), List.of()), facts,
                1L, 2L, 3L);

        assertEquals(Set.of("users:read", "users:list"), snapshot.permissionCodes());
        assertEquals(Set.of("directory", "users", "users.list"), snapshot.resourceCodes());
        assertFalse(snapshot.permissionCodes().contains("internal:menu"));
    }
}
