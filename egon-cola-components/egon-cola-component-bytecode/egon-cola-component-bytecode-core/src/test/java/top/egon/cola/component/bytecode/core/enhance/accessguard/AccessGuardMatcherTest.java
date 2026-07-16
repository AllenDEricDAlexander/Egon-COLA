package top.egon.cola.component.bytecode.core.enhance.accessguard;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessGuardMatcherTest {

    private static final String ACCESS_GUARD =
            "Ltop/egon/cola/component/accessguard/annotation/AccessGuard;";
    private final AccessGuardMatcher matcher = new AccessGuardMatcher();
    private final ClassNode owner = owner();

    @Test
    void matchesApprovedPublicPrivateMethodsAndConstructors() {
        assertTrue(matcher.match(owner, method(Opcodes.ACC_PUBLIC, "publicValue")).isPresent());
        assertTrue(matcher.match(owner,
                method(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "staticValue")).isPresent());
        assertTrue(matcher.match(owner,
                method(Opcodes.ACC_PUBLIC, "<init>")).isPresent());
        assertFalse(matcher.match(owner,
                new MethodNode(Opcodes.ACC_PROTECTED, "plain", "()V", null, null)).isPresent());
    }

    @Test
    void rejectsExplicitUnsupportedTargets() {
        assertUnsupported(Opcodes.ACC_PROTECTED, "protectedValue");
        assertUnsupported(0, "packageValue");
        assertUnsupported(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "abstractValue");
        assertUnsupported(Opcodes.ACC_PUBLIC | Opcodes.ACC_NATIVE, "nativeValue");
        assertUnsupported(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "syntheticValue");
        assertUnsupported(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE, "bridgeValue");
    }

    private void assertUnsupported(int access, String name) {
        assertThrows(IllegalArgumentException.class,
                () -> matcher.match(owner, method(access, name)));
    }

    private MethodNode method(int access, String name) {
        MethodNode method = new MethodNode(access, name, "()V", null, null);
        method.visibleAnnotations = new ArrayList<>();
        method.visibleAnnotations.add(new AnnotationNode(ACCESS_GUARD));
        return method;
    }

    private ClassNode owner() {
        ClassNode owner = new ClassNode();
        owner.name = "sample/AccessGuardTarget";
        return owner;
    }
}
