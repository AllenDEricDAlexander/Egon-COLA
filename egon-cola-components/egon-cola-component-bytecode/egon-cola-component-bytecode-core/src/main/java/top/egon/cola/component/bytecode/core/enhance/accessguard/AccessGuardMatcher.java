package top.egon.cola.component.bytecode.core.enhance.accessguard;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import top.egon.cola.component.bytecode.core.enhance.MethodId;

import java.util.List;
import java.util.Optional;

public final class AccessGuardMatcher {

    private final GovernanceAnnotationFilter annotations = new GovernanceAnnotationFilter();

    public Optional<AccessGuardPolicy> match(ClassNode owner, MethodNode method) {
        if (!annotated(method)) {
            return Optional.empty();
        }
        if ("<init>".equals(method.name)) {
            return Optional.empty();
        }
        if ("<clinit>".equals(method.name)) {
            throw unsupported(owner, method, "class initializers are not supported");
        }
        int access = method.access;
        if ((access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE
                | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE)) != 0) {
            throw unsupported(owner, method,
                    "abstract, native, synthetic, and bridge methods are not supported");
        }
        boolean publicMethod = (access & Opcodes.ACC_PUBLIC) != 0;
        boolean privateMethod = (access & Opcodes.ACC_PRIVATE) != 0;
        if (!publicMethod && !privateMethod) {
            throw unsupported(owner, method,
                    "only public and private methods are supported");
        }
        return Optional.of(new AccessGuardPolicy(
                MethodId.compute(owner.name, method.name, method.desc),
                owner.name, method.name, method.desc, method.access));
    }

    private boolean annotated(MethodNode method) {
        return contains(method.visibleAnnotations) || contains(method.invisibleAnnotations);
    }

    private boolean contains(List<AnnotationNode> values) {
        return values != null && values.stream().anyMatch(value -> annotations.isGovernance(value.desc));
    }

    private IllegalArgumentException unsupported(
            ClassNode owner,
            MethodNode method,
            String reason
    ) {
        return new IllegalArgumentException(
                "Unsupported Access Guard target " + owner.name.replace('/', '.')
                        + "#" + method.name + method.desc + ": " + reason);
    }
}
