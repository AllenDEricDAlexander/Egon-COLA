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

/** Generated-project architecture contract for the Light Open Common MP profile. */
class OpenArchitectureTest {

    @Test
    void generatedProjectUsesCommonPersistenceOwnership() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        List<Path> javaFiles;
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            javaFiles = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
        String source = javaFiles.stream().map(OpenArchitectureTest::read)
                .reduce("", String::concat);

        assertFalse(source.contains("jakarta.persistence"));
        assertFalse(source.contains("JpaRepository"));
        assertFalse(source.contains("EntityManager"));
        assertFalse(source.contains("UuidV7"));
        assertFalse(source.contains("repo.jpa"));
        assertFalse(source.contains("repo.impl"));
        assertFalse(source.contains("repo.mapper"));
        assertTrue(source.contains("extends EgonModel<"));
        assertTrue(source.contains("extends EgonColaMapper<"));
        assertTrue(source.contains("extends EgonColaServiceImpl<"));

        assertEquals(8, count(javaFiles, "PO.java"));
        assertEquals(8, count(javaFiles, "DAO.java"));
        assertEquals(5, count(javaFiles, "DomainServiceImpl.java"));
    }

    @Test
    void generatedShardingConfigurationUsesPositiveTenantRouting() throws IOException {
        String sharding = read(Path.of("src/main/resources/sharding/shardingsphere-sharding.yml"));
        String readwrite = read(Path.of(
                "src/main/resources/sharding/shardingsphere-sharding-readwrite.yml"));
        assertTrue(sharding.contains("shardingColumn: tenant_id"));
        assertTrue(readwrite.contains("shardingColumn: tenant_id"));
        assertTrue(sharding.contains("light_users:"));
        assertTrue(sharding.contains("light_school_classes:"));
        assertFalse(sharding.contains("SnowflakeLongShardingAlgorithm"));
        assertFalse(readwrite.contains("SnowflakeLongShardingAlgorithm"));
    }

    private static long count(List<Path> paths, String suffix) {
        return paths.stream().filter(path -> path.getFileName().toString().endsWith(suffix)).count();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read generated contract path " + path, exception);
        }
    }
}
