package top.egon.cola.component.bytecode.core.architecture.rule;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureDependency;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRuleContext;

import java.util.EnumSet;

public final class Arch001DomainDirectionRule extends AbstractDependencyRule {

    private static final EnumSet<ArchitectureLayer> FORBIDDEN = EnumSet.of(
            ArchitectureLayer.APPLICATION,
            ArchitectureLayer.INFRASTRUCTURE,
            ArchitectureLayer.ADAPTER,
            ArchitectureLayer.FACADE,
            ArchitectureLayer.STARTER
    );

    public Arch001DomainDirectionRule() {
        super("ARCH-001", "Domain must not depend on outer COLA layers.",
                "Move the contract into Domain/Common or invert the dependency.");
    }

    @Override
    protected boolean violates(ArchitectureRuleContext context, ArchitectureDependency dependency) {
        return dependency.sourceLayer() == ArchitectureLayer.DOMAIN
                && FORBIDDEN.contains(dependency.targetLayer());
    }
}
