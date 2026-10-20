package top.egon.cola.component.yaoguang.admin.architecture;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import top.egon.cola.component.yaoguang.admin.bootstrap.YaoguangAdminConfiguration;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KnowledgeHostConfigurationTest {
    @Test
    void independentHostEnablesWorkersAndReusesTheIdentityRedisConnection() {
        RedissonClient redis = mock(RedissonClient.class);
        new ApplicationContextRunner().withUserConfiguration(YaoguangAdminConfiguration.class)
                .withBean("rbac3RuntimeRedissonClient", RedissonClient.class, () -> redis)
                .run(context -> {
                    assertThat(context).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class);
                    assertThat(context.getBean("redissonClient")).isSameAs(redis);
                    assertThat(context.getBean("jacksonObjectMapper", ObjectMapper.class)
                            .writeValueAsString(Instant.parse("2026-10-20T00:00:00Z")))
                            .isEqualTo("\"2026-10-20T00:00:00Z\"");
                });
    }
}
