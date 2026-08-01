package top.egon.cola.platform.rbac3.admin.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AdminLayerBoundaryTest {

    @Test
    void controllersDoNotInjectRepositoriesAndAdminDoesNotDependOnStarter()
            throws Exception {
        Path basedir = Path.of(System.getProperty("basedir"));
        Path http = basedir.resolve(
                "src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http");
        if (Files.exists(http)) {
            try (var files = Files.walk(http)) {
                assertFalse(files.filter(path -> path.toString().endsWith(".java"))
                        .map(this::read)
                        .anyMatch(source -> source.contains("Repository")));
            }
        }
        String pom = Files.readString(basedir.resolve("pom.xml"));
        assertFalse(pom.contains("egon-cola-platform-rbac3-starter"));
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (java.io.IOException error) {
            throw new IllegalStateException(error);
        }
    }
}
