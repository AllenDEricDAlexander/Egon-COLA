package top.egon.cola.component.gateway.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayCoreBoundaryTest {

    @Test
    void coreSourceDoesNotImportRuntimeFrameworksOrProductModules()
            throws Exception {
        Path sourceRoot = Path.of(
                "src/main/java/top/egon/cola/component/gateway/core"
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

    private Stream<String> lines(Path path) {
        try {
            return Files.readAllLines(path).stream();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private boolean isForbidden(String line) {
        return line.startsWith("import org.springframework.")
                || line.startsWith("import jakarta.")
                || line.startsWith("import io.netty.")
                || line.startsWith("import io.grpc.")
                || line.startsWith("import reactor.")
                || line.startsWith("import org.redisson.")
                || line.startsWith("import org.apache.kafka.")
                || line.startsWith("import com.fasterxml.jackson.")
                || line.startsWith("import lombok.")
                || line.startsWith(
                        "import top.egon.cola.component.gateway.engine."
                )
                || line.startsWith(
                        "import top.egon.cola.component.gateway.admin."
                )
                || line.startsWith(
                        "import top.egon.cola.component.gateway.starter."
                )
                || line.startsWith(
                        "import top.egon.cola.component.ddc.http.registration."
                );
    }
}
