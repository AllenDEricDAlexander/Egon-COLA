package top.egon.cola.component.common.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoreBoundaryTest {

    @Test
    void coreSourceDoesNotImportRuntimeFrameworksOrOtherCommonModules() throws Exception {
        Path sourceRoot = Path.of("src/main/java/top/egon/cola/component/common/core");
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            List<String> badImports = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            return Files.readAllLines(path).stream();
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .filter(line -> line.startsWith("import org.springframework.")
                            || line.startsWith("import jakarta.")
                            || line.startsWith("import javax.servlet.")
                            || line.startsWith("import org.redisson.")
                            || line.startsWith("import redis.")
                            || line.startsWith("import com.fasterxml.jackson.")
                            || line.startsWith("import lombok.")
                            || line.startsWith("import com.alibaba.cola.")
                            || line.startsWith("import top.egon.cola.component.common.model.")
                            || line.startsWith("import top.egon.cola.component.common.result.")
                            || line.startsWith("import top.egon.cola.component.common.trace."))
                    .toList();
            assertEquals(List.of(), badImports);
        }
    }
}
