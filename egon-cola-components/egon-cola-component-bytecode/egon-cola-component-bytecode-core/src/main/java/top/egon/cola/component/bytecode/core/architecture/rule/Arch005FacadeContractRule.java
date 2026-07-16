package top.egon.cola.component.bytecode.core.architecture.rule;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureDependency;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRuleContext;

import java.util.EnumSet;

public final class Arch005FacadeContractRule extends AbstractDependencyRule {

    private static final EnumSet<ArchitectureLayer> FORBIDDEN = EnumSet.of(
            ArchitectureLayer.DOMAIN,
            ArchitectureLayer.APPLICATION,
            ArchitectureLayer.INFRASTRUCTURE,
            ArchitectureLayer.ADAPTER,
            ArchitectureLayer.STARTER
    );

    public Arch005FacadeContractRule() {
        super("ARCH-005", "Facade must remain a self-contained contract layer.",
                "Expose contract DTOs from Facade/Common and move implementation dependencies out.");
    }

    @Override
    protected boolean violates(ArchitectureRuleContext context, ArchitectureDependency dependency) {
        return dependency.sourceLayer() == ArchitectureLayer.FACADE
                && FORBIDDEN.contains(dependency.targetLayer());
    }
}
