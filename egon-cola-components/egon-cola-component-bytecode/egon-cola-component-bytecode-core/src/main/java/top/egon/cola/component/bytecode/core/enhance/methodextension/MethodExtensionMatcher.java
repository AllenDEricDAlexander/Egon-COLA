package top.egon.cola.component.bytecode.core.enhance.methodextension;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import top.egon.cola.component.bytecode.core.enhance.MethodId;

import java.util.List;
import java.util.Optional;

public final class MethodExtensionMatcher {

    private static final String ANNOTATION_DESCRIPTOR =
            "Ltop/egon/cola/component/methodextension/annotation/MethodExtension;";
    private static final int UNSUPPORTED_ACCESS = Opcodes.ACC_STATIC
            | Opcodes.ACC_ABSTRACT
            | Opcodes.ACC_NATIVE
            | Opcodes.ACC_SYNTHETIC
            | Opcodes.ACC_BRIDGE;

    private final HierarchyAnnotationResolver hierarchyResolver;

    public MethodExtensionMatcher() {
        this(new HierarchyAnnotationResolver());
    }

    MethodExtensionMatcher(HierarchyAnnotationResolver hierarchyResolver) {
        this.hierarchyResolver = hierarchyResolver;
    }

    public Optional<MethodExtensionPolicy> match(
            ClassLoader loader,
            ClassNode owner,
            MethodNode method
    ) {
        if ((method.access & UNSUPPORTED_ACCESS) != 0
                || method.name.startsWith("<")) {
            return Optional.empty();
        }
        if (!annotated(method.visibleAnnotations)
                && !annotated(method.invisibleAnnotations)
                && !hierarchyResolver.hasInheritedAnnotation(loader, owner, method)) {
            return Optional.empty();
        }
        return Optional.of(new MethodExtensionPolicy(
                MethodId.compute(owner.name, method.name, method.desc),
                owner.name,
                method.name,
                method.desc,
                method.access
        ));
    }

    private boolean annotated(List<AnnotationNode> annotations) {
        return annotations != null && annotations.stream()
                .anyMatch(annotation -> ANNOTATION_DESCRIPTOR.equals(annotation.desc));
    }
}
