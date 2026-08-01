package top.egon.cola.platform.rbac3.core.decision;

import top.egon.cola.platform.rbac3.core.activation.AuthorizationRuleFacts;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class PermissionSetMerger {

    public Set<String> merge(
            List<AuthorizationRuleFacts.PermissionBinding> bindings,
            Set<String> effectiveRoleIds
    ) {
        var result = new TreeSet<String>();
        for (AuthorizationRuleFacts.PermissionBinding binding : bindings) {
            if (effectiveRoleIds.contains(binding.roleId())) {
                result.add(binding.permissionCode());
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
