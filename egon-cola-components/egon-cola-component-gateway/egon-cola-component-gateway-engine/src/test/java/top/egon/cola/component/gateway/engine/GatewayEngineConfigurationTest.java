package top.egon.cola.component.gateway.engine;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import top.egon.cola.component.gateway.engine.traffic.RedisTokenBucketExecutor;
import top.egon.cola.component.gateway.engine.traffic.RedissonRedisTokenBucketExecutor;

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

    @Test
    void createsRateLimitExecutorFromExactClientAlongsideApplicationClient() {
        RedissonClient dedicatedClient = redissonClient();
        RedissonClient applicationClient = redissonClient();

        try (AnnotationConfigApplicationContext context = context(
                Map.of(
                        "gatewayRateLimitRedissonClient", dedicatedClient,
                        "applicationRedissonClient", applicationClient
                ),
                true
        )) {
            assertTrue(context.containsBean("gatewayRedisTokenBucketExecutor"));
            assertTrue(context.getBean(RedisTokenBucketExecutor.class)
                    instanceof RedissonRedisTokenBucketExecutor);
        }
    }

    private AnnotationConfigApplicationContext context(
            String redissonClientName,
            RedissonClient redissonClient) {
        return context(Map.of(redissonClientName, redissonClient), false);
    }

    private AnnotationConfigApplicationContext context(
            Map<String, RedissonClient> redissonClients,
            boolean preserveRateLimitExecutor) {
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
        redissonClients.forEach(context.getBeanFactory()::registerSingleton);
        context.register(GatewayEngineConfiguration.class);
        context.addBeanFactoryPostProcessor(beanFactory -> {
            for (String beanName : beanFactory.getBeanDefinitionNames()) {
                if (beanName.startsWith("gateway")
                        && !beanName.equals("gatewayRateLimitRedissonClient")
                        && !beanName.equals("gatewayEngineConfiguration")
                        && (!preserveRateLimitExecutor
                        || !beanName.equals("gatewayRedisTokenBucketExecutor"))) {
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
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "equals" -> proxy == arguments[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "RedissonClient test double";
                    default -> null;
                }
        );
    }
}
