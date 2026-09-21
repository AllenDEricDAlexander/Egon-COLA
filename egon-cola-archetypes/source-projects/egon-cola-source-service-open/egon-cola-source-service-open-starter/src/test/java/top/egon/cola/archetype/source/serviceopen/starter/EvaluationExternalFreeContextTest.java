package top.egon.cola.archetype.source.serviceopen.starter;

import top.egon.cola.archetype.source.serviceopen.domain.course.service.OrganizationDirectoryService;
import top.egon.cola.archetype.source.serviceopen.infrastructure.client.organization.OrganizationDirectoryClient;
import top.egon.cola.archetype.source.serviceopen.infrastructure.client.organization.impl.LocalOrganizationDirectoryClientImpl;
import top.egon.cola.archetype.source.serviceopen.infrastructure.course.service.impl.OrganizationDirectoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import top.egon.cola.component.dtp.context.DtpTaskDecorator;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"dubbo.protocol.port=-1", "dubbo.application.qos-enable=false"})
class EvaluationExternalFreeContextTest extends top.egon.cola.archetype.source.serviceopen.support.PersistenceTestSupport {

    @Autowired private ApplicationContext context;
    @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired private top.egon.cola.archetype.source.serviceopen.domain.course.service.CourseDomainService courseDomainService;
    @Autowired private top.egon.cola.archetype.source.serviceopen.infrastructure.course.repo.CourseRepository courses;
    @Autowired private Environment environment;
    @Autowired private OrganizationDirectoryService organizationDirectory;
    @Autowired private DtpTaskDecorator dtpTaskDecorator;

    @Test
    void savingAnExistingDomainCourseUpdatesWithItsLoadedVersion() {
        try (var tenant = org.slf4j.MDC.putCloseable("tenantId", "41");
             var user = org.slf4j.MDC.putCloseable("userId", "service-test")) {
            new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                var course = courseDomainService.createCourse(new top.egon.cola.archetype.source.serviceopen.domain.course.vos.CourseCode("UPDATE_TEST"), "Before", 2);
                course = courseDomainService.save(course);
                course.setName("After");
                courseDomainService.save(course);
                assertThat(courses.getById(course.getId()).getName()).isEqualTo("After");
                assertThat(courses.getById(course.getId()).getVersion()).isEqualTo(1L);
            });
        }
    }

    @Test
    void shouldAssembleWithoutExternalInfrastructure() {
        assertThat(context.containsBean("courseManage")).isTrue();
        assertThat(context.containsBean("evaluationExamManage")).isTrue();
        assertThat(context.containsBean("scoreManage")).isTrue();
        assertThat(context.containsBean("mqMessageService")).isTrue();
        assertThat(context.containsBean("rabbitMqMessageService")).isFalse();
        assertThat(environment.getProperty("app.integrations.rabbitmq.enabled", Boolean.class))
                .isFalse();
        assertThat(environment.getProperty(
                "app.integrations.rabbitmq.listener-auto-startup", Boolean.class)).isFalse();
        assertThat(environment.getProperty("app.integrations.organization.enabled", Boolean.class))
                .isFalse();
        assertThat(organizationDirectory).isInstanceOf(OrganizationDirectoryServiceImpl.class);
        assertThat(context.getBean("organizationDirectoryClient", OrganizationDirectoryClient.class))
                .isInstanceOf(LocalOrganizationDirectoryClientImpl.class);
        assertThat(context.getBeanNamesForType(OrganizationDirectoryClient.class))
                .containsExactly("organizationDirectoryClient");
        assertThat(dtpTaskDecorator).isNotNull();
        assertThat(context.getBeansOfType(org.springframework.web.bind.annotation.RestController.class))
                .isEmpty();
        assertThat(context.getBeansOfType(Object.class).keySet())
                .noneMatch(name -> name.toLowerCase().contains("openapi"));
    }
}
