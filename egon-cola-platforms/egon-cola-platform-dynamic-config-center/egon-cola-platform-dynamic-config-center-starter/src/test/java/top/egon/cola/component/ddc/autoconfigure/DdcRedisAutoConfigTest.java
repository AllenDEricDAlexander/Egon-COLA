package top.egon.cola.component.ddc.autoconfigure;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

class DdcRedisAutoConfigTest {

    @Test
    void createsOneSharedClientWhenConfigurationAndRegistryAreEnabled() {
        RedissonClient client = mock(RedissonClient.class);

        try (MockedStatic<Redisson> redisson = mockStatic(Redisson.class)) {
            redisson.when(() -> Redisson.create(any(Config.class)))
                    .thenReturn(client);
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(DdcRedisAutoConfig.class))
                    .withPropertyValues(
                            "egon.cola.component.ddc.enabled=true",
                            "egon.cola.component.ddc.redis.enabled=true",
                            "egon.cola.component.ddc.registry.enabled=true"
                    )
                    .run(context -> {
                        assertThat(context).hasSingleBean(RedissonClient.class);
                        assertThat(context.getBean("ddcRedissonClient"))
                                .isSameAs(client);
                    });

            redisson.verify(
                    () -> Redisson.create(any(Config.class)),
                    times(1)
            );
        }
    }
}
