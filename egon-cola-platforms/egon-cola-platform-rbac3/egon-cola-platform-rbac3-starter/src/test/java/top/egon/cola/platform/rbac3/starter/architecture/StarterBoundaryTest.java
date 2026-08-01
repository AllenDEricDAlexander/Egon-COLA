package top.egon.cola.platform.rbac3.starter.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StarterBoundaryTest {

    private static final List<String> FORBIDDEN_SOURCE_REFERENCES = List.of(
            "top.egon.cola.platform.rbac3.admin",
            "top.egon.cola.platform.rbac3.gateway",
            "jakarta.persistence",
            "javax.sql",
            "java.sql",
            "WebClient",
            "RestClient");

    @Test
    void starterRemainsBusinessSideAndHasNoAdminOrDatabaseDependency()
            throws Exception {
        Path basedir = Path.of(System.getProperty("basedir"));
        Path sourceRoot = basedir.resolve("src/main/java");
        try (var files = Files.walk(sourceRoot)) {
            List<String> violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> lines(path).stream())
                    .filter(line -> FORBIDDEN_SOURCE_REFERENCES.stream()
                            .anyMatch(line::contains))
                    .toList();
            assertEquals(List.of(), violations);
        }

        String pom = Files.readString(basedir.resolve("pom.xml"));
        assertFalse(pom.contains("egon-cola-platform-rbac3-admin"));
        assertFalse(pom.contains("egon-cola-platform-rbac3-gateway-adapter"));
    }

    @Test
    void projectDoesNotIntroduceAnIndependentRbac3TestModule() throws Exception {
        Path platform = Path.of(System.getProperty("basedir")).getParent();
        try (var children = Files.list(platform)) {
            assertFalse(children
                    .map(path -> path.getFileName().toString())
                    .anyMatch(name -> name.matches(".*rbac3.*-test(s)?")));
        }
    }

    private static List<String> lines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (java.io.IOException error) {
            throw new IllegalStateException(error);
        }
    }
}
