package architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvaluationPersistenceArchitectureTest {
    @Test
    void generated_evaluation_tree_uses_common_persistence_contract() throws IOException {
        List<Path> sourceRoots = new ArrayList<>();
        sourceRoots.add(Path.of("src/main/java"));
        try (Stream<Path> siblings = Files.list(Path.of(".."))) {
            sourceRoots.add(siblings
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().endsWith("-infrastructure"))
                    .map(path -> path.resolve("src/main/java"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "generated infrastructure source root is missing")));
        }
        List<Path> files = new ArrayList<>();
        for (Path sourceRoot : sourceRoots) {
            try (Stream<Path> paths = Files.walk(sourceRoot)) {
                files.addAll(paths.filter(path -> path.toString().endsWith(".java")).toList());
            }
        }
        String source = files.stream().map(EvaluationPersistenceArchitectureTest::read)
                .reduce("", String::concat);
        assertFalse(source.contains("jakarta.persistence"));
        assertFalse(source.contains("JpaRepository"));
        assertFalse(source.contains("repo.jpa"));
        assertFalse(source.contains("repo.impl"));
        assertFalse(source.contains("repo.mapper"));
        assertFalse(source.contains("UuidV7"));
        assertTrue(source.contains("extends EgonModel<"));
        assertTrue(source.contains("extends EgonColaMapper<"));
        assertTrue(source.contains("extends EgonColaServiceImpl<"));
        assertEquals(5, count(files, "PO.java"));
        assertEquals(5, count(files, "DAO.java"));
        assertEquals(3, count(files, "DomainServiceImpl.java"));
    }

    private static long count(List<Path> paths, String suffix) {
        return paths.stream().filter(path -> path.getFileName().toString().endsWith(suffix)).count();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
