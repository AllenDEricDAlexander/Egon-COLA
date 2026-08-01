package top.egon.cola.platform.rbac3.core.decision;

import org.junit.jupiter.api.Test;
import top.egon.cola.platform.rbac3.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorizationAlgebraPropertyTest {

    @Test
    void permissionUnionIsCommutativeAssociativeAndIdempotent() {
        PermissionSetMerger merger = new PermissionSetMerger();
        Set<String> roles = Set.of("r1", "r2", "r3");
        for (int seed = 0; seed < 64; seed++) {
            List<AuthorizationRuleFacts.PermissionBinding> values = bindings(seed);
            List<AuthorizationRuleFacts.PermissionBinding> reversed =
                    new ArrayList<>(values);
            Collections.reverse(reversed);

            Set<String> expected = merger.merge(values, roles);
            assertEquals(expected, merger.merge(reversed, roles));

            List<AuthorizationRuleFacts.PermissionBinding> duplicated =
                    new ArrayList<>(values);
            duplicated.addAll(values);
            assertEquals(expected, merger.merge(duplicated, roles));

            int first = values.size() / 3;
            int second = first * 2;
            Set<String> partitionUnion = new LinkedHashSet<>();
            partitionUnion.addAll(merger.merge(values.subList(0, first), roles));
            partitionUnion.addAll(merger.merge(values.subList(first, second), roles));
            partitionUnion.addAll(merger.merge(values.subList(second, values.size()), roles));
            assertEquals(expected, partitionUnion);
        }
    }

    @Test
    void fieldMergeIsOrderIndependentAndSensitiveFieldsDefaultToNone() {
        FieldPolicyMerger merger = new FieldPolicyMerger();
        List<AuthorizationRuleFacts.FieldRuleFact> rules = List.of(
                new AuthorizationRuleFacts.FieldRuleFact(
                        "r1", "customer", "mobile", FieldAccessLevel.WRITE),
                new AuthorizationRuleFacts.FieldRuleFact(
                        "r2", "customer", "mobile", FieldAccessLevel.READ),
                new AuthorizationRuleFacts.FieldRuleFact(
                        "r1", "customer", "secret", FieldAccessLevel.WRITE));
        List<AuthorizationRuleFacts.FieldDefinitionFact> definitions = List.of(
                new AuthorizationRuleFacts.FieldDefinitionFact(
                        "customer", "mobile", FieldAccessLevel.MASKED_READ));

        var forward = merger.merge(rules, definitions, Set.of("r1", "r2"));
        var reverse = merger.merge(rules.reversed(), definitions, Set.of("r1", "r2"));

        assertEquals(forward, reverse);
        assertEquals(FieldAccessLevel.MASKED_READ, forward.get("customer#mobile"));
        assertEquals(FieldAccessLevel.NONE, forward.get("customer#secret"));
    }

    private static List<AuthorizationRuleFacts.PermissionBinding> bindings(int seed) {
        Random random = new Random(seed);
        List<AuthorizationRuleFacts.PermissionBinding> values = new ArrayList<>();
        for (int index = 0; index < 96; index++) {
            values.add(new AuthorizationRuleFacts.PermissionBinding(
                    "r" + (1 + random.nextInt(4)),
                    "permission:" + random.nextInt(24)));
        }
        return values;
    }
}
