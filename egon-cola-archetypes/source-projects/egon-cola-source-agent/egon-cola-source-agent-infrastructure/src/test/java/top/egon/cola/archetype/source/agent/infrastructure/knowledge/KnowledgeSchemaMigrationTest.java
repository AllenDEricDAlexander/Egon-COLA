package top.egon.cola.archetype.source.agent.infrastructure.knowledge;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Locks the knowledge schema contract: two migrations, the vector table stays outside Flyway. */
class KnowledgeSchemaMigrationTest {

    private static final Path MIGRATION_ROOT = Path.of("src/main/resources/db/migration");
    private static final String KNOWLEDGE_SCHEMA = "V20260910_001__create_knowledge_schema.sql";
    private static final String OUTBOX_SCHEMA = "V20260910_002__create_transactional_outbox_schema.sql";

    @Test
    void declares_the_knowledge_and_outbox_migrations() throws IOException {
        assertThat(migrationFiles()).containsExactly(KNOWLEDGE_SCHEMA, OUTBOX_SCHEMA);
    }

    @Test
    void leaves_the_vector_table_to_the_vector_store() {
        assertThat(read(KNOWLEDGE_SCHEMA))
                .contains("create extension if not exists vector")
                .doesNotContain("vector_store");
        assertThat(read(OUTBOX_SCHEMA)).doesNotContain("vector_store");
    }

    @Test
    void indexes_the_tenant_scoped_access_paths() {
        assertThat(read(KNOWLEDGE_SCHEMA))
                .contains("create unique index uk_knowledge_base_tenant_code")
                .contains("(tenant_id, lower(code))")
                .contains("create index idx_knowledge_base_tenant_created")
                .contains("create index idx_knowledge_document_tenant_base_created")
                .contains("ck_knowledge_base_status")
                .contains("ck_knowledge_base_chunk_strategy")
                .contains("ck_knowledge_document_status");
        assertThat(read(OUTBOX_SCHEMA))
                .contains("create table egon_cola_outbox_message")
                .contains("create unique index uk_outbox_message_id")
                .contains("create unique index uk_outbox_idempotency_key")
                .contains("create index idx_outbox_claim")
                .contains("create index idx_outbox_reclaim")
                .contains("create index idx_outbox_cleanup");
    }

    private static List<String> migrationFiles() throws IOException {
        if (!Files.isDirectory(MIGRATION_ROOT)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(MIGRATION_ROOT)) {
            return files.filter(path -> path.toString().endsWith(".sql"))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    private static String read(String migration) {
        try {
            return Files.readString(MIGRATION_ROOT.resolve(migration), StandardCharsets.UTF_8).toLowerCase();
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
