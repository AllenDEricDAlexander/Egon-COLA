package top.egon.cola.archetype.source.webopen.starter;

import top.egon.cola.archetype.source.webopen.domain.teaching.service.EvaluationQueryService;
import top.egon.cola.archetype.source.webopen.infrastructure.client.evaluation.impl.GrpcEvaluationQueryClientImpl;
import top.egon.cola.archetype.source.webopen.infrastructure.client.evaluation.impl.LocalEvaluationQueryClientImpl;
import top.egon.cola.archetype.source.webopen.infrastructure.teaching.service.impl.EvaluationQueryServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = OrganizationApplication.class,
        properties = "spring.profiles.active=test")
class OrganizationExternalFreeContextTest extends top.egon.cola.archetype.source.webopen.support.PersistenceTestSupport {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private EvaluationQueryService evaluationQueryPort;

    @Test
    void testProfileIsExternalFree() {
        assertThat(context.getBeansOfType(RedisConnectionFactory.class)).isEmpty();
        assertThat(context.getBeansOfType(ConnectionFactory.class)).isEmpty();
        assertThat(Arrays.stream(context.getBeanDefinitionNames())
                .filter(name -> name.toLowerCase().contains("nacos"))).isEmpty();
        assertThat(environment.getProperty("dubbo.registry.address")).isEqualTo("N/A");
        assertThat(environment.getProperty("dubbo.protocol.name")).isEqualTo("injvm");
        assertThat(environment.getProperty("organization.integrations.redis.enabled"))
                .isEqualTo("false");
        assertThat(environment.getProperty("organization.integrations.rabbit.enabled"))
                .isEqualTo("false");
        assertThat(environment.getProperty("organization.integrations.evaluation.enabled"))
                .isEqualTo("false");
        assertThat(evaluationQueryPort).isInstanceOf(EvaluationQueryServiceImpl.class);
        assertThat(context.getBean("evaluationQueryClient")).isInstanceOf(LocalEvaluationQueryClientImpl.class);
        assertThat(context.getBeansOfType(GrpcEvaluationQueryClientImpl.class)).isEmpty();
    }

    @Test
    void doesNotExposeForbiddenRuntimeBeans() {
        assertThat(Arrays.stream(context.getBeanDefinitionNames())
                .map(name -> context.getType(name, false))
                .filter(type -> type != null)
                .map(Class::getName)
                .filter(name -> name.contains("yuheng")
                        || name.contains("fly" + "way")
                        || name.contains("jpa")
                        || name.contains("organization-" + "facade")
                        || name.contains("evaluation-" + "facade")))
                .isEmpty();
    }
}
