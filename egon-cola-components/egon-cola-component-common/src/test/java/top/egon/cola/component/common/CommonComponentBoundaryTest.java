package top.egon.cola.component.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonComponentBoundaryTest {

    @Test
    void commonSourceDoesNotImportRuntimeFrameworks() throws Exception {
        Path sourceRoot = Path.of("src/main/java/top/egon/cola/component/common");
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
                            || line.startsWith("import org.redisson.")
                            || line.startsWith("import redis.")
                            || line.startsWith("import top.egon.cola.component.dtp.")
                            || line.startsWith("import com.alibaba.cola."))
                    .toList();
            assertEquals(List.of(), badImports);
        }
    }

    @Test
    void oldColaStyleApiHasBeenRemoved() throws Exception {
        Path sourceRoot = Path.of("src/main/java/top/egon/cola/component/common");
        List<String> oldClassNames;
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            oldClassNames = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> List.of(
                            "Response.java",
                            "SingleResponse.java",
                            "MultiResponse.java",
                            "DTO.java",
                            "Command.java",
                            "Query.java",
                            "ClientObject.java",
                            "Assert.java",
                            "BizException.java",
                            "SysException.java",
                            "BaseException.java"
                    ).contains(name))
                    .toList();
        }
        assertTrue(oldClassNames.isEmpty());
    }
}
