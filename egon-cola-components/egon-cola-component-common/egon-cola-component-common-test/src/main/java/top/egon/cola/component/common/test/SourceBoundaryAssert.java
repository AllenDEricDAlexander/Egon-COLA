package top.egon.cola.component.common.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Source-level boundary assertions for common modules.
 */
public final class SourceBoundaryAssert {

    private static final List<String> FORBIDDEN_IMPORT_PREFIXES = List.of(
            "import org.springframework.",
            "import jakarta.",
            "import javax.servlet.",
            "import org.redisson.",
            "import redis.",
            "import com.alibaba.cola."
    );

    private SourceBoundaryAssert() {
    }

    public static void assertNoForbiddenImports(Path sourceRoot) {
        if (!Files.exists(sourceRoot)) {
            return;
        }
        List<String> badImports;
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            badImports = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(SourceBoundaryAssert::readLines)
                    .filter(SourceBoundaryAssert::isForbiddenImport)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan source root " + sourceRoot, e);
        }
        if (!badImports.isEmpty()) {
            throw new AssertionError("Forbidden imports found: " + badImports);
        }
    }

    private static Stream<String> readLines(Path path) {
        try {
            return Files.readAllLines(path).stream();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read source file " + path, e);
        }
    }

    private static boolean isForbiddenImport(String line) {
        return FORBIDDEN_IMPORT_PREFIXES.stream().anyMatch(line::startsWith);
    }
}
