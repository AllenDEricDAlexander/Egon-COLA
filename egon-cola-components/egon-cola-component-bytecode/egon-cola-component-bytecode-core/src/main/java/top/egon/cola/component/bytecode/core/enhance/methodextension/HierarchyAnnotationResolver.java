package top.egon.cola.component.bytecode.core.enhance.methodextension;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

public final class HierarchyAnnotationResolver {

    private static final String ANNOTATION_DESCRIPTOR =
            "Ltop/egon/cola/component/methodextension/annotation/MethodExtension;";
    private static final Object MONITOR = new Object();
    private static final Map<ClassLoader, Map<String, Optional<ClassMetadata>>> CACHE =
            new WeakHashMap<>();
    private static final Map<String, Optional<ClassMetadata>> BOOTSTRAP_CACHE =
            new HashMap<>();

    public boolean hasInheritedAnnotation(
            ClassLoader loader,
            ClassNode owner,
            MethodNode method
    ) {
        if ((method.access & Opcodes.ACC_PRIVATE) != 0) {
            return false;
        }
        Set<String> visited = new HashSet<>();
        for (String interfaceName : owner.interfaces) {
            if (hasAnnotation(loader, interfaceName, method.name, method.desc, visited)) {
                return true;
            }
        }
        return hasAnnotation(loader, owner.superName, method.name, method.desc, visited);
    }

    private boolean hasAnnotation(
            ClassLoader loader,
            String className,
            String methodName,
            String methodDescriptor,
            Set<String> visited
    ) {
        if (className == null || !visited.add(className)) {
            return false;
        }
        ClassMetadata metadata = metadata(loader, className).orElse(null);
        if (metadata == null) {
            return false;
        }
        boolean declared = metadata.methods().stream().anyMatch(method ->
                method.name().equals(methodName)
                        && method.descriptor().equals(methodDescriptor)
                        && (method.access() & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) == 0
                        && method.annotated());
        if (declared) {
            return true;
        }
        for (String interfaceName : metadata.interfaces()) {
            if (hasAnnotation(loader, interfaceName, methodName, methodDescriptor, visited)) {
                return true;
            }
        }
        return hasAnnotation(
                loader, metadata.superName(), methodName, methodDescriptor, visited);
    }

    private Optional<ClassMetadata> metadata(ClassLoader loader, String className) {
        synchronized (MONITOR) {
            Map<String, Optional<ClassMetadata>> loaderCache = loader == null
                    ? BOOTSTRAP_CACHE : CACHE.computeIfAbsent(loader, ignored -> new HashMap<>());
            return loaderCache.computeIfAbsent(className, ignored -> read(loader, className));
        }
    }

    private Optional<ClassMetadata> read(ClassLoader loader, String className) {
        String resourceName = className + ".class";
        try (InputStream stream = loader == null
                ? ClassLoader.getSystemResourceAsStream(resourceName)
                : loader.getResourceAsStream(resourceName)) {
            if (stream == null) {
                return Optional.empty();
            }
            ClassNode classNode = new ClassNode(Opcodes.ASM9);
            new ClassReader(stream).accept(
                    classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            List<MethodAnnotationMetadata> methods = new ArrayList<>();
            for (MethodNode method : classNode.methods) {
                methods.add(new MethodAnnotationMetadata(
                        method.name,
                        method.desc,
                        method.access,
                        annotated(method.visibleAnnotations) || annotated(method.invisibleAnnotations)
                ));
            }
            return Optional.of(new ClassMetadata(
                    classNode.superName, classNode.interfaces, methods));
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private boolean annotated(List<AnnotationNode> annotations) {
        return annotations != null && annotations.stream()
                .anyMatch(annotation -> ANNOTATION_DESCRIPTOR.equals(annotation.desc));
    }

    private record ClassMetadata(
            String superName,
            List<String> interfaces,
            List<MethodAnnotationMetadata> methods
    ) {
        private ClassMetadata {
            interfaces = List.copyOf(interfaces);
            methods = List.copyOf(methods);
        }
    }

    private record MethodAnnotationMetadata(
            String name,
            String descriptor,
            int access,
            boolean annotated
    ) {
    }
}
