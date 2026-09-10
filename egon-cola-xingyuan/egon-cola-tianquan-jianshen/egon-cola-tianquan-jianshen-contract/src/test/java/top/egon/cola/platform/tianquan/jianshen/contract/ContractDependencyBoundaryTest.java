package top.egon.cola.platform.tianquan.jianshen.contract;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractDependencyBoundaryTest {

    private static final List<String> FORBIDDEN_BYTECODE_REFERENCES = List.of(
            "org/springframework/",
            "jakarta/persistence/",
            "org/redisson/",
            "reactor/",
            "top/egon/cola/platform/tianquan/jianshen/core/",
            "top/egon/cola/platform/tianquan/jianshen/starter/",
            "top/egon/cola/platform/tianquan/jianshen/admin/",
            "top/egon/cola/platform/tianquan/jianshen/yuheng/"
    );

    @Test
    void contractSourceDoesNotImportRuntimeFrameworksOrProductModules()
            throws Exception {
        Path sourceRoot = Path.of(
                "src/main/java/top/egon/cola/platform/tianquan/jianshen/contract"
        );
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            List<String> forbiddenImports = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(this::lines)
                    .filter(this::isForbidden)
                    .toList();

            assertEquals(List.of(), forbiddenImports);
        }
    }

    @Test
    void compiledContractBytecodeDoesNotReferenceRuntimeFrameworksOrModules()
            throws Exception {
        Path classRoot = Path.of(
                "target/classes/top/egon/cola/platform/tianquan/jianshen/contract"
        );
        try (Stream<Path> files = Files.walk(classRoot)) {
            List<String> forbiddenReferences = files
                    .filter(path -> path.toString().endsWith(".class"))
                    .flatMap(path -> forbiddenReferences(path).stream())
                    .toList();

            assertEquals(List.of(), forbiddenReferences);
        }
    }

    private Stream<String> lines(Path path) {
        try {
            return Files.readAllLines(path).stream();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private boolean isForbidden(String line) {
        return line.startsWith("import org.springframework.")
                || line.startsWith("import jakarta.persistence.")
                || line.startsWith("import org.redisson.")
                || line.startsWith("import reactor.")
                || line.startsWith(
                        "import top.egon.cola.platform.tianquan.jianshen.core."
                )
                || line.startsWith(
                        "import top.egon.cola.platform.tianquan.jianshen.starter."
                )
                || line.startsWith(
                        "import top.egon.cola.platform.tianquan.jianshen.admin."
                )
                || line.startsWith(
                        "import top.egon.cola.platform.tianquan.jianshen.yuheng."
                );
    }

    private List<String> forbiddenReferences(Path path) {
        try {
            String bytecode = new String(
                    Files.readAllBytes(path),
                    StandardCharsets.ISO_8859_1
            );
            return FORBIDDEN_BYTECODE_REFERENCES.stream()
                    .filter(bytecode::contains)
                    .map(reference -> path + ": " + reference)
                    .toList();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
