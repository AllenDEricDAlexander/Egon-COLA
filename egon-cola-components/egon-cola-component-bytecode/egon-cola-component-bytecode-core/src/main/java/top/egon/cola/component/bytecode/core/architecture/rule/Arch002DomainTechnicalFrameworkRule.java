package top.egon.cola.component.bytecode.core.architecture.rule;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureDependency;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRuleContext;

public final class Arch002DomainTechnicalFrameworkRule extends AbstractDependencyRule {

    private final ArchitectureRuleConfiguration configuration;

    public Arch002DomainTechnicalFrameworkRule(ArchitectureRuleConfiguration configuration) {
        super("ARCH-002", "Domain must not depend on technical frameworks.",
                "Keep framework integration outside Domain and expose a domain-facing port.");
        this.configuration = configuration;
    }

    @Override
    protected boolean violates(ArchitectureRuleContext context, ArchitectureDependency dependency) {
        return dependency.sourceLayer() == ArchitectureLayer.DOMAIN
                && startsWithAny(dependency.targetClass(), configuration.technicalFrameworkPrefixes())
                && !startsWithAny(
                        dependency.targetClass(), configuration.technicalFrameworkAllowPrefixes());
    }
}
