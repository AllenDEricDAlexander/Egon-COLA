package ${package}.starter;

import ${package}.domain.client.evaluation.EvaluationQueryPort;
import ${package}.infrastructure.client.evaluation.DubboEvaluationQueryClient;
import ${package}.infrastructure.client.evaluation.LocalEvaluationQueryStub;
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
class OrganizationApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private EvaluationQueryPort evaluationQueryPort;

    @Test
    void testProfileIsExternalFree() {
        assertThat(context.getBeansOfType(RedisConnectionFactory.class)).isEmpty();
        assertThat(context.getBeansOfType(ConnectionFactory.class)).isEmpty();
        assertThat(Arrays.stream(context.getBeanDefinitionNames())
                .filter(name -> name.toLowerCase().contains("nacos"))).isEmpty();
        assertThat(environment.getProperty("dubbo.registry.address")).isEqualTo("N/A");
        assertThat(environment.getProperty("dubbo.protocol.name")).isEqualTo("injvm");
        assertThat(environment.getProperty("organization.integrations.redis.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("organization.integrations.rabbit.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("organization.integrations.evaluation.enabled")).isEqualTo("false");
        assertThat(evaluationQueryPort).isInstanceOf(LocalEvaluationQueryStub.class);
        assertThat(context.getBeansOfType(DubboEvaluationQueryClient.class)).isEmpty();
    }
}
