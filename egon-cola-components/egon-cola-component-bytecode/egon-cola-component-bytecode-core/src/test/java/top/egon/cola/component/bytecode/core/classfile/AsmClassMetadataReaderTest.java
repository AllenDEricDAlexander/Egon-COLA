package top.egon.cola.component.bytecode.core.classfile;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.bytecode.api.architecture.DependencyKind;
import top.egon.cola.component.bytecode.core.classfile.fixture.DependencyFixture;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsmClassMetadataReaderTest {

    private final ClassMetadataReader reader = new AsmClassMetadataReader();

    @Test
    void readsAllSupportedDependencyLocationsWithoutLoadingTargets() throws IOException {
        ClassMetadata metadata = reader.read("fixture", classBytes(DependencyFixture.class));

        Set<DependencyKind> kinds = metadata.dependencies().stream()
                .map(ClassDependency::kind)
                .collect(Collectors.toSet());

        assertTrue(kinds.containsAll(Set.of(
                DependencyKind.EXTENDS,
                DependencyKind.IMPLEMENTS,
                DependencyKind.FIELD,
                DependencyKind.PARAMETER,
                DependencyKind.RETURN,
                DependencyKind.THROWS,
                DependencyKind.SIGNATURE,
                DependencyKind.ANNOTATION,
                DependencyKind.NEW,
                DependencyKind.CAST,
                DependencyKind.METHOD_CALL,
                DependencyKind.INVOKEDYNAMIC,
                DependencyKind.LAMBDA_TARGET,
                DependencyKind.METHOD_HANDLE,
                DependencyKind.CONSTANT_POOL
        )));
        assertEquals(DependencyFixture.class.getName(), metadata.className());
        assertFalse(metadata.dependencies().isEmpty());
    }

    @Test
    void parsingClassBytesDoesNotRunStaticInitializer() throws IOException {
        String className = AsmClassMetadataReaderTest.class.getName() + "$ExplosiveInitializer";
        byte[] bytes = classBytes(className);

        ClassMetadata metadata = reader.read("fixture", bytes);

        assertEquals(className, metadata.className());
    }

    @Test
    void skipsReservedConstantPoolSlotsAfterLongAndDoubleConstants() throws IOException {
        ClassMetadata metadata = reader.read("fixture", classBytes(WideConstantFixture.class));

        assertEquals(WideConstantFixture.class.getName(), metadata.className());
    }

    private byte[] classBytes(Class<?> type) throws IOException {
        return classBytes(type.getName());
    }

    private byte[] classBytes(String className) throws IOException {
        String resource = "/" + className.replace('.', '/') + ".class";
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing class resource " + resource);
            }
            return input.readAllBytes();
        }
    }

    static class ExplosiveInitializer {
        static {
            if (true) {
                throw new IllegalStateException("must not initialize");
            }
        }
    }

    static final class WideConstantFixture {
        static final long LONG_VALUE = 9_223_372_036_854_775_000L;
        static final double DOUBLE_VALUE = 3.141592653589793D;
    }
}
