package top.egon.cola.component.common.test;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SourceBoundaryAssertTest {

    @Test
    void acceptsSourceWithoutForbiddenImports() throws Exception {
        Path tempDir = Files.createTempDirectory("common-boundary-ok");
        Path source = tempDir.resolve("Sample.java");
        Files.writeString(source, "package sample;\nimport java.util.List;\nclass Sample {}\n");

        assertDoesNotThrow(() -> SourceBoundaryAssert.assertNoForbiddenImports(tempDir));
    }

    @Test
    void rejectsSourceWithRuntimeFrameworkImport() throws Exception {
        Path tempDir = Files.createTempDirectory("common-boundary-bad");
        Path source = tempDir.resolve("Sample.java");
        Files.writeString(source, "package sample;\nimport org.springframework.context.ApplicationContext;\nclass Sample {}\n");

        assertThrows(AssertionError.class, () -> SourceBoundaryAssert.assertNoForbiddenImports(tempDir));
    }
}
