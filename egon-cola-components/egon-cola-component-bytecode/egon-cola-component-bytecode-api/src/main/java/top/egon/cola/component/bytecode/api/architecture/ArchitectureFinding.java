package top.egon.cola.component.bytecode.api.architecture;

public record ArchitectureFinding(
        String ruleId,
        ArchitectureSeverity severity,
        String module,
        ArchitectureLayer sourceLayer,
        ArchitectureLayer targetLayer,
        String sourceClass,
        String sourceMember,
        String sourceDescriptor,
        String targetClass,
        String targetMember,
        String targetDescriptor,
        DependencyKind dependencyKind,
        LocationKind locationKind,
        Integer lineNumber,
        String message,
        String suggestion
) {
}
