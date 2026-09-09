package top.egon.cola.platform.tianquan.jianshen.admin.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Rbac3ModuleBoundaryTest {

    @Test
    void moduleDependencyDirectionAndRuntimeBoundariesRemainClosed()
            throws Exception {
        Path admin = Path.of(System.getProperty("basedir"));
        Path platform = admin.getParent();
        String adminPom = Files.readString(admin.resolve("pom.xml"));
        assertTrue(adminPom.contains("egon-cola-tianquan-jianshen-starter"));

        assertNoSourceReference(
                platform.resolve("egon-cola-tianquan-jianshen-core/src/main/java"),
                List.of("org.springframework", "jakarta.persistence", "org.redisson",
                        "java.sql", "WebClient", "RestClient"));
        assertNoSourceReference(
                platform.resolve("egon-cola-tianquan-jianshen-gateway-adapter/src/main/java"),
                List.of("top.egon.cola.platform.tianquan.jianshen.admin", "EntityManager",
                        "JdbcTemplate", "WebClient", "RestClient"));
        assertNoSourceReference(admin.resolve("src/main/java"), List.of("@Async"));
    }

    @Test
    void noIndependentTestModuleExists() throws Exception {
        Path platform = Path.of(System.getProperty("basedir")).getParent();
        try (var children = Files.list(platform)) {
            assertFalse(children.map(path -> path.getFileName().toString())
                    .anyMatch(name -> name.matches(".*tianquan-jianshen.*-test(s)?")));
        }
    }

    @Test
    void iamDomainsUseTheApprovedPackageRoots() throws Exception {
        Path sourceRoot = Path.of(System.getProperty("basedir"))
                .resolve("src/main/java/top/egon/cola/platform/tianquan/jianshen/admin");
        List<String> targetRoots = List.of(
                "iam/user", "iam/role", "iam/business", "iam/application",
                "iam/organization", "iam/position", "authorization",
                "registration", "shared/tenant"
        );
        targetRoots.forEach(root -> assertTrue(
                Files.isDirectory(sourceRoot.resolve(root)),
                "missing IAM package root " + root));

        List<String> oldRoots = List.of(
                "tenant", "identity", "resource", "role", "assignment",
                "activation", "directory", "constraint", "management",
                "participation", "simulation", "runtime");
        for (String root : oldRoots) {
            Path directory = sourceRoot.resolve(root);
            if (Files.exists(directory)) {
                try (var files = Files.walk(directory)) {
                    assertTrue(files.noneMatch(path -> path.toString().endsWith(".java")),
                            "legacy IAM package still contains Java: " + root);
                }
            }
        }

        List<String> staleImports = new ArrayList<>();
        try (var files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> lines(path).stream()
                            .filter(line -> line.contains(".admin.identity.")
                                    || line.contains(".admin.directory.")
                                    || line.contains(".admin.constraint.")
                                    || line.contains(".admin.assignment.")
                                    || line.contains(".admin.activation."))
                            .map(line -> path + ": " + line.trim())
                            .forEach(staleImports::add));
        }
        assertEquals(List.of(), staleImports);
    }

    @Test
    void registrationAndRuntimeBoundariesAreSeparated() throws Exception {
        Path sourceRoot = Path.of(System.getProperty("basedir"))
                .resolve("src/main/java/top/egon/cola/platform/tianquan/jianshen/admin");
        List<String> violations = new ArrayList<>();
        try (var files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> lines(path).stream()
                            .filter(line -> line.contains(".admin.registration.")
                                    && line.contains(".admin.authorization."))
                            .map(line -> path + ": " + line.trim())
                            .forEach(violations::add));
        }
        assertEquals(List.of(), violations);
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
