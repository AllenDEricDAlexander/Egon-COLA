package architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the generated Web persistence ownership contract before the JPA migration is complete.
 */
class WebArchitectureTest {

    @Test
    void uses_common_persistence_contract_in_infrastructure() throws IOException {
        Path infrastructureRoot;
        try (Stream<Path> siblings = Files.list(Path.of(".."))) {
            infrastructureRoot = siblings
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().endsWith("-infrastructure"))
                    .map(path -> path.resolve("src/main/java"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "generated infrastructure source root is missing"));
        }
        List<Path> javaFiles;
        try (Stream<Path> paths = Files.walk(infrastructureRoot)) {
            javaFiles = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
        String source = javaFiles.stream()
                .map(WebArchitectureTest::read)
                .reduce("", String::concat);
        assertTrue(source.contains("extends EgonModel<"));
        assertTrue(source.contains("extends EgonColaMapper<"));
        assertTrue(source.contains("extends EgonColaServiceImpl<"));
        assertFalse(source.contains("JpaRepository"));
        assertFalse(source.contains("repo.mapper"));
        assertEquals(8, javaFiles.stream()
                .filter(path -> path.getFileName().toString().endsWith("PO.java"))
                .count());
        assertEquals(8, javaFiles.stream()
                .filter(path -> path.getFileName().toString().endsWith("DAO.java"))
                .count());
        assertEquals(4, javaFiles.stream()
                .filter(path -> path.getFileName().toString().endsWith("DomainServiceImpl.java"))
                .count());
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("cannot read generated source " + path, failure);
        }
    }
}
