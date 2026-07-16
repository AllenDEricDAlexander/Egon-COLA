package top.egon.cola.component.bytecode.core.enhance.methodextension;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import sample.bytecode.MethodExtensionFixture;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodExtensionMatcherTest {

    private static final String ANNOTATION_DESCRIPTOR =
            "Ltop/egon/cola/component/methodextension/annotation/MethodExtension;";

    @Test
    void matchesAllConcreteInstanceVisibilitiesAndInheritedInterfaceAnnotation() {
        ClassNode fixture = fixtureNode();
        Map<String, MethodNode> methods = fixture.methods.stream()
                .collect(Collectors.toMap(method -> method.name, Function.identity()));
        MethodExtensionMatcher matcher = new MethodExtensionMatcher();
        ClassLoader loader = getClass().getClassLoader();

        for (String name : java.util.List.of(
                "primitive", "reference", "voidValue", "protectedValue",
                "packageValue", "privateValue", "finalSynchronized", "inherited")) {
            assertTrue(matcher.match(loader, fixture, methods.get(name)).isPresent(), name);
        }
    }

    @Test
    void excludesUnsupportedTargetsAndDoesNotInheritOntoPrivateMethods() {
        ClassNode fixture = fixtureNode();
        MethodExtensionMatcher matcher = new MethodExtensionMatcher();
        ClassLoader loader = getClass().getClassLoader();

        for (MethodNode method : fixture.methods) {
            if (java.util.Set.of("<init>", "staticValue", "nativeValue")
                    .contains(method.name)) {
                assertFalse(matcher.match(loader, fixture, method).isPresent(), method.name);
            }
        }
        for (int access : java.util.List.of(
                Opcodes.ACC_ABSTRACT,
                Opcodes.ACC_SYNTHETIC,
                Opcodes.ACC_BRIDGE)) {
            MethodNode unsupported = annotated(new MethodNode(
                    Opcodes.ASM9, Opcodes.ACC_PUBLIC | access,
                    "unsupported", "()V", null, null));
            assertFalse(matcher.match(loader, fixture, unsupported).isPresent());
        }

        ClassNode privateImplementation = fixtureNode();
        privateImplementation.methods.clear();
        MethodNode privateInherited = new MethodNode(
                Opcodes.ASM9, Opcodes.ACC_PRIVATE, "inherited",
                "(Ljava/lang/String;)Ljava/lang/String;", null, null);
        privateImplementation.methods.add(privateInherited);
        assertFalse(matcher.match(loader, privateImplementation, privateInherited).isPresent());
    }

    private ClassNode fixtureNode() {
        String resource = "/" + MethodExtensionFixture.class.getName()
                .replace('.', '/') + ".class";
        try (var stream = getClass().getResourceAsStream(resource)) {
            ClassNode classNode = new ClassNode(Opcodes.ASM9);
            new ClassReader(stream.readAllBytes()).accept(classNode, ClassReader.SKIP_CODE);
            return classNode;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private MethodNode annotated(MethodNode method) {
        method.visibleAnnotations = new ArrayList<>();
        method.visibleAnnotations.add(new AnnotationNode(ANNOTATION_DESCRIPTOR));
        return method;
    }
}
