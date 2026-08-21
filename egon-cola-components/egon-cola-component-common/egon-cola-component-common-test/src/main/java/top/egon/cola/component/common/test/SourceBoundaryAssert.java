package top.egon.cola.component.common.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
        assertNoForbiddenImports(sourceRoot, Set.of());
    }

    /**
     * Asserts that source files do not use a forbidden framework import unless an explicit
     * package prefix is allowed for the caller's narrow boundary exception.
     *
     * @param sourceRoot source tree to scan
     * @param allowedImportPrefixes package prefixes such as {@code jakarta.validation.}
     */
    public static void assertNoForbiddenImports(Path sourceRoot, Set<String> allowedImportPrefixes) {
        Objects.requireNonNull(sourceRoot, "sourceRoot must not be null");
        Objects.requireNonNull(allowedImportPrefixes, "allowedImportPrefixes must not be null");
        Set<String> normalizedAllowedPrefixes = allowedImportPrefixes.stream()
                .map(SourceBoundaryAssert::normalizeImportPrefix)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (!Files.exists(sourceRoot)) {
            return;
        }
        List<String> badImports;
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            badImports = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(SourceBoundaryAssert::readLines)
                    .filter(line -> isForbiddenImport(line, normalizedAllowedPrefixes))
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

    private static boolean isForbiddenImport(String line, Set<String> allowedImportPrefixes) {
        return FORBIDDEN_IMPORT_PREFIXES.stream().anyMatch(line::startsWith)
                && allowedImportPrefixes.stream().noneMatch(line::startsWith);
    }

    private static String normalizeImportPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("allowed import prefix must not be blank");
        }
        String normalized = prefix.trim();
        return normalized.startsWith("import ") ? normalized : "import " + normalized;
    }
}
