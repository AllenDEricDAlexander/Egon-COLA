package top.egon.cola.component.bytecode.core.hierarchy;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassHierarchyResolver {

    private static final String OBJECT = "java/lang/Object";

    private final ClassLoader loader;
    private final Map<String, ClassInformation> information = new ConcurrentHashMap<>();

    public ClassHierarchyResolver(ClassLoader loader) {
        this.loader = loader;
    }

    public String commonSuperClass(String firstType, String secondType) {
        if (firstType.equals(secondType)) {
            return firstType;
        }
        if (firstType.startsWith("[") || secondType.startsWith("[")) {
            return OBJECT;
        }
        if (isAssignableFrom(firstType, secondType, ConcurrentHashMap.newKeySet())) {
            return firstType;
        }
        if (isAssignableFrom(secondType, firstType, ConcurrentHashMap.newKeySet())) {
            return secondType;
        }
        ClassInformation current = information(firstType);
        if (current.interfaceType()) {
            return OBJECT;
        }
        while (current.superName() != null) {
            String candidate = current.superName();
            if (isAssignableFrom(candidate, secondType, ConcurrentHashMap.newKeySet())) {
                return candidate;
            }
            current = information(candidate);
        }
        return OBJECT;
    }

    private boolean isAssignableFrom(String target, String source, Set<String> visited) {
        if (target.equals(source) || OBJECT.equals(target)) {
            return true;
        }
        if (!visited.add(source)) {
            return false;
        }
        ClassInformation sourceInformation = information(source);
        if (sourceInformation.interfaces().stream()
                .anyMatch(type -> isAssignableFrom(target, type, visited))) {
            return true;
        }
        return sourceInformation.superName() != null
                && isAssignableFrom(target, sourceInformation.superName(), visited);
    }

    private ClassInformation information(String internalName) {
        return information.computeIfAbsent(internalName, this::readInformation);
    }

    private ClassInformation readInformation(String internalName) {
        try (InputStream stream = resource(internalName + ".class")) {
            if (stream == null) {
                return ClassInformation.unknown(internalName);
            }
            InformationVisitor visitor = new InformationVisitor();
            new ClassReader(stream).accept(visitor,
                    ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return visitor.information;
        } catch (Exception exception) {
            return ClassInformation.unknown(internalName);
        }
    }

    private InputStream resource(String name) {
        InputStream stream = loader == null ? null : loader.getResourceAsStream(name);
        return stream == null ? ClassLoader.getSystemResourceAsStream(name) : stream;
    }

    private record ClassInformation(
            String name,
            String superName,
            Set<String> interfaces,
            boolean interfaceType
    ) {
        static ClassInformation unknown(String name) {
            return new ClassInformation(name, OBJECT, Set.of(), false);
        }
    }

    private static final class InformationVisitor extends ClassVisitor {

        private ClassInformation information;

        private InformationVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(
                int version,
                int access,
                String name,
                String signature,
                String superName,
                String[] interfaces
        ) {
            information = new ClassInformation(
                    name,
                    superName,
                    Set.copyOf(Arrays.asList(interfaces)),
                    (access & Opcodes.ACC_INTERFACE) != 0
            );
        }
    }
}
