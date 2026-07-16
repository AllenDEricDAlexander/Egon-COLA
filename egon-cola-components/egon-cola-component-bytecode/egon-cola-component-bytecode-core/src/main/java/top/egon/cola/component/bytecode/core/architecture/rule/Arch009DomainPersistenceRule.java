package top.egon.cola.component.bytecode.core.architecture.rule;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureDependency;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRuleContext;

public final class Arch009DomainPersistenceRule extends AbstractDependencyRule {

    private final ArchitectureRuleConfiguration configuration;

    public Arch009DomainPersistenceRule(ArchitectureRuleConfiguration configuration) {
        super("ARCH-009", "Domain must not depend on persistence details.",
                "Retain only repository interfaces in Domain and move persistence details to Infrastructure.");
        this.configuration = configuration;
    }

    @Override
    protected boolean violates(ArchitectureRuleContext context, ArchitectureDependency dependency) {
        if (dependency.sourceLayer() != ArchitectureLayer.DOMAIN) {
            return false;
        }
        if (context.findType(dependency.targetClass())
                .map(type -> type.interfaceType())
                .orElse(false)) {
            return false;
        }
        return startsWithAny(dependency.targetClass(), configuration.persistencePrefixes())
                || dependency.targetClass().endsWith("MapperImpl")
                || dependency.targetClass().endsWith("RepositoryImpl");
    }
}
