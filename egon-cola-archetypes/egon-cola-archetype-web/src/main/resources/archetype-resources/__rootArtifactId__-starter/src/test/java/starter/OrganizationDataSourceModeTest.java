package ${package}.starter;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import ${package}.infrastructure.config.datasource.LogicalDataSourceFlywayMigrationStrategy;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;

class OrganizationDataSourceModeTest {

    @Test
    void shouldProvideFinalMigrationsWithGlobalDailySequenceAndHeaders() throws Exception {
        List<String> resources = List.of(
            "db/migration/default/V20260724_001__init_organization_default_schema.sql",
            "db/migration/sharding/single/V20260724_002__init_organization_single_schema.sql",
            "db/migration/sharding/shard/V20260724_003__init_organization_sharding_schema.sql");

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
    void shouldStartBothShardingModesWithOnlyTheTestProfile() {
        assertShardingContextStarts(false);
        assertShardingContextStarts(true);
    }

    @Test
    void shouldKeepSingleModeOnBootDataSourceAndFlyway() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                OrganizationApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("test")
                .properties("spring.main.banner-mode=off")
                .run()) {
            assertThat(context.getEnvironment().getActiveProfiles())
                .containsExactly("test");
            assertThat(context.getBean(DataSource.class))
                .isInstanceOf(HikariDataSource.class);
            assertThat(context.getBeansOfType(FlywayMigrationStrategy.class))
                .isEmpty();
            assertThat(context.getBean(Flyway.class).info().applied())
                .isNotEmpty();
        }
    }

    private static void assertShardingContextStarts(boolean readwrite) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                OrganizationApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("test")
                .properties(testProperties(readwrite))
                .run(sharedH2FlywayTargets(readwrite))) {
            assertThat(context.getEnvironment().getActiveProfiles())
                .containsExactly("test");
            assertThat(context.getBean(DataSource.class).getClass().getName())
                .contains("ShardingSphereDataSource");
            assertThat(context.getBean(FlywayMigrationStrategy.class))
                    .isInstanceOf(LogicalDataSourceFlywayMigrationStrategy.class);
            assertThat(context.getBean(jakarta.persistence.EntityManagerFactory.class))
                .isNotNull();
        }
    }

    private static Map<String, Object> testProperties(boolean readwrite) {
        // ShardingSphere 将 H2 识别为 MySQL 兼容存储；共享测试 catalog，
        // 使 JDBC 元数据能够模拟 PostgreSQL 的公共 public schema。
        String sharedUrl = h2Url(readwrite ? "organization-readwrite" : "organization-sharding");
        return Map.ofEntries(
            Map.entry("ORGANIZATION_SHARDING_DRIVER_CLASS_NAME", "org.h2.Driver"),
            Map.entry("ORGANIZATION_SHARDING_SINGLE_URL", sharedUrl),
            Map.entry("ORGANIZATION_SHARDING_SHARD_0_URL", sharedUrl),
            Map.entry("ORGANIZATION_SHARDING_SHARD_1_URL", sharedUrl),
            Map.entry("ORGANIZATION_SHARDING_USERNAME", "sa"),
            Map.entry("ORGANIZATION_SHARDING_PASSWORD", ""),
            Map.entry("ORGANIZATION_SINGLE_PRIMARY_URL", sharedUrl),
            Map.entry("ORGANIZATION_SINGLE_REPLICA_0_URL", sharedUrl),
            Map.entry("ORGANIZATION_SHARD_0_PRIMARY_URL", sharedUrl),
            Map.entry("ORGANIZATION_SHARD_0_REPLICA_0_URL", sharedUrl),
            Map.entry("ORGANIZATION_SHARD_1_PRIMARY_URL", sharedUrl),
            Map.entry("ORGANIZATION_SHARD_1_REPLICA_0_URL", sharedUrl),
            Map.entry("ORGANIZATION_SINGLE_PRIMARY_USERNAME", "sa"),
            Map.entry("ORGANIZATION_SINGLE_PRIMARY_PASSWORD", ""),
            Map.entry("ORGANIZATION_SINGLE_REPLICA_0_USERNAME", "sa"),
            Map.entry("ORGANIZATION_SINGLE_REPLICA_0_PASSWORD", ""),
            Map.entry("ORGANIZATION_SHARD_0_PRIMARY_USERNAME", "sa"),
            Map.entry("ORGANIZATION_SHARD_0_PRIMARY_PASSWORD", ""),
            Map.entry("ORGANIZATION_SHARD_0_REPLICA_0_USERNAME", "sa"),
            Map.entry("ORGANIZATION_SHARD_0_REPLICA_0_PASSWORD", ""),
            Map.entry("ORGANIZATION_SHARD_1_PRIMARY_USERNAME", "sa"),
            Map.entry("ORGANIZATION_SHARD_1_PRIMARY_PASSWORD", ""),
            Map.entry("ORGANIZATION_SHARD_1_REPLICA_0_USERNAME", "sa"),
            Map.entry("ORGANIZATION_SHARD_1_REPLICA_0_PASSWORD", ""),
            Map.entry("spring.main.banner-mode", "off"));
    }

    private static String[] sharedH2FlywayTargets(boolean readwrite) {
        String single = "classpath:db/migration/sharding/single";
        String shard = "classpath:db/migration/sharding/shard";
        String[] targetNames = readwrite
            ? new String[] {"single_primary", "shard_0_primary", "shard_1_primary"}
            : new String[] {"single", "shard_0", "shard_1"};
        String topologyPrefix = readwrite
            ? "app.sharding-readwrite"
            : "app.sharding";
        return new String[] {
            "--spring.profiles.active=test",
            "--app.datasource.mode="
                + (readwrite ? "SHARDING_READWRITE" : "SHARDING"),
            "--" + topologyPrefix + ".flyway.targets[0].data-source-name=" + targetNames[0],
            "--" + topologyPrefix + ".flyway.targets[0].locations[0]=" + single,
            "--" + topologyPrefix + ".flyway.targets[0].locations[1]=" + shard,
            "--" + topologyPrefix + ".flyway.targets[1].data-source-name=" + targetNames[1],
            "--" + topologyPrefix + ".flyway.targets[1].locations[0]=" + single,
            "--" + topologyPrefix + ".flyway.targets[1].locations[1]=" + shard,
            "--" + topologyPrefix + ".flyway.targets[2].data-source-name=" + targetNames[2],
            "--" + topologyPrefix + ".flyway.targets[2].locations[0]=" + single,
            "--" + topologyPrefix + ".flyway.targets[2].locations[1]=" + shard
        };
    }

    private static String h2Url(String database) {
        return "jdbc:h2:mem:"
            + database
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
            + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
    }
}
