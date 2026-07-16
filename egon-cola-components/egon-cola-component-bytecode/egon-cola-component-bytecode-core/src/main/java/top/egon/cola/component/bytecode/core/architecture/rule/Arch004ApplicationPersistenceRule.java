package top.egon.cola.component.bytecode.core.architecture.rule;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureDependency;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureLayer;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRuleContext;

public final class Arch004ApplicationPersistenceRule extends AbstractDependencyRule {

    private final ArchitectureRuleConfiguration configuration;

    public Arch004ApplicationPersistenceRule(ArchitectureRuleConfiguration configuration) {
        super("ARCH-004", "Application must not depend directly on persistence APIs or implementations.",
                "Move persistence access behind a Domain repository interface.");
        this.configuration = configuration;
    }

    @Override
    protected boolean violates(ArchitectureRuleContext context, ArchitectureDependency dependency) {
        return dependency.sourceLayer() == ArchitectureLayer.APPLICATION
                && (startsWithAny(dependency.targetClass(), configuration.persistencePrefixes())
                || persistenceImplementation(dependency.targetClass()));
    }

    private boolean persistenceImplementation(String className) {
        return className.endsWith("MapperImpl") || className.endsWith("RepositoryImpl");
    }
}
