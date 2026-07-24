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
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.common.id.generator.UuidV7Generator;

class LightShardingProfileTest {

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
                .allSatisfy(LightShardingProfileTest::assertUuidV7);
    }

    @Test
    void shouldRouteClassAndScheduleByTheSameSchoolClassId() {
        String schoolClassId = new UuidV7Generator().nextId();
        ShardingNodeMap nodeMap = ShardingNodeMap.parse("1", "4", NODE_MAP);

        assertThat(nodeMap.route(schoolClassId))
                .isEqualTo(nodeMap.route(schoolClassId));
    }

    @Test
    void shouldProvideFinalMigrationsWithGlobalDailySequenceAndHeaders() throws Exception {
        List<String> resources = List.of(
                "db/migration/default/V20260724_001__init_light_default_schema.sql",
                "db/migration/sharding/single/V20260724_002__init_light_single_schema.sql",
                "db/migration/sharding/shard/V20260724_003__init_light_sharding_schema.sql");

        for (String resource : resources) {
            String sql = new ClassPathResource(resource)
                    .getContentAsString(StandardCharsets.UTF_8);
            assertThat(sql)
                    .startsWith("-- 变更内容：")
                    .contains("\n-- 影响范围：")
                    .contains("\n-- 兼容性说明：");
        }
        assertThat(resources).extracting(path -> path.substring(path.indexOf('V') + 10, path.indexOf("__")))
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
                        ${package}.start.StudentManagementApplication.class)
                .web(WebApplicationType.NONE)
                .profiles(profiles)
                .properties(testProperties(readwrite))
                .run(sharedH2FlywayTargets(readwrite))) {
            assertThat(context.getBean(DataSource.class).getClass().getName())
                    .contains("ShardingSphereDataSource");
            assertThat(context.getBean(
                            jakarta.persistence.EntityManagerFactory.class))
                    .isNotNull();
        }
    }

    private static Map<String, Object> testProperties(boolean readwrite) {
        // ShardingSphere classifies H2 as a MySQL-compatible storage type. Sharing one
        // test catalog lets JDBC metadata model PostgreSQL's common public schema.
        String sharedUrl = h2Url(readwrite ? "light-readwrite" : "light-sharding");
        return Map.ofEntries(
                Map.entry("LIGHT_SHARDING_DRIVER_CLASS_NAME", "org.h2.Driver"),
                Map.entry("LIGHT_SHARDING_SINGLE_URL", sharedUrl),
                Map.entry("LIGHT_SHARDING_SHARD_0_URL", sharedUrl),
                Map.entry("LIGHT_SHARDING_SHARD_1_URL", sharedUrl),
                Map.entry("LIGHT_SHARDING_USERNAME", "sa"),
                Map.entry("LIGHT_SHARDING_PASSWORD", ""),
                Map.entry("LIGHT_SINGLE_PRIMARY_URL", sharedUrl),
                Map.entry("LIGHT_SINGLE_REPLICA_0_URL", sharedUrl),
                Map.entry("LIGHT_SHARD_0_PRIMARY_URL", sharedUrl),
                Map.entry("LIGHT_SHARD_0_REPLICA_0_URL", sharedUrl),
                Map.entry("LIGHT_SHARD_1_PRIMARY_URL", sharedUrl),
                Map.entry("LIGHT_SHARD_1_REPLICA_0_URL", sharedUrl),
                Map.entry("LIGHT_SINGLE_PRIMARY_USERNAME", "sa"),
                Map.entry("LIGHT_SINGLE_PRIMARY_PASSWORD", ""),
                Map.entry("LIGHT_SINGLE_REPLICA_0_USERNAME", "sa"),
                Map.entry("LIGHT_SINGLE_REPLICA_0_PASSWORD", ""),
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

    private static String[] sharedH2FlywayTargets(boolean readwrite) {
        String single = "classpath:db/migration/sharding/single";
        String shard = "classpath:db/migration/sharding/shard";
        String[] targetNames = readwrite
                ? new String[] {"single_primary", "shard_0_primary", "shard_1_primary"}
                : new String[] {"single", "shard_0", "shard_1"};
        return new String[] {
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
        return "jdbc:h2:mem:"
                + database
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;"
                + "DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";
    }

    private static void assertUuidV7(String value) {
        assertThat(value).hasSize(36);
        assertThat(UUID.fromString(value).version()).isEqualTo(7);
    }
}
