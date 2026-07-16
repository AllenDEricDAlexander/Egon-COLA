package top.egon.cola.component.bytecode.core.architecture.rule;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureDependency;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRuleContext;

public final class Arch007CommonBusinessRule extends AbstractDependencyRule {

    public Arch007CommonBusinessRule() {
        super("ARCH-007", "Common must not depend on business modules.",
                "Keep reusable primitives in Common and move business-specific code to its owning layer.");
    }

    @Override
    protected boolean violates(ArchitectureRuleContext context, ArchitectureDependency dependency) {
        return dependency.sourceLayer() == ArchitectureLayer.COMMON
                && dependency.targetLayer() != ArchitectureLayer.COMMON
                && dependency.targetLayer() != ArchitectureLayer.UNKNOWN;
    }
}
