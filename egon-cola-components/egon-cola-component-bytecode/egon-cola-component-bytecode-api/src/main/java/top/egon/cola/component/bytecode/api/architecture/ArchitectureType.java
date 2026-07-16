package top.egon.cola.component.bytecode.api.architecture;

import java.util.Set;

public record ArchitectureType(
        String module,
        String className,
        ArchitectureLayer layer,
        Set<String> annotations,
        boolean interfaceType
) {
    public ArchitectureType {
        annotations = annotations == null ? Set.of() : Set.copyOf(annotations);
    }
}
