package top.egon.cola.component.common.pojo;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PojoDependencyBoundaryTest {

    @Test
    void productionCodeDoesNotImportJacksonDatabind() throws Exception {
        Path sourceRoot = Path.of("src/main/java");
        List<String> badImports;
        try (var files = Files.walk(sourceRoot)) {
            badImports = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            return Files.readAllLines(path).stream();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .filter(line -> line.startsWith("import com.fasterxml.jackson.databind."))
                    .toList();
        }

        assertTrue(badImports.isEmpty(), "common-core production code must not import jackson-databind");
    }
}
