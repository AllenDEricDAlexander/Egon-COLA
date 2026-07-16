package top.egon.cola.component.bytecode.core.architecture.rule;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureDependency;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRuleContext;

public final class Arch006StarterBusinessRule extends AbstractDependencyRule {

    public Arch006StarterBusinessRule() {
        super("ARCH-006", "Starter must not reference business implementations directly.",
                "Keep Starter limited to bootstrapping and depend on stable contracts.");
    }

    @Override
    protected boolean violates(ArchitectureRuleContext context, ArchitectureDependency dependency) {
        return dependency.sourceLayer() == ArchitectureLayer.STARTER
                && (dependency.targetLayer() == ArchitectureLayer.DOMAIN
                || dependency.targetLayer() == ArchitectureLayer.APPLICATION)
                && implementationType(context, dependency.targetClass());
    }
}
