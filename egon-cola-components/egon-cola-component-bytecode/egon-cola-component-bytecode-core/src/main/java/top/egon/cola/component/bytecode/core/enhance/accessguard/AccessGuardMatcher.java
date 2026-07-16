package top.egon.cola.component.bytecode.core.enhance.accessguard;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import top.egon.cola.component.bytecode.core.enhance.MethodId;
import top.egon.cola.component.bytecode.bridge.BridgeFailHint;

import java.util.List;
import java.util.Optional;

public final class AccessGuardMatcher {

    private static final String ACCESS_GUARD =
            "Ltop/egon/cola/component/accessguard/annotation/AccessGuard;";

    private final GovernanceAnnotationFilter annotations = new GovernanceAnnotationFilter();

    public Optional<AccessGuardPolicy> match(ClassNode owner, MethodNode method) {
        if (!annotated(method)) {
            return Optional.empty();
        }
        if ("<init>".equals(method.name)) {
            return matchConstructor(owner, method);
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

    private Optional<AccessGuardPolicy> matchConstructor(ClassNode owner, MethodNode method) {
        AnnotationNode annotation = findAccessGuard(method);
        if (annotation == null) {
            throw unsupported(owner, method, "only aggregate @AccessGuard is supported");
        }
        boolean publicConstructor = (method.access & Opcodes.ACC_PUBLIC) != 0;
        boolean privateConstructor = (method.access & Opcodes.ACC_PRIVATE) != 0;
        if (!publicConstructor && !privateConstructor) {
            throw unsupported(owner, method,
                    "only public and private constructors are supported");
        }
        if (booleanValue(annotation, "timeoutBreaker", false)) {
            throw unsupported(owner, method, "timeout is not supported for constructors");
        }
        if (!stringValue(annotation, "fallbackMethod", "").isBlank()) {
            throw unsupported(owner, method, "fallbackMethod is not supported for constructors");
        }
        if (!stringValue(annotation, "returnJson", "").isBlank()) {
            throw unsupported(owner, method, "returnJson is not supported for constructors");
        }
        BridgeFailHint failHint = failHint(annotation);
        return Optional.of(new AccessGuardPolicy(
                MethodId.compute(owner.name, method.name, method.desc),
                owner.name, method.name, method.desc, method.access, failHint));
    }

    private AnnotationNode findAccessGuard(MethodNode method) {
        AnnotationNode visible = find(method.visibleAnnotations, ACCESS_GUARD);
        return visible == null ? find(method.invisibleAnnotations, ACCESS_GUARD) : visible;
    }

    private AnnotationNode find(List<AnnotationNode> values, String descriptor) {
        if (values == null) {
            return null;
        }
        return values.stream().filter(value -> descriptor.equals(value.desc)).findFirst().orElse(null);
    }

    private BridgeFailHint failHint(AnnotationNode annotation) {
        Object value = value(annotation, "failStrategy");
        if (value instanceof String[] enumValue) {
            return switch (enumValue[1]) {
                case "FAIL_CLOSED" -> BridgeFailHint.FAIL_CLOSED;
                case "LOCAL_FALLBACK" -> throw new IllegalArgumentException(
                        "LOCAL_FALLBACK is not supported for Access Guard constructors");
                default -> BridgeFailHint.FAIL_OPEN;
            };
        }
        return BridgeFailHint.FAIL_OPEN;
    }

    private boolean booleanValue(AnnotationNode annotation, String name, boolean defaultValue) {
        Object value = value(annotation, name);
        return value instanceof Boolean booleanValue ? booleanValue : defaultValue;
    }

    private String stringValue(AnnotationNode annotation, String name, String defaultValue) {
        Object value = value(annotation, name);
        return value instanceof String stringValue ? stringValue : defaultValue;
    }

    private Object value(AnnotationNode annotation, String name) {
        if (annotation.values == null) {
            return null;
        }
        for (int index = 0; index < annotation.values.size(); index += 2) {
            if (name.equals(annotation.values.get(index))) {
                return annotation.values.get(index + 1);
            }
        }
        return null;
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
