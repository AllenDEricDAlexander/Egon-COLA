package top.egon.cola.component.bytecode.core.architecture;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureDependency;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRule;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRuleContext;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureType;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public final class DefaultArchitectureRuleContext implements ArchitectureRuleContext {

    private final ArchitectureGraph graph;

    public DefaultArchitectureRuleContext(ArchitectureGraph graph) {
        this.graph = Objects.requireNonNull(graph, "graph");
    }

    @Override
    public Collection<ArchitectureType> types() {
        return graph.types();
    }

    @Override
    public Collection<ArchitectureDependency> dependencies() {
        return graph.dependencies();
    }

    @Override
    public Optional<ArchitectureType> findType(String className) {
        return graph.findType(className);
    }

    @Override
    public ArchitectureFinding finding(
            ArchitectureRule rule,
            ArchitectureDependency dependency,
            String message,
            String suggestion
    ) {
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(dependency, "dependency");
        return new ArchitectureFinding(
                rule.id(),
                rule.severity(),
                dependency.module(),
                dependency.sourceLayer(),
                dependency.targetLayer(),
                dependency.sourceClass(),
                dependency.sourceMember(),
                dependency.sourceDescriptor(),
                dependency.targetClass(),
                dependency.targetMember(),
                dependency.targetDescriptor(),
                dependency.dependencyKind(),
                dependency.locationKind(),
                dependency.lineNumber(),
                message,
                suggestion
        );
    }
}
