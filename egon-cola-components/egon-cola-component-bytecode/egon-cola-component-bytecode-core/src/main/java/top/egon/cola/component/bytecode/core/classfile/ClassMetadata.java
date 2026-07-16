package top.egon.cola.component.bytecode.core.classfile;

import java.util.List;
import java.util.Set;

public record ClassMetadata(
        String module,
        String className,
        String superName,
        Set<String> interfaces,
        Set<String> annotations,
        boolean interfaceType,
        List<ClassDependency> dependencies
) {
    public ClassMetadata {
        interfaces = interfaces == null ? Set.of() : Set.copyOf(interfaces);
        annotations = annotations == null ? Set.of() : Set.copyOf(annotations);
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
    }
}
