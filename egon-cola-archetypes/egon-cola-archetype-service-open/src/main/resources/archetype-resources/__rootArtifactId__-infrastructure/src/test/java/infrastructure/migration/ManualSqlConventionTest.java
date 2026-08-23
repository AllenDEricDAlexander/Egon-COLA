#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.infrastructure.migration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManualSqlConventionTest {

    private static final String MASTER =
            "db/manual/postgresql/master-data/001__create_evaluation_master_data_schema.sql";
    private static final String SHARD =
            "db/manual/postgresql/shard/002__create_evaluation_sharded_schema.sql";

    @Test
    void shouldProvideOperatorOwnedBigintScriptsWithoutMigrationHistory() {
        String master = ManualSchemaTestSupport.read(MASTER);
        String shard = ManualSchemaTestSupport.read(SHARD);

        assertThat(master).contains("BIGINT").doesNotContainIgnoringCase("flyway");
        assertThat(shard).contains("BIGINT").doesNotContainIgnoringCase("flyway");
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
