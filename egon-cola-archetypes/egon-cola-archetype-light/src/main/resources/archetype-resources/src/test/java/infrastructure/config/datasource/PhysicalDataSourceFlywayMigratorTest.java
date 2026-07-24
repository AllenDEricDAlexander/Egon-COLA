package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;

class PhysicalDataSourceFlywayMigratorTest {

    @Test
    void shouldMigrateTargetsSeriallyInNameOrder() {
        List<String> migrated = new ArrayList<>();
        PhysicalDataSourceFlywayMigrator migrator =
                new PhysicalDataSourceFlywayMigrator((dataSource, target, properties) ->
                        migrated.add(target.dataSourceName()));
        Map<String, DataSource> dataSources = new LinkedHashMap<>();
        dataSources.put("shard_1", mock(DataSource.class));
        dataSources.put("single", mock(DataSource.class));
        dataSources.put("shard_0", mock(DataSource.class));

        migrator.migrate(
                dataSources,
                List.of(target("single"), target("shard_1"), target("shard_0")),
                new FlywayProperties());

        assertThat(migrated).containsExactly("shard_0", "shard_1", "single");
    }

    @Test
    void shouldStopAtFailingTargetAndIncludeSafeContext() {
        List<String> migrated = new ArrayList<>();
        PhysicalDataSourceFlywayMigrator migrator =
                new PhysicalDataSourceFlywayMigrator((dataSource, target, properties) -> {
                    migrated.add(target.dataSourceName());
                    if (target.dataSourceName().equals("shard_1")) {
                        throw new IllegalStateException("migration failure");
                    }
                });
        Map<String, DataSource> dataSources = Map.of(
                "shard_0", mock(DataSource.class),
                "shard_1", mock(DataSource.class),
                "single", mock(DataSource.class));

        assertThatThrownBy(() -> migrator.migrate(
                        dataSources,
                        List.of(target("single"), target("shard_1"), target("shard_0")),
                        new FlywayProperties()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("shard_1")
                .hasMessageContaining("classpath:db/shard_1")
                .hasMessageNotContaining("password");
        assertThat(migrated).containsExactly("shard_0", "shard_1");
    }

    private static ShardingDataSourceProperties.FlywayTargetProperties target(String name) {
        return new ShardingDataSourceProperties.FlywayTargetProperties(
                name,
                List.of("classpath:db/" + name));
    }
}
