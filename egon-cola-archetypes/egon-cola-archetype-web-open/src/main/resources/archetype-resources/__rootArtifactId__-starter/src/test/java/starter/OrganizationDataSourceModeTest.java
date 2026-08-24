package ${package}.starter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class OrganizationDataSourceModeTest {

    @Test
    void shouldStartDefaultAndReadwriteModesWithoutAutomaticSchemaMutation() {
        assertShardingContextStarts(false);
        assertShardingContextStarts(true);
    }

    @Test
    void shouldExposeManualOnlyDatabaseBoundary() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                        OrganizationApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("test")
                .properties(testProperties(false))
                .run()) {
            assertThat(context.getEnvironment().getActiveProfiles())
                    .containsExactly("test");
            assertThat(context.getBean(DataSource.class).getClass().getName())
                    .contains("ShardingSphereDataSource");
        }
    }

    private static void assertShardingContextStarts(boolean readwrite) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                        OrganizationApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("test")
                .properties(testProperties(readwrite))
                .run("--app.datasource.mode="
                        + (readwrite ? "SHARDING_READWRITE" : "SHARDING"))) {
            assertThat(context.getEnvironment().getActiveProfiles())
                    .containsExactly("test");
            assertThat(context.getBean(DataSource.class).getClass().getName())
                    .contains("ShardingSphereDataSource");
        }
    }

    private static Map<String, Object> testProperties(boolean readwrite) {
        String topology = readwrite
                ? "organization-readwrite"
                : "organization-sharding";
        String masterDataUrl = h2Url(topology + "-master-data");
        String shardZeroUrl = h2Url(topology + "-shard-0");
        String shardOneUrl = h2Url(topology + "-shard-1");
        return Map.ofEntries(
                Map.entry("app.sharding.database-name", "PUBLIC"),
                Map.entry("ORGANIZATION_SHARDING_DRIVER_CLASS_NAME", "org.h2.Driver"),
                Map.entry("ORGANIZATION_SHARDING_MASTER_DATA_URL", masterDataUrl),
                Map.entry("ORGANIZATION_SHARDING_SHARD_0_URL", shardZeroUrl),
                Map.entry("ORGANIZATION_SHARDING_SHARD_1_URL", shardOneUrl),
                Map.entry("ORGANIZATION_SHARDING_USERNAME", "sa"),
                Map.entry("ORGANIZATION_SHARDING_PASSWORD", ""),
                Map.entry("ORGANIZATION_MASTER_DATA_PRIMARY_URL", masterDataUrl),
                Map.entry("ORGANIZATION_MASTER_DATA_REPLICA_0_URL", masterDataUrl),
                Map.entry("ORGANIZATION_SHARD_0_PRIMARY_URL", shardZeroUrl),
                Map.entry("ORGANIZATION_SHARD_0_REPLICA_0_URL", shardZeroUrl),
                Map.entry("ORGANIZATION_SHARD_1_PRIMARY_URL", shardOneUrl),
                Map.entry("ORGANIZATION_SHARD_1_REPLICA_0_URL", shardOneUrl),
                Map.entry("ORGANIZATION_MASTER_DATA_PRIMARY_USERNAME", "sa"),
                Map.entry("ORGANIZATION_MASTER_DATA_PRIMARY_PASSWORD", ""),
                Map.entry("ORGANIZATION_MASTER_DATA_REPLICA_0_USERNAME", "sa"),
                Map.entry("ORGANIZATION_MASTER_DATA_REPLICA_0_PASSWORD", ""),
                Map.entry("ORGANIZATION_SHARD_0_PRIMARY_USERNAME", "sa"),
                Map.entry("ORGANIZATION_SHARD_0_PRIMARY_PASSWORD", ""),
                Map.entry("ORGANIZATION_SHARD_0_REPLICA_0_USERNAME", "sa"),
                Map.entry("ORGANIZATION_SHARD_0_REPLICA_0_PASSWORD", ""),
                Map.entry("ORGANIZATION_SHARD_1_PRIMARY_USERNAME", "sa"),
                Map.entry("ORGANIZATION_SHARD_1_PRIMARY_PASSWORD", ""),
                Map.entry("ORGANIZATION_SHARD_1_REPLICA_0_USERNAME", "sa"),
                Map.entry("ORGANIZATION_SHARD_1_REPLICA_0_PASSWORD", ""),
                Map.entry("dubbo.application.qos-enable", "false"),
                Map.entry("dubbo.protocol.port", "-1"),
                Map.entry("dubbo.provider.export", "false"),
                Map.entry("spring.main.banner-mode", "off"));
    }

    private static String h2Url(String database) {
        return "jdbc:h2:mem:" + database
                + ";MODE=PostgreSQL;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
    }
}
