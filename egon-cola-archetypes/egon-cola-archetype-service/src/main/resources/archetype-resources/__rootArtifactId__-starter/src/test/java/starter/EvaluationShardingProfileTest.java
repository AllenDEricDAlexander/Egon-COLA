package ${package}.starter;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;

class EvaluationShardingProfileTest {

    @Test
    void shouldProvideFinalMigrationsWithGlobalDailySequenceAndHeaders() throws Exception {
        List<String> resources = List.of(
                "db/migration/default/V20260724_001__init_evaluation_default_schema.sql",
                "db/migration/sharding/single/V20260724_002__init_evaluation_single_schema.sql",
                "db/migration/sharding/shard/V20260724_003__init_evaluation_sharding_schema.sql");

        for (String resource : resources) {
            String sql = new ClassPathResource(resource)
                    .getContentAsString(StandardCharsets.UTF_8);
            assertThat(sql)
                    .startsWith("-- 变更内容：")
                    .contains("\n-- 影响范围：")
                    .contains("\n-- 兼容性说明：");
        }
        assertThat(resources)
                .extracting(path -> path.substring(path.indexOf('V') + 10, path.indexOf("__")))
                .containsExactly("001", "002", "003");
    }

    @Test
    void shouldStartPrimaryOnlyAndReadwriteShardingProfilesWithJpaValidation() {
        assertShardingContextStarts(false);
        assertShardingContextStarts(true);
    }

    private static void assertShardingContextStarts(boolean readwrite) {
        String[] profiles = readwrite
                ? new String[] {"test", "sharding", "readwrite"}
                : new String[] {"test", "sharding"};
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                EvaluationServiceApplication.class)
                .web(WebApplicationType.NONE)
                .profiles(profiles)
                .properties(testProperties(readwrite))
                .run(sharedH2FlywayTargets(readwrite))) {
            assertThat(context.getBean(DataSource.class).getClass().getName())
                    .contains("ShardingSphereDataSource");
            assertThat(context.getBean(jakarta.persistence.EntityManagerFactory.class))
                    .isNotNull();
        }
    }

    private static Map<String, Object> testProperties(boolean readwrite) {
        // ShardingSphere 将 H2 识别为 MySQL 兼容存储；共享测试 catalog，
        // 使 JDBC 元数据能够模拟 PostgreSQL 的公共 public schema。
        String sharedUrl = h2Url(readwrite ? "evaluation-readwrite" : "evaluation-sharding");
        return Map.ofEntries(
                Map.entry("EVALUATION_SHARDING_DRIVER_CLASS_NAME", "org.h2.Driver"),
                Map.entry("EVALUATION_SHARDING_SINGLE_URL", sharedUrl),
                Map.entry("EVALUATION_SHARDING_SHARD_0_URL", sharedUrl),
                Map.entry("EVALUATION_SHARDING_SHARD_1_URL", sharedUrl),
                Map.entry("EVALUATION_SHARDING_USERNAME", "sa"),
                Map.entry("EVALUATION_SHARDING_PASSWORD", ""),
                Map.entry("EVALUATION_SINGLE_PRIMARY_URL", sharedUrl),
                Map.entry("EVALUATION_SINGLE_REPLICA_0_URL", sharedUrl),
                Map.entry("EVALUATION_SHARD_0_PRIMARY_URL", sharedUrl),
                Map.entry("EVALUATION_SHARD_0_REPLICA_0_URL", sharedUrl),
                Map.entry("EVALUATION_SHARD_1_PRIMARY_URL", sharedUrl),
                Map.entry("EVALUATION_SHARD_1_REPLICA_0_URL", sharedUrl),
                Map.entry("EVALUATION_SINGLE_PRIMARY_USERNAME", "sa"),
                Map.entry("EVALUATION_SINGLE_PRIMARY_PASSWORD", ""),
                Map.entry("EVALUATION_SINGLE_REPLICA_0_USERNAME", "sa"),
                Map.entry("EVALUATION_SINGLE_REPLICA_0_PASSWORD", ""),
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

    private static String[] sharedH2FlywayTargets(boolean readwrite) {
        String single = "classpath:db/migration/sharding/single";
        String shard = "classpath:db/migration/sharding/shard";
        String[] targetNames = readwrite
                ? new String[] {"single_primary", "shard_0_primary", "shard_1_primary"}
                : new String[] {"single", "shard_0", "shard_1"};
        return new String[] {
            "--spring.profiles.active="
                    + (readwrite ? "test,sharding,readwrite" : "test,sharding"),
            "--app.sharding.flyway.targets[0].data-source-name=" + targetNames[0],
            "--app.sharding.flyway.targets[0].locations[0]=" + single,
            "--app.sharding.flyway.targets[0].locations[1]=" + shard,
            "--app.sharding.flyway.targets[1].data-source-name=" + targetNames[1],
            "--app.sharding.flyway.targets[1].locations[0]=" + single,
            "--app.sharding.flyway.targets[1].locations[1]=" + shard,
            "--app.sharding.flyway.targets[2].data-source-name=" + targetNames[2],
            "--app.sharding.flyway.targets[2].locations[0]=" + single,
            "--app.sharding.flyway.targets[2].locations[1]=" + shard
        };
    }

    private static String h2Url(String database) {
        return "jdbc:h2:mem:" + database
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
    }
}
