package top.egon.cola.component.bytecode.core.enhance.observation;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import top.egon.cola.component.bytecode.api.observation.EgonObserved;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservationMatcherTest {

    @Test
    void supportsEveryConcreteVisibilityAndStaticFinalSynchronizedMethod() {
        ObservationMatcher matcher = matcher(false);
        for (int access : List.of(
                Opcodes.ACC_PUBLIC,
                Opcodes.ACC_PROTECTED,
                0,
                Opcodes.ACC_PRIVATE,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNCHRONIZED)) {
            assertTrue(matcher.match("sample/application/Service",
                    method(access, "work", "()V")).isPresent(), "access=" + access);
        }
    }

    @Test
    void appliesExclusionAndHardBoundariesBeforeAnnotationAndIncludes() {
        ObservationMatcher matcher = new ObservationMatcher(
                List.of("sample.application.*"), List.of("work*"),
                List.of("sample.application.Secret#*"), false, -1L);
        MethodNode annotated = observed(method(Opcodes.ACC_PUBLIC, "work", "()V"),
                List.of("channel=test"), 5L);

        assertFalse(matcher.match("sample/application/Secret", annotated).isPresent());
        assertFalse(matcher.match("top/egon/cola/component/bytecode/Hidden", annotated)
                .isPresent());
        assertTrue(matcher.match("other/Owner", annotated).isPresent());
        assertFalse(matcher.match("sample/application/Service",
                method(Opcodes.ACC_ABSTRACT, "work", "()V")).isPresent());
        assertFalse(matcher.match("sample/application/Service",
                method(Opcodes.ACC_NATIVE, "work", "()V")).isPresent());
        assertFalse(matcher.match("sample/application/Service",
                method(Opcodes.ACC_BRIDGE, "work", "()V")).isPresent());
        assertFalse(matcher.match("sample/application/Service",
                method(Opcodes.ACC_SYNTHETIC, "work", "()V")).isPresent());
        assertFalse(matcher.match("sample/application/Service",
                method(Opcodes.ACC_PUBLIC, "lambda$work$0", "()V")).isPresent());
        assertFalse(matcher.match("sample/application/Service",
                method(Opcodes.ACC_STATIC, "<clinit>", "()V")).isPresent());
    }

    @Test
    void constructorsRequireAnnotationOrExplicitConfiguration() {
        MethodNode constructor = method(Opcodes.ACC_PUBLIC, "<init>", "()V");
        assertFalse(matcher(false).match("sample/application/Service", constructor).isPresent());
        assertTrue(matcher(true).match("sample/application/Service", constructor).isPresent());
        assertTrue(matcher(false).match("other/Owner",
                observed(constructor, List.of(), -1L)).isPresent());
    }

    @Test
    void validatesAndBoundsStaticTagsAndBuildsStableMethodIds() {
        ObservationMatcher matcher = matcher(false);
        ObservationPolicy policy = matcher.match("other/Owner", observed(
                method(Opcodes.ACC_PUBLIC, "work", "(I)V"),
                List.of("channel=test", "mode=safe"), 5L)).orElseThrow();

        assertEquals(Map.of("channel", "test", "mode", "safe"), policy.staticTags());
        assertEquals(5_000_000L, policy.slowThresholdNanos());
        assertEquals(policy.methodId(), matcher.match("other/Owner", observed(
                method(Opcodes.ACC_PUBLIC, "work", "(I)V"),
                List.of("channel=test"), 5L)).orElseThrow().methodId());
        assertNotEquals(policy.methodId(), matcher.match("other/Owner", observed(
                method(Opcodes.ACC_PUBLIC, "work", "(J)V"),
                List.of(), 5L)).orElseThrow().methodId());

        assertThrows(IllegalArgumentException.class, () -> matcher.match("other/Owner",
                observed(method(Opcodes.ACC_PUBLIC, "work", "()V"),
                        List.of("dynamic=${secret}"), 5L)));
        assertThrows(IllegalArgumentException.class, () -> matcher.match("other/Owner",
                observed(method(Opcodes.ACC_PUBLIC, "work", "()V"),
                        List.of("missing-separator"), 5L)));
    }

    private ObservationMatcher matcher(boolean observeConstructors) {
        return new ObservationMatcher(
                List.of("sample.application.*"), List.of("work*"), List.of(),
                observeConstructors, -1L);
    }

    private MethodNode method(int access, String name, String descriptor) {
        return new MethodNode(Opcodes.ASM9, access, name, descriptor, null, null);
    }

    private MethodNode observed(MethodNode method, List<String> tags, long thresholdMillis) {
        AnnotationNode annotation = new AnnotationNode(Type.getDescriptor(EgonObserved.class));
        annotation.values = new ArrayList<>(List.of(
                "tags", new ArrayList<>(tags),
                "slowThresholdMillis", thresholdMillis
        ));
        method.visibleAnnotations = new ArrayList<>(List.of(annotation));
        return method;
    }
}
