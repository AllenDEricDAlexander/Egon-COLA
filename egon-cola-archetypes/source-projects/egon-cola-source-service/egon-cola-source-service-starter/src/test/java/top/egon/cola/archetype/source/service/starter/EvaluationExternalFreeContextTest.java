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
class EvaluationExternalFreeContextTest {

    @Autowired private ApplicationContext context;
    @Autowired private Environment environment;
    @Autowired private OrganizationDirectoryPort organizationDirectory;

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
