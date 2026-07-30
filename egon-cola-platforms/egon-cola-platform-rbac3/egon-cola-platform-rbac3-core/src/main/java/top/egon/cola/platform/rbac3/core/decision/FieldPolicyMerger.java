package top.egon.cola.platform.rbac3.core.decision;

import top.egon.cola.platform.rbac3.contract.authorization.FieldAccessLevel;
import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class FieldPolicyMerger {

    public Map<String, FieldAccessLevel> merge(
            List<AuthorizationRuleFacts.FieldRuleFact> rules,
            List<AuthorizationRuleFacts.FieldDefinitionFact> definitions,
            Set<String> effectiveRoleIds
    ) {
        var caps = new TreeMap<String, FieldAccessLevel>();
        definitions.forEach(definition -> caps.put(key(
                definition.resourceCode(), definition.fieldCode()),
                definition.maximumAccess()));
        var result = new TreeMap<String, FieldAccessLevel>();
        for (AuthorizationRuleFacts.FieldRuleFact rule : rules) {
            if (!effectiveRoleIds.contains(rule.roleId())) {
                continue;
            }
            String key = key(rule.resourceCode(), rule.fieldCode());
            FieldAccessLevel requested = maximum(result.get(key), rule.accessLevel());
            FieldAccessLevel cap = caps.getOrDefault(key, FieldAccessLevel.NONE);
            result.put(key, minimum(requested, cap));
        }
        return Collections.unmodifiableMap(result);
    }

    private FieldAccessLevel maximum(FieldAccessLevel left, FieldAccessLevel right) {
        return left == null || rank(right) > rank(left) ? right : left;
    }

    private FieldAccessLevel minimum(FieldAccessLevel left, FieldAccessLevel right) {
        return rank(left) <= rank(right) ? left : right;
    }

    private int rank(FieldAccessLevel value) {
        return switch (value) {
            case NONE -> 0;
            case MASKED_READ -> 1;
            case READ -> 2;
            case WRITE -> 3;
        };
    }

    private String key(String resourceCode, String fieldCode) {
        return resourceCode + "#" + fieldCode;
    }
}
