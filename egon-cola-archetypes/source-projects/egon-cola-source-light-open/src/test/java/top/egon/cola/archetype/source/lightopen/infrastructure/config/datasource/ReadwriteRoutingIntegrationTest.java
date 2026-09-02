package top.egon.cola.archetype.source.lightopen.infrastructure.config.datasource;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ReadwriteRoutingIntegrationTest {
    @Test
    void readwrite_configuration_routes_sharded_tables_by_tenant() throws IOException {
        String yaml = read("sharding/shardingsphere-sharding-readwrite.yml");
        assertThat(yaml).contains("light_school_classes:")
                .contains("light_class_course_schedules:")
                .contains("shardingColumn: tenant_id")
                .contains("tenant_long_database_bucket")
                .contains("tenant_long_table_bucket");
    }

    @Test
    void manual_open_schema_is_not_auto_executed() throws IOException {
        assertThat(read("db/manual/postgresql/master-data/003__migrate_light_master_data_to_egon_model.sql"))
                .contains("SELECT 1 / CASE WHEN EXISTS");
    }

    private static String read(String resource) throws IOException {
        try (InputStream input = ReadwriteRoutingIntegrationTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
            assertThat(input).as("resource %s", resource).isNotNull();
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
