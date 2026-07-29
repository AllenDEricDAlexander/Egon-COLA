package top.egon.cola.component.bytecode.core.enhance.accessguard;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import top.egon.cola.component.bytecode.bridge.BridgeFailHint;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessGuardMatcherTest {

    private static final String ACCESS_GUARD =
            "Ltop/egon/cola/component/accessguard/api/AccessGuard;";
    private static final String RATE_LIMIT =
            "Ltop/egon/cola/component/accessguard/api/RateLimitGuard;";
    private final AccessGuardMatcher matcher = new AccessGuardMatcher();
    private final ClassNode owner = owner();

    @Test
    void governanceFilterContainsOnlyTheFourV2Descriptors() {
        GovernanceAnnotationFilter filter = new GovernanceAnnotationFilter();

        assertTrue(filter.isGovernance(ACCESS_GUARD));
        assertTrue(filter.isGovernance(
                "Ltop/egon/cola/component/accessguard/api/AllowListGuard;"));
        assertTrue(filter.isGovernance(RATE_LIMIT));
        assertTrue(filter.isGovernance(
                "Ltop/egon/cola/component/accessguard/api/TimeLimitGuard;"));
        assertFalse(filter.isGovernance(
                "Ltop/egon/cola/component/accessguard/annotation/LegacyGuard;"));
        assertFalse(filter.isGovernance(
                "Ltop/egon/cola/component/accessguard/annotation/AccessGuard;"));
    }

    @Test
    void matchesV2PublicPrivateMethodsAndExplicitConstructors() {
        assertTrue(matcher.match(owner, method(Opcodes.ACC_PUBLIC, "publicValue", ACCESS_GUARD)).isPresent());
        assertTrue(matcher.match(owner,
                method(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "staticValue", RATE_LIMIT)).isPresent());
        AccessGuardPolicy constructor = matcher.match(owner,
                method(Opcodes.ACC_PUBLIC, "<init>", ACCESS_GUARD)).orElseThrow();

        assertEquals(BridgeFailHint.FAIL_CLOSED, constructor.constructorFailHint());
        assertFalse(matcher.match(owner,
                new MethodNode(Opcodes.ACC_PROTECTED, "plain", "()V", null, null)).isPresent());
    }

    @Test
    void typeAnnotationGuardsMethodsAndMethodAnnotationOverridesPresence() {
        owner.visibleAnnotations = new ArrayList<>();
        owner.visibleAnnotations.add(new AnnotationNode(ACCESS_GUARD));
        MethodNode inherited = new MethodNode(Opcodes.ACC_PUBLIC, "inherited", "()V", null, null);
        MethodNode overridden = method(Opcodes.ACC_PUBLIC, "overridden", RATE_LIMIT);

        assertTrue(matcher.match(owner, inherited).isPresent());
        assertTrue(matcher.match(owner, overridden).isPresent());
        assertFalse(matcher.match(owner,
                new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)).isPresent());
    }

    @Test
    void validatesOnlyBytecodeConstraintsWithoutReadingPlanFields() {
        MethodNode synchronizedMethod = method(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNCHRONIZED, "synchronizedValue", ACCESS_GUARD);
        synchronizedMethod.visibleAnnotations.get(0).values = new ArrayList<>(java.util.List.of(
                "timeoutBreaker", true,
                "fallbackMethod", "missingFallback"));

        assertTrue(matcher.match(owner, synchronizedMethod).isPresent());
        assertTrue(matcher.match(owner,
                method(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "staticValue", ACCESS_GUARD)).isPresent());
    }

    @Test
    void rejectsUnsupportedExplicitTargets() {
        assertUnsupported(Opcodes.ACC_PROTECTED, "protectedValue");
        assertUnsupported(0, "packageValue");
        assertUnsupported(Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, "abstractValue");
        assertUnsupported(Opcodes.ACC_PUBLIC | Opcodes.ACC_NATIVE, "nativeValue");
        assertUnsupported(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "syntheticValue");
        assertUnsupported(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE, "bridgeValue");
    }

    private void assertUnsupported(int access, String name) {
        assertThrows(IllegalArgumentException.class,
                () -> matcher.match(owner, method(access, name, ACCESS_GUARD)));
    }

    private MethodNode method(int access, String name, String descriptor) {
        MethodNode method = new MethodNode(access, name, "()V", null, null);
        method.visibleAnnotations = new ArrayList<>();
        method.visibleAnnotations.add(new AnnotationNode(descriptor));
        return method;
    }

    private ClassNode owner() {
        ClassNode owner = new ClassNode();
        owner.name = "sample/AccessGuardTarget";
        return owner;
    }
}
