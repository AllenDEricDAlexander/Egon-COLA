package top.egon.cola.component.bytecode.api.architecture;

public record ArchitectureDependency(
        String module,
        String sourceClass,
        String sourceMember,
        String sourceDescriptor,
        ArchitectureLayer sourceLayer,
        String targetClass,
        String targetMember,
        String targetDescriptor,
        ArchitectureLayer targetLayer,
        DependencyKind dependencyKind,
        LocationKind locationKind,
        Integer lineNumber
) {
}
