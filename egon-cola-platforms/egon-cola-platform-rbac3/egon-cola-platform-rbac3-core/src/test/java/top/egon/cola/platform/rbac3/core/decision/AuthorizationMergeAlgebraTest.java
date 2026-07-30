package top.egon.cola.platform.rbac3.core.decision;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AuthorizationMergeAlgebraTest {

    @Test
    void mergesPermissionScopeAndFieldPoliciesWithHardCaps() {
        assertEquals(Set.of("a", "b"), new PermissionSetMerger().merge(List.of(
                new AuthorizationRuleFacts.PermissionBinding("r1", "b"),
                new AuthorizationRuleFacts.PermissionBinding("r2", "a"),
                new AuthorizationRuleFacts.PermissionBinding("r2", "b")
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
}
