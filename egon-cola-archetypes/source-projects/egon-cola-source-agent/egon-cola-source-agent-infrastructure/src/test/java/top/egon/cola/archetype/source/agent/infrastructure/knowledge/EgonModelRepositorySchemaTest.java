package top.egon.cola.archetype.source.agent.infrastructure.knowledge;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class EgonModelRepositorySchemaTest {
    @Test
    void correctionIsEmptyOnlyAndRetainsFlywayAndExternalTableOwnership() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V20260913_001__egon_model_repository.sql"));
        assertThat(sql.indexOf("REBUILD_REQUIRED")).isLessThan(sql.indexOf("ALTER TABLE"));
        assertThat(sql).contains("ADD COLUMN deleted_at timestamp(6) without time zone", "version bigint NOT NULL DEFAULT 0", "DROP IDENTITY IF EXISTS", "DROP COLUMN is_deleted RESTRICT", "WHERE deleted_at IS NULL")
                .doesNotContain("UPDATE knowledge_", "1970-01-01", "egon_cola_outbox_message", "DROP TABLE");
        try (var files = Files.list(Path.of("src/main/resources/db/migration"))) {
            assertThat(files.filter(path -> path.getFileName().toString().endsWith(".sql")).count()).isEqualTo(3);
        }
    }
}
