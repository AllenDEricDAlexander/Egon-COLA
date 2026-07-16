package top.egon.cola.component.bytecode.core.architecture.rule;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureDependency;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRuleContext;

public final class Arch003ApplicationDirectionRule extends AbstractDependencyRule {

    public Arch003ApplicationDirectionRule() {
        super("ARCH-003", "Application must not depend on Infrastructure or Adapter.",
                "Depend on a Domain port and implement it in the outer layer.");
    }

    @Override
    protected boolean violates(ArchitectureRuleContext context, ArchitectureDependency dependency) {
        return dependency.sourceLayer() == ArchitectureLayer.APPLICATION
                && (dependency.targetLayer() == ArchitectureLayer.INFRASTRUCTURE
                || dependency.targetLayer() == ArchitectureLayer.ADAPTER);
    }
}
