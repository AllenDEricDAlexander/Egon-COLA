package top.egon.cola.archetype.source.lightopen.infrastructure.config.datasource;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.boot.test.context.SpringBootTest(
        classes = top.egon.cola.archetype.source.lightopen.start.StudentManagementApplication.class,
        properties = {"app.integrations.rabbitmq.enabled=false", "app.integrations.redis.enabled=false",
                "app.integrations.external-http.enabled=false", "egon.cola.component.rpc.enabled=false",
                "egon.cola.component.tianshu.enabled=false"})
class RepositoryPersistenceContractTest extends top.egon.cola.archetype.source.lightopen.support.PersistenceTestSupport {
    @org.springframework.beans.factory.annotation.Autowired
    private top.egon.cola.archetype.source.lightopen.infrastructure.teaching.repo.CourseRepository courses;

    @org.springframework.beans.factory.annotation.Autowired
    private top.egon.cola.archetype.source.lightopen.domain.user.service.RoleDomainService roles;

    @Test
    void savingTheExistingRoleUsesItsStoredIdAndVersion() {
        try (var tenant = org.slf4j.MDC.putCloseable("tenantId", "1");
             var user = org.slf4j.MDC.putCloseable("userId", "test-user")) {
            var role = roles.findByCode(new top.egon.cola.archetype.source.lightopen.domain.user.vos.RoleCode("STUDENT")).orElseThrow();
            roles.save(role);
            assertThat(new org.springframework.jdbc.core.JdbcTemplate(testDataSource)
                    .queryForObject("SELECT version FROM light_roles WHERE id=1001 AND tenant_id=1", Long.class)).isEqualTo(1L);
        }
    }

    @Test
    void repositoryActuallyFillsAndUpdatesThroughTheSpringProxy() {
        try (var tenant = org.slf4j.MDC.putCloseable("tenantId", "41");
             var user = org.slf4j.MDC.putCloseable("userId", "test-user")) {
            var course = new top.egon.cola.archetype.source.lightopen.infrastructure.teaching.po.CoursePO()
                    .setCourseCode("MP_TEST").setName("Repository contract").setStatus("ACTIVE");
            assertThat(courses.save(course)).isTrue();
            assertThat(course.getId()).isPositive();
            assertThat(course.getTenantId()).isEqualTo(41L);
            assertThat(course.getVersion()).isZero();
            assertThat(course.getDeletedAt()).isNull();
            assertThat(courses.getById(course.getId()).getName()).isEqualTo("Repository contract");
            course.setName("Updated contract");
            assertThat(courses.updateById(course)).isTrue();
            assertThat(courses.getById(course.getId()).getVersion()).isEqualTo(1L);
            org.slf4j.MDC.put("tenantId", "42");
            assertThat(courses.getById(course.getId())).isNull();
        }
    }


    @Test
    void everyMapperPublishesExplicitActiveReadsAndVersionedDeletion() throws Exception {
        List<Path> mappings;
        try (var files = Files.walk(Path.of("src/main/resources/mybatis/mapper"))) {
            mappings = files.filter(file -> file.toString().endsWith("DAO.xml")).toList();
        }
        assertThat(mappings).hasSize(8);
        for (Path path : mappings) {
            String xml = Files.readString(path);
            assertThat(xml).as(path.toString()).doesNotContain("is_deleted", "${");
            assertThat(xml).contains("selectActiveById", "selectActiveByIds", "deleteVersionedById", "deleted_at IS NULL", "MP_OPTLOCK_VERSION_ORIGINAL");
        }
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            for (Path path : paths.filter(file -> file.toString().endsWith("DomainService.java")).toList()) {
                assertThat(Files.readString(path)).doesNotContainPattern("interface\\s+\\w+DomainService\\s*<").doesNotContain("EgonColaIService", "EgonModel", "repo.po");
            }
        }
    }
}
