package top.egon.cola.archetype.source.web.infrastructure.config.datasource;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class RepositoryPersistenceContractTest {
    @Test
    void allMappersUseExplicitActiveSqlAndVersionedCommands() throws Exception {
        try (var paths = Files.walk(Path.of("src/main/resources/mybatis/mapper"))) {
            var files = paths.filter(p -> p.toString().endsWith("DAO.xml")).toList();
            assertThat(files).hasSize(8);
            for (var path : files) {
                assertThat(Files.readString(path)).doesNotContain("is_deleted", "${")
                        .contains("deleted_at IS NULL", "selectActiveById", "selectActiveByIds", "deleteVersionedById", "MP_OPTLOCK_VERSION_ORIGINAL");
            }
        }
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            var repositories = paths.filter(p -> p.toString().endsWith("Repository.java")).toList();
            assertThat(repositories).hasSize(8);
            for (var repository : repositories) { assertThat(Files.readString(repository)).contains("extends EgonColaRepository<", "@Validated", "@RequiredArgsConstructor"); }
        }
    }
}
