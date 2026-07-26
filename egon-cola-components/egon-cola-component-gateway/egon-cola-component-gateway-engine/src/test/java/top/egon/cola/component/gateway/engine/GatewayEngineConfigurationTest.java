package top.egon.cola.component.gateway.engine;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayEngineConfigurationTest {

    @Test
    void createsDedicatedRateLimitClientWhenApplicationClientExists() {
        RedissonClient applicationClient = redissonClient();

        try (AnnotationConfigApplicationContext context = context(
                "applicationRedissonClient",
                applicationClient
        )) {
            assertTrue(context.containsBeanDefinition(
                    "gatewayRateLimitRedissonClient"
            ));
            assertSame(
                    applicationClient,
                    context.getBean("applicationRedissonClient")
            );
        }
    }

    @Test
    void retainsUserProvidedRateLimitClient() {
        RedissonClient dedicatedClient = redissonClient();

        try (AnnotationConfigApplicationContext context = context(
                "gatewayRateLimitRedissonClient",
                dedicatedClient
        )) {
            assertFalse(context.containsBeanDefinition(
                    "gatewayRateLimitRedissonClient"
            ));
            assertSame(
                    dedicatedClient,
                    context.getBean("gatewayRateLimitRedissonClient")
            );
        }
    }

    private AnnotationConfigApplicationContext context(
            String redissonClientName,
            RedissonClient redissonClient) {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("gateway-rate-limit-test", Map.of(
                        "egon.cola.component.gateway.engine.traffic.redis.enabled",
                        "true",
                        "egon.cola.component.gateway.engine.traffic.redis.address",
                        "redis://127.0.0.1:6379"
                ))
        );
        context.getBeanFactory().registerSingleton(
                redissonClientName,
                redissonClient
        );
        context.register(GatewayEngineConfiguration.class);
        context.addBeanFactoryPostProcessor(beanFactory -> {
            for (String beanName : beanFactory.getBeanDefinitionNames()) {
                if (beanName.startsWith("gateway")
                        && !beanName.equals("gatewayRateLimitRedissonClient")) {
                    ((DefaultListableBeanFactory) beanFactory)
                            .removeBeanDefinition(beanName);
                }
            }
            if (beanFactory.containsBeanDefinition(
                    "gatewayRateLimitRedissonClient"
            )) {
                beanFactory.getBeanDefinition("gatewayRateLimitRedissonClient")
                        .setLazyInit(true);
            }
        });
        context.refresh();
        return context;
    }

    private RedissonClient redissonClient() {
        return (RedissonClient) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{RedissonClient.class},
                (proxy, method, arguments) -> null
        );
    }
}
