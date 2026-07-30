package top.egon.cola.platform.rbac3.core.constraint;

import top.egon.cola.platform.rbac3.core.hierarchy.RoleNode;

import java.util.Collection;

public final class RoleFamilyRisk {

    public RoleNode.RiskLevel aggregate(Collection<RoleNode> family) {
        return family.stream()
                .map(RoleNode::riskLevel)
                .max(java.util.Comparator.comparingInt(Enum::ordinal))
                .orElse(RoleNode.RiskLevel.LOW);
    }
}
