package ${package}.starter;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;

class EvaluationDataSourceModeTest {

    @Test
    void shouldProvideFinalMigrationsWithGlobalDailySequenceAndHeaders() throws Exception {
        List<String> resources = List.of(
                "db/migration/sharding/master-data/"
                        + "V20260726_001__init_evaluation_master_data_schema.sql",
                "db/migration/sharding/shard/"
                        + "V20260726_002__init_evaluation_sharded_schema.sql");

        for (String resource : resources) {
            String sql = new ClassPathResource(resource)
                    .getContentAsString(StandardCharsets.UTF_8);
            assertThat(sql)
                    .startsWith("-- 变更内容：")
                    .contains("\n-- 影响范围：")
                    .contains("\n-- 兼容性说明：");
        }
        assertThat(resources)
                .extracting(path -> path.substring(
                        path.indexOf('V') + 10, path.indexOf("__")))
                .containsExactly("001", "002");
    }

    @Test
    void shouldStartDefaultAndReadwriteModesWithOnlyTheTestProfile() {
        assertShardingContextStarts(false);
        assertShardingContextStarts(true);
    }

    @Test
    void shouldDefaultToShardingSphereWithoutLogicalFlywayBean() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                        EvaluationServiceApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("test")
                .properties(testProperties(false))
                .run()) {
            assertThat(context.getEnvironment().getActiveProfiles())
                    .containsExactly("test");
            assertThat(context.getEnvironment().getProperty("app.datasource.mode"))
                    .isEqualTo("SHARDING");
            assertThat(context.getBean(DataSource.class).getClass().getName())
                    .contains("ShardingSphereDataSource");
            assertThat(context.getBeansOfType(Flyway.class)).isEmpty();
        }
    }

    private static void assertShardingContextStarts(boolean readwrite) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                        EvaluationServiceApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("test")
                .properties(testProperties(readwrite))
                .run("--app.datasource.mode="
                        + (readwrite ? "SHARDING_READWRITE" : "SHARDING"))) {
            assertThat(context.getEnvironment().getActiveProfiles())
                    .containsExactly("test");
            assertThat(context.getBean(DataSource.class).getClass().getName())
                    .contains("ShardingSphereDataSource");
            assertThat(context.getBeansOfType(Flyway.class)).isEmpty();
            assertThat(context.getBean(
                            jakarta.persistence.EntityManagerFactory.class))
                    .isNotNull();
        }
    }

    private static Map<String, Object> testProperties(boolean readwrite) {
        String topology = readwrite
                ? "evaluation-readwrite"
                : "evaluation-sharding";
        String masterDataUrl = h2Url(topology + "-master-data");
        String shardZeroUrl = h2Url(topology + "-shard-0");
        String shardOneUrl = h2Url(topology + "-shard-1");
        return Map.ofEntries(
                Map.entry("EVALUATION_SHARDING_DRIVER_CLASS_NAME", "org.h2.Driver"),
                Map.entry("EVALUATION_SHARDING_MASTER_DATA_URL", masterDataUrl),
                Map.entry("EVALUATION_SHARDING_SHARD_0_URL", shardZeroUrl),
                Map.entry("EVALUATION_SHARDING_SHARD_1_URL", shardOneUrl),
                Map.entry("EVALUATION_SHARDING_USERNAME", "sa"),
                Map.entry("EVALUATION_SHARDING_PASSWORD", ""),
                Map.entry("EVALUATION_MASTER_DATA_PRIMARY_URL", masterDataUrl),
                Map.entry("EVALUATION_MASTER_DATA_REPLICA_0_URL", masterDataUrl),
                Map.entry("EVALUATION_SHARD_0_PRIMARY_URL", shardZeroUrl),
                Map.entry("EVALUATION_SHARD_0_REPLICA_0_URL", shardZeroUrl),
                Map.entry("EVALUATION_SHARD_1_PRIMARY_URL", shardOneUrl),
                Map.entry("EVALUATION_SHARD_1_REPLICA_0_URL", shardOneUrl),
                Map.entry("EVALUATION_MASTER_DATA_PRIMARY_USERNAME", "sa"),
                Map.entry("EVALUATION_MASTER_DATA_PRIMARY_PASSWORD", ""),
                Map.entry("EVALUATION_MASTER_DATA_REPLICA_0_USERNAME", "sa"),
                Map.entry("EVALUATION_MASTER_DATA_REPLICA_0_PASSWORD", ""),
                Map.entry("EVALUATION_SHARD_0_PRIMARY_USERNAME", "sa"),
                Map.entry("EVALUATION_SHARD_0_PRIMARY_PASSWORD", ""),
                Map.entry("EVALUATION_SHARD_0_REPLICA_0_USERNAME", "sa"),
                Map.entry("EVALUATION_SHARD_0_REPLICA_0_PASSWORD", ""),
                Map.entry("EVALUATION_SHARD_1_PRIMARY_USERNAME", "sa"),
                Map.entry("EVALUATION_SHARD_1_PRIMARY_PASSWORD", ""),
                Map.entry("EVALUATION_SHARD_1_REPLICA_0_USERNAME", "sa"),
                Map.entry("EVALUATION_SHARD_1_REPLICA_0_PASSWORD", ""),
                Map.entry("dubbo.application.qos-enable", "false"),
                Map.entry("dubbo.protocol.port", "-1"),
                Map.entry("dubbo.provider.export", "false"),
                Map.entry("spring.main.banner-mode", "off"));
    }

    private static String h2Url(String database) {
        return "jdbc:h2:mem:" + database
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
    }
}
