package top.egon.cola.archetype.source.serviceopen.infrastructure.migration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManualSqlConventionTest {

    private static final String MASTER =
            "db/manual/postgresql/master-data/001__create_evaluation_master_data_schema.sql";
    private static final String SHARD =
            "db/manual/postgresql/shard/002__create_evaluation_sharded_schema.sql";
    private static final String MASTER_MIGRATION =
            "db/manual/postgresql/master-data/003__migrate_evaluation_master_data_to_egon_model.sql";
    private static final String SHARD_MIGRATION =
            "db/manual/postgresql/shard/004__migrate_evaluation_sharded_to_tenant_model.sql";

    @Test
    void shouldProvideOperatorOwnedBigintScriptsWithoutMigrationHistory() {
        String master = ManualSchemaTestSupport.read(MASTER);
        String shard = ManualSchemaTestSupport.read(SHARD);
        String masterMigration = ManualSchemaTestSupport.read(MASTER_MIGRATION);
        String shardMigration = ManualSchemaTestSupport.read(SHARD_MIGRATION);

        assertThat(master).contains("BIGINT").doesNotContainIgnoringCase("flyway");
        assertThat(shard).contains("BIGINT").doesNotContainIgnoringCase("flyway");
        assertThat(masterMigration).contains("BIGINT", "tenant_id")
                .doesNotContainIgnoringCase("flyway");
        assertThat(shardMigration).contains("BIGINT", "tenant_id")
                .doesNotContainIgnoringCase("flyway");
        assertThat(master).doesNotContainIgnoringCase("uuid", "varchar(36)");
        assertThat(shard).doesNotContainIgnoringCase("uuid", "varchar(36)");
    }

    @Test
    void shouldDocumentOperatorOwnedSchemaWithoutRuntimeMigration() {
        String readme = ManualSchemaTestSupport.read("db/manual/postgresql/README.md");

        assertThat(readme).contains("application never executes");
        assertThat(readme).contains("Never point Spring SQL initialization");
    }
}
