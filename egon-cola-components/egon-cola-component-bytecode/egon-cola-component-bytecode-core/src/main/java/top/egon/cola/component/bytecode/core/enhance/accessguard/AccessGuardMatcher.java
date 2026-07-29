package top.egon.cola.component.bytecode.core.enhance.accessguard;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import top.egon.cola.component.bytecode.bridge.BridgeFailHint;
import top.egon.cola.component.bytecode.core.enhance.MethodId;

import java.util.List;
import java.util.Optional;

public final class AccessGuardMatcher {

    private static final String ACCESS_GUARD =
            "Ltop/egon/cola/component/accessguard/api/AccessGuard;";

    private final GovernanceAnnotationFilter annotations = new GovernanceAnnotationFilter();

    public Optional<AccessGuardPolicy> match(ClassNode owner, MethodNode method) {
        boolean methodAnnotated = annotated(method);
        if ("<init>".equals(method.name)) {
            return methodAnnotated ? matchConstructor(owner, method) : Optional.empty();
        }
        if ("<clinit>".equals(method.name)) {
            if (methodAnnotated) {
                throw unsupported(owner, method, "class initializers are not supported");
            }
            return Optional.empty();
        }
        if (!methodAnnotated && !typeAnnotated(owner)) {
            return Optional.empty();
        }
        validateMethod(owner, method);
        return Optional.of(policy(owner, method, BridgeFailHint.GLOBAL_DEFAULT));
    }

    private Optional<AccessGuardPolicy> matchConstructor(ClassNode owner, MethodNode method) {
        if (findAnnotation(method.visibleAnnotations, ACCESS_GUARD) == null
                && findAnnotation(method.invisibleAnnotations, ACCESS_GUARD) == null) {
            throw unsupported(owner, method, "only @AccessGuard is supported for constructors");
        }
        boolean publicConstructor = (method.access & Opcodes.ACC_PUBLIC) != 0;
        boolean privateConstructor = (method.access & Opcodes.ACC_PRIVATE) != 0;
        if (!publicConstructor && !privateConstructor) {
            throw unsupported(owner, method,
                    "only public and private constructors are supported");
        }
        return Optional.of(policy(owner, method, BridgeFailHint.FAIL_CLOSED));
    }

    private static void validateMethod(ClassNode owner, MethodNode method) {
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
    }

    private static AccessGuardPolicy policy(
            ClassNode owner,
            MethodNode method,
            BridgeFailHint failHint
    ) {
        return new AccessGuardPolicy(
                MethodId.compute(owner.name, method.name, method.desc),
                owner.name,
                method.name,
                method.desc,
                method.access,
                failHint);
    }

    private boolean annotated(MethodNode method) {
        return contains(method.visibleAnnotations) || contains(method.invisibleAnnotations);
    }

    private static boolean typeAnnotated(ClassNode owner) {
        return findAnnotation(owner.visibleAnnotations, ACCESS_GUARD) != null
                || findAnnotation(owner.invisibleAnnotations, ACCESS_GUARD) != null;
    }

    private boolean contains(List<AnnotationNode> values) {
        return values != null && values.stream().anyMatch(value -> annotations.isGovernance(value.desc));
    }

    private static AnnotationNode findAnnotation(List<AnnotationNode> values, String descriptor) {
        if (values == null) {
            return null;
        }
        return values.stream().filter(value -> descriptor.equals(value.desc)).findFirst().orElse(null);
    }

    private static IllegalArgumentException unsupported(
            ClassNode owner,
            MethodNode method,
            String reason
    ) {
        return new IllegalArgumentException(
                "Unsupported Access Guard target " + owner.name.replace('/', '.')
                        + "#" + method.name + method.desc + ": " + reason);
    }
}
