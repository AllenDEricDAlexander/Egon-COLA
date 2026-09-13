package top.egon.cola.archetype.source.service.starter;

import top.egon.cola.archetype.source.service.domain.client.organization.OrganizationDirectoryPort;
import top.egon.cola.archetype.source.service.infrastructure.client.organization.LocalOrganizationDirectoryStub;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"egon.cola.component.rpc.enabled=false", "egon.cola.component.tianshu.enabled=false"})
class EvaluationExternalFreeContextTest extends top.egon.cola.archetype.source.service.support.PersistenceTestSupport {

    @Autowired private ApplicationContext context;
    @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired private top.egon.cola.archetype.source.service.domain.course.service.CourseDomainService courseDomainService;
    @Autowired private top.egon.cola.archetype.source.service.infrastructure.course.repo.CourseRepository courses;
    @Autowired private Environment environment;
    @Autowired private OrganizationDirectoryPort organizationDirectory;

    @Test
    void savingAnExistingDomainCourseUpdatesWithItsLoadedVersion() {
        try (var tenant = org.slf4j.MDC.putCloseable("tenantId", "41");
             var user = org.slf4j.MDC.putCloseable("userId", "service-test")) {
            new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                var course = courseDomainService.createCourse(new top.egon.cola.archetype.source.service.domain.course.vos.CourseCode("UPDATE_TEST"), "Before", 2);
                course = courseDomainService.save(course);
                course.setName("After");
                courseDomainService.save(course);
                assertThat(courses.getById(course.getId()).getName()).isEqualTo("After");
                assertThat(courses.getById(course.getId()).getVersion()).isEqualTo(1L);
            });
        }
    }

    @Test
    void repositoryPersistsThroughTheRealSpringProxyAndIsolatesTenants() {
        try (var tenant = org.slf4j.MDC.putCloseable("tenantId", "41");
             var user = org.slf4j.MDC.putCloseable("userId", "service-test")) {
            var course = new top.egon.cola.archetype.source.service.infrastructure.course.repo.po.CoursePO();
            course.setCode("REPO_TEST");
            course.setName("Repository contract");
            course.setCredit(2);
            course.setStatus("ACTIVE");
            assertThat(courses.save(course)).isTrue();
            assertThat(course.getId()).isPositive();
            assertThat(course.getTenantId()).isEqualTo(41L);
            assertThat(course.getVersion()).isZero();
            assertThat(courses.getById(course.getId()).getName()).isEqualTo("Repository contract");
            var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<top.egon.cola.archetype.source.service.infrastructure.course.repo.po.CoursePO>(1, 10);
            assertThat(courses.selectActivePage(page).getRecords()).hasSize(1);
            org.slf4j.MDC.put("tenantId", "42");
            assertThat(courses.getById(course.getId())).isNull();
        }
    }

    @Test
    void shouldAssembleWithoutExternalInfrastructure() {
        assertThat(context.containsBean("courseManage")).isTrue();
        assertThat(context.containsBean("evaluationExamManage")).isTrue();
        assertThat(context.containsBean("scoreManage")).isTrue();
        assertThat(context.containsBean("rabbitCourseEventPublisher")).isFalse();
        assertThat(context.containsBean("rabbitExamEventPublisher")).isFalse();
        assertThat(environment.getProperty("app.integrations.rabbitmq.enabled", Boolean.class))
                .isFalse();
        assertThat(environment.getProperty(
                "app.integrations.rabbitmq.listener-auto-startup", Boolean.class)).isFalse();
        assertThat(environment.getProperty("app.integrations.organization.enabled", Boolean.class))
                .isFalse();
        assertThat(organizationDirectory).isInstanceOf(LocalOrganizationDirectoryStub.class);
        assertThat(context.containsBean("nativeOrganizationDirectoryClient")).isFalse();
    }
}
