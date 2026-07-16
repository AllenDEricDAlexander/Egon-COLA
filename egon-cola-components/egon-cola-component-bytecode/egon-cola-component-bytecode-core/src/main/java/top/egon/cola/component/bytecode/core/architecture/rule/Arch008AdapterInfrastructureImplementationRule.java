package top.egon.cola.component.bytecode.core.architecture.rule;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureDependency;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRuleContext;

public final class Arch008AdapterInfrastructureImplementationRule extends AbstractDependencyRule {

    public Arch008AdapterInfrastructureImplementationRule() {
        super("ARCH-008", "Adapter must not invoke Infrastructure implementations directly.",
                "Route the dependency through an Application service or Domain port.");
    }

    @Override
    protected boolean violates(ArchitectureRuleContext context, ArchitectureDependency dependency) {
        return dependency.sourceLayer() == ArchitectureLayer.ADAPTER
                && dependency.targetLayer() == ArchitectureLayer.INFRASTRUCTURE
                && implementationType(context, dependency.targetClass());
    }
}
