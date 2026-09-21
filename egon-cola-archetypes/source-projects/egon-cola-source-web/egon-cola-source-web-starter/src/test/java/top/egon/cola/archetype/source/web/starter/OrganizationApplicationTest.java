package top.egon.cola.archetype.source.web.starter;

import top.egon.cola.archetype.source.web.domain.teaching.service.EvaluationQueryService;
import top.egon.cola.archetype.source.web.infrastructure.client.evaluation.impl.NativeEvaluationQueryClientImpl;
import top.egon.cola.archetype.source.web.infrastructure.client.evaluation.impl.LocalEvaluationQueryClientImpl;
import top.egon.cola.archetype.source.web.infrastructure.teaching.service.impl.EvaluationQueryServiceImpl;
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
class OrganizationApplicationTest extends top.egon.cola.archetype.source.web.support.PersistenceTestSupport {

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
        assertThat(environment.getProperty("egon.cola.component.tianshu.registry.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("egon.cola.component.rpc.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("organization.integrations.redis.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("organization.integrations.rabbit.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("organization.integrations.evaluation.enabled")).isEqualTo("false");
        // The Domain Service always answers; only the named Client behind it is transport-specific.
        assertThat(evaluationQueryPort).isInstanceOf(EvaluationQueryServiceImpl.class);
        assertThat(context.getBean("evaluationQueryClient")).isInstanceOf(LocalEvaluationQueryClientImpl.class);
        assertThat(context.getBeansOfType(NativeEvaluationQueryClientImpl.class)).isEmpty();
    }
}
