package ${package}.infrastructure.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import ${package}.infrastructure.teaching.repo.po.ClassCourseSchedulePO;
import ${package}.infrastructure.teaching.service.impl.CourseDomainServiceImpl;
import ${package}.infrastructure.teaching.service.impl.SchoolClassDomainServiceImpl;
import ${package}.infrastructure.user.service.impl.UserDomainServiceImpl;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.common.id.generator.UuidV7Generator;

class LightDataSourceModeTest {

    private static final String NODE_MAP =
            "0=shard_0:0,1=shard_0:1,2=shard_1:0,3=shard_1:1";

    @Test
    void shouldUseUuidV7ForEverySurrogateKey() {
        UuidV7Generator idGenerator = new UuidV7Generator();
        String userId = new UserDomainServiceImpl(idGenerator)
                .createUser("external-1", "Mario", "mario@example.com")
                .id()
                .value();
        String courseId = new CourseDomainServiceImpl(idGenerator)
                .createCourse(
                        new ${package}.domain.teaching.vos.CourseCode("math"),
                        "Mathematics")
                .id();
        String schoolClassId = new SchoolClassDomainServiceImpl(idGenerator)
                .createSchoolClass(
                        "Class One",
                        new ${package}.domain.teaching.vos.Semester("2026-FALL"))
                .id()
                .value();
        String scheduleId = idGenerator.nextId();
        ClassCourseSchedulePO schedule = new ClassCourseSchedulePO(
                scheduleId,
                schoolClassId,
                courseId,
                LocalDateTime.of(2026, 9, 1, 9, 0),
                LocalDateTime.of(2026, 9, 1, 10, 0),
                Instant.now());

        assertThat(List.<String>of(userId, courseId, schoolClassId, schedule.getId()))
                .allSatisfy(LightDataSourceModeTest::assertUuidV7);
    }

    @Test
    void shouldRouteClassAndScheduleByTheSameSchoolClassId() {
        String schoolClassId = new UuidV7Generator().nextId();
        ShardingNodeMap nodeMap = ShardingNodeMap.parse("4", NODE_MAP);

        assertThat(nodeMap.route(schoolClassId))
                .isEqualTo(nodeMap.route(schoolClassId));
    }

    @Test
    void shouldProvideFinalMigrationsWithGlobalDailySequenceAndHeaders() throws Exception {
        List<String> resources = List.of(
                "db/migration/sharding/master-data/"
                        + "V20260726_001__init_light_master_data_schema.sql",
                "db/migration/sharding/shard/"
                        + "V20260726_002__init_light_sharded_schema.sql");

        for (String resource : resources) {
            String sql = new ClassPathResource(resource)
                    .getContentAsString(StandardCharsets.UTF_8);
            assertThat(sql)
                    .startsWith("-- 变更内容：")
                    .contains("\n-- 影响范围：")
                    .contains("\n-- 兼容性说明：");
        }
        assertThat(resources).extracting(path -> path.substring(path.indexOf('V') + 10, path.indexOf("__")))
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
                        ${package}.start.StudentManagementApplication.class)
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
                        ${package}.start.StudentManagementApplication.class)
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
        String topology = readwrite ? "light-readwrite" : "light-sharding";
        String masterDataUrl = h2Url(topology + "-master-data");
        String shardZeroUrl = h2Url(topology + "-shard-0");
        String shardOneUrl = h2Url(topology + "-shard-1");
        return Map.ofEntries(
                Map.entry("app.sharding.database-name", "PUBLIC"),
                Map.entry("LIGHT_SHARDING_DRIVER_CLASS_NAME", "org.h2.Driver"),
                Map.entry("LIGHT_SHARDING_MASTER_DATA_URL", masterDataUrl),
                Map.entry("LIGHT_SHARDING_SHARD_0_URL", shardZeroUrl),
                Map.entry("LIGHT_SHARDING_SHARD_1_URL", shardOneUrl),
                Map.entry("LIGHT_SHARDING_USERNAME", "sa"),
                Map.entry("LIGHT_SHARDING_PASSWORD", ""),
                Map.entry("LIGHT_MASTER_DATA_PRIMARY_URL", masterDataUrl),
                Map.entry("LIGHT_MASTER_DATA_REPLICA_0_URL", masterDataUrl),
                Map.entry("LIGHT_SHARD_0_PRIMARY_URL", shardZeroUrl),
                Map.entry("LIGHT_SHARD_0_REPLICA_0_URL", shardZeroUrl),
                Map.entry("LIGHT_SHARD_1_PRIMARY_URL", shardOneUrl),
                Map.entry("LIGHT_SHARD_1_REPLICA_0_URL", shardOneUrl),
                Map.entry("LIGHT_MASTER_DATA_PRIMARY_USERNAME", "sa"),
                Map.entry("LIGHT_MASTER_DATA_PRIMARY_PASSWORD", ""),
                Map.entry("LIGHT_MASTER_DATA_REPLICA_0_USERNAME", "sa"),
                Map.entry("LIGHT_MASTER_DATA_REPLICA_0_PASSWORD", ""),
                Map.entry("LIGHT_SHARD_0_PRIMARY_USERNAME", "sa"),
                Map.entry("LIGHT_SHARD_0_PRIMARY_PASSWORD", ""),
                Map.entry("LIGHT_SHARD_0_REPLICA_0_USERNAME", "sa"),
                Map.entry("LIGHT_SHARD_0_REPLICA_0_PASSWORD", ""),
                Map.entry("LIGHT_SHARD_1_PRIMARY_USERNAME", "sa"),
                Map.entry("LIGHT_SHARD_1_PRIMARY_PASSWORD", ""),
                Map.entry("LIGHT_SHARD_1_REPLICA_0_USERNAME", "sa"),
                Map.entry("LIGHT_SHARD_1_REPLICA_0_PASSWORD", ""),
                Map.entry("spring.main.banner-mode", "off"));
    }

    private static String h2Url(String database) {
        return "jdbc:h2:mem:"
                + database
                + ";MODE=PostgreSQL;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
    }

    private static void assertUuidV7(String value) {
        assertThat(value).hasSize(36);
        assertThat(UUID.fromString(value).version()).isEqualTo(7);
    }
}
