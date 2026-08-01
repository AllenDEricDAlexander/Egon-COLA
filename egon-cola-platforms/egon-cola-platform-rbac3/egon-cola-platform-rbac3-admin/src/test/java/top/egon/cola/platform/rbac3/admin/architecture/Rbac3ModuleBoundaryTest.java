package top.egon.cola.platform.rbac3.admin.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class Rbac3ModuleBoundaryTest {

    @Test
    void moduleDependencyDirectionAndRuntimeBoundariesRemainClosed()
            throws Exception {
        Path admin = Path.of(System.getProperty("basedir"));
        Path platform = admin.getParent();
        String adminPom = Files.readString(admin.resolve("pom.xml"));
        assertFalse(adminPom.contains("egon-cola-platform-rbac3-starter"));

        assertNoSourceReference(
                platform.resolve("egon-cola-platform-rbac3-core/src/main/java"),
                List.of("org.springframework", "jakarta.persistence", "org.redisson",
                        "java.sql", "WebClient", "RestClient"));
        assertNoSourceReference(
                platform.resolve("egon-cola-platform-rbac3-gateway-adapter/src/main/java"),
                List.of("top.egon.cola.platform.rbac3.admin", "EntityManager",
                        "JdbcTemplate", "WebClient", "RestClient"));
        assertNoSourceReference(
                admin.resolve("src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http"),
                List.of("Repository"));
        assertNoSourceReference(admin.resolve("src/main/java"), List.of("@Async"));
    }

    @Test
    void noIndependentTestModuleExists() throws Exception {
        Path platform = Path.of(System.getProperty("basedir")).getParent();
        try (var children = Files.list(platform)) {
            assertFalse(children.map(path -> path.getFileName().toString())
                    .anyMatch(name -> name.matches(".*rbac3.*-test(s)?")));
        }
    }

    private static void assertNoSourceReference(
            Path root,
            List<String> forbidden) throws Exception {
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> lines(path).stream()
                            .filter(line -> forbidden.stream().anyMatch(line::contains))
                            .map(line -> path + ": " + line.trim())
                            .forEach(violations::add));
        }
        assertEquals(List.of(), violations);
    }

    private static List<String> lines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (java.io.IOException error) {
            throw new IllegalStateException(error);
        }
    }
}
