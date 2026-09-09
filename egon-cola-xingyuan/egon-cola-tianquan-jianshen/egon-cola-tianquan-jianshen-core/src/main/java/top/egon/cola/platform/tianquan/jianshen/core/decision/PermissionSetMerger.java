package top.egon.cola.platform.tianquan.jianshen.core.decision;

import top.egon.cola.platform.tianquan.jianshen.core.activation.AuthorizationRuleFacts;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class PermissionSetMerger {

    public Set<String> merge(
            List<AuthorizationRuleFacts.ResourceGrantBinding> bindings,
            Set<String> effectiveRoleIds
    ) {
        var result = new TreeSet<String>();
        for (AuthorizationRuleFacts.ResourceGrantBinding binding : bindings) {
            if (effectiveRoleIds.contains(binding.roleId())) {
                if (binding.permissionCode() != null) {
                    result.add(binding.permissionCode());
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
