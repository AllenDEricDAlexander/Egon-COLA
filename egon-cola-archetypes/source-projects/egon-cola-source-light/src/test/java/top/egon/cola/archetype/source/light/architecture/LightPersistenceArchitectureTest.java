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
 * Generated-project architecture contract for the Light MyBatis-Plus profile.
 */
class LightPersistenceArchitectureTest {

    @Test
    void generatedProjectUsesCommonPersistenceOwnership() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        List<Path> javaFiles;
        try (Stream<Path> paths = Files.walk(sourceRoot)) {
            javaFiles = paths.filter(path -> path.toString().endsWith(".java")).toList();
        }
        String source = javaFiles.stream()
                .map(path -> read(path))
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
        assertFalse(source.contains("EgonColaIService"));
        assertFalse(source.contains("EgonColaServiceImpl"));
        assertTrue(source.contains("extends EgonColaRepository<"));
        assertEquals(8, count(javaFiles, "Repository.java"));
        for (Path file : javaFiles) {
            if (file.toString().contains("service/impl") && file.toString().endsWith("DomainServiceImpl.java")) {
                assertFalse(read(file).contains("repo.dao"), "Business Service must compose repositories: " + file);
                assertFalse(read(file).contains("extends EgonCola"));
            }
        }

        assertEquals(8, count(javaFiles, "PO.java"));
        assertEquals(8, count(javaFiles, "DAO.java"));
        assertEquals(5, count(javaFiles, "DomainServiceImpl.java"));
        assertFalse(source.contains("class ShardingDataSourceBootstrapper"));
        assertFalse(source.contains("YamlShardingSphereDataSourceFactory"));
        String pom = Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8);
        assertTrue(pom.contains("egon-cola-component-common-mybatis-plus-sharding-jdbc-ext-spring-boot-starter"));
        assertFalse(pom.contains("shardingsphere-jdbc"));
        assertFalse(Files.exists(Path.of("src/main/resources/datasource/sharding.yml")));
        assertFalse(Files.exists(Path.of("src/main/resources/sharding/shardingsphere-sharding.yml")));
        assertTrue(Files.exists(Path.of("src/main/resources/egon-mybatis-plus-sharding.yml")));
    }

    @Test
    void generatedShardingConfigurationUsesTenantRouting() throws IOException {
        String sharding = read(Path.of("src/main/resources/egon-mybatis-plus-sharding.yml"));
        assertTrue(sharding.contains("mybatis-plus:"));
        assertTrue(sharding.contains("sharding:"));
        assertTrue(sharding.contains("STANDARD_TENANT_ID"));
        assertTrue(sharding.contains("tenant_id"));
        assertTrue(sharding.contains("type: SINGLE"));
        assertTrue(sharding.contains("transaction-default-type: LOCAL"));
        assertFalse(sharding.contains("UuidV7BucketShardingAlgorithm"));
        assertFalse(sharding.contains("datasource/sharding.yml"));
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
