package top.egon.cola.archetype.source.lightopen.infrastructure.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ManualSqlConventionTest {

    private static final String MASTER =
            "db/manual/postgresql/master-data/001__create_light_master_data_schema.sql";
    private static final String SHARD =
            "db/manual/postgresql/shard/002__create_light_sharded_schema.sql";
    private static final String MASTER_MIGRATION =
            "db/manual/postgresql/master-data/003__migrate_light_master_data_to_egon_model.sql";
    private static final String SHARD_MIGRATION =
            "db/manual/postgresql/shard/004__migrate_light_sharded_to_tenant_model.sql";
    private static final Pattern FORBIDDEN = Pattern.compile(
            ("fly" + "way|liqui" + "base|u" + "uid|schema\\.sql|\\bV\\d+__"),
            Pattern.CASE_INSENSITIVE);

    @Test
    void exposes_only_the_ordered_light_manual_sql_contract() throws Exception {
        List<String> resources = List.of(MASTER, SHARD, MASTER_MIGRATION, SHARD_MIGRATION);
        for (String resource : resources) {
            String sql = read(resource);
            assertThat(sql).startsWith("-- 变更内容：")
                    .contains("\n-- 影响范围：")
                    .contains("\n-- 兼容性说明：")
                    .contains("BIGINT")
                    .doesNotContainPattern(FORBIDDEN);
        }
        assertThat(read("db/manual/postgresql/README.md"))
                .contains("master-data/001__create_light_master_data_schema.sql")
                .contains("shard/002__create_light_sharded_schema.sql")
                .contains("master-data/003__migrate_light_master_data_to_egon_model.sql")
                .contains("shard/004__migrate_light_sharded_to_tenant_model.sql")
                .contains("psql")
                .containsIgnoringCase("checksum")
                .containsIgnoringCase("verification")
                .containsIgnoringCase("rollback");
    }

    private static String read(String resource) throws IOException {
        try (InputStream input = ManualSqlConventionTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
            assertThat(input).as("manual resource %s", resource).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
