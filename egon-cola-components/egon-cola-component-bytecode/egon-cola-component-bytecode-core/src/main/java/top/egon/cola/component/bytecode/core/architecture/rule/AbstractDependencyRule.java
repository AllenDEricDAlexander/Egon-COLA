package top.egon.cola.component.bytecode.core.architecture.rule;

import top.egon.cola.component.bytecode.api.architecture.ArchitectureDependency;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureFinding;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRule;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureRuleContext;
import top.egon.cola.component.bytecode.api.architecture.ArchitectureSeverity;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractDependencyRule implements ArchitectureRule {

    private final String id;
    private final String message;
    private final String suggestion;

    protected AbstractDependencyRule(String id, String message, String suggestion) {
        this.id = id;
        this.message = message;
        this.suggestion = suggestion;
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    public final ArchitectureSeverity severity() {
        return ArchitectureSeverity.ERROR;
    }

    @Override
    public final List<ArchitectureFinding> evaluate(ArchitectureRuleContext context) {
        List<ArchitectureFinding> findings = new ArrayList<>();
        for (ArchitectureDependency dependency : context.dependencies()) {
            if (violates(context, dependency)) {
                findings.add(context.finding(this, dependency, message, suggestion));
            }
        }
        return List.copyOf(findings);
    }

    protected abstract boolean violates(
            ArchitectureRuleContext context,
            ArchitectureDependency dependency
    );

    protected final boolean startsWithAny(String className, java.util.Set<String> prefixes) {
        return prefixes.stream().anyMatch(className::startsWith);
    }

    protected final boolean implementationType(ArchitectureRuleContext context, String className) {
        return context.findType(className)
                .map(type -> !type.interfaceType())
                .orElseGet(() -> className.endsWith("Impl"));
    }
}
