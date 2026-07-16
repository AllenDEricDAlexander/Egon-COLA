package top.egon.cola.component.bytecode.core.classfile;

import top.egon.cola.component.bytecode.api.architecture.DependencyKind;
import top.egon.cola.component.bytecode.api.architecture.LocationKind;

public record ClassDependency(
        String sourceClass,
        String sourceMember,
        String sourceDescriptor,
        String targetClass,
        String targetMember,
        String targetDescriptor,
        DependencyKind kind,
        LocationKind locationKind,
        Integer lineNumber
) {
}
