package top.egon.cola.component.gateway.engine;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.env.MapPropertySource;
import top.egon.cola.component.gateway.engine.http.GatewayHttpEngineProperties;
import top.egon.cola.component.gateway.engine.traffic.RedisTokenBucketExecutor;
import top.egon.cola.component.gateway.engine.traffic.RedissonRedisTokenBucketExecutor;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayEngineConfigurationTest {

    private static final long MIB = 1024L * 1024L;

    @Test
    void configuresEngineDdcIdentityAsInfraLocalGe() {
        YamlPropertiesFactoryBean loader = new YamlPropertiesFactoryBean();
        loader.setResources(new ClassPathResource("application.yml"));

        assertEquals("${DDC_BIZ_CODE:infra}", loader.getObject()
                .getProperty("egon.cola.component.ddc.biz-code"));
        assertEquals("${DDC_ENV:local}", loader.getObject()
                .getProperty("egon.cola.component.ddc.env"));
        assertEquals("${DDC_APP_CODE:ge}", loader.getObject()
                .getProperty("egon.cola.component.ddc.app-code"));
    }

    @Test
    void bindsLegacyUpstreamTimeoutAlongsideIndependentSafetyDefaults() {
        GatewayEngineRuntimeProperties properties = new Binder(
                new MapConfigurationPropertySource(Map.of(
                        "egon.cola.component.gateway.engine.http.upstream-timeout",
                        "PT7S"
                ))
        ).bind(
                "egon.cola.component.gateway.engine",
                Bindable.of(GatewayEngineRuntimeProperties.class)
        ).get();

        assertEquals(Duration.ofSeconds(7),
                properties.getHttp().getUpstreamTimeout());
        assertEquals(2L * MIB, properties.getHttp().getMaxBodyBytes());
        assertEquals(1024L * MIB,
                properties.getHttp().getAbsoluteMaxRequestBodyBytes());
        assertEquals(8 * 1024,
                properties.getHttp().getBodyLogSampleBytes());
        assertEquals(64 * 1024,
                properties.getHttp().getAbsoluteMaxBodyLogSampleBytes());
        assertEquals(Duration.ofSeconds(60),
                properties.getHttp().getMaxConnectTimeout());
        assertEquals(Duration.ofMinutes(10),
                properties.getHttp().getMaxResponseHeaderTimeout());
        assertEquals(Duration.ofMinutes(30),
                properties.getHttp().getMaxStreamIdleTimeout());
        assertEquals(Duration.ofHours(2),
                properties.getHttp().getMaxTotalTimeout());
        assertEquals(Duration.ofHours(2),
                properties.getHttp().getMaxWebsocketIdleTimeout());
        assertEquals(64L * MIB,
                properties.getHttp().getMaxWebsocketFrameBytes());
    }

    @Test
    void buildsCompilerDefaultsWithLegacyUpstreamTimeoutSemantics() {
        GatewayEngineRuntimeProperties properties =
                new GatewayEngineRuntimeProperties();
        enableDevelopmentPlaintext(properties);
        properties.getHttp().setMaxBodyBytes(3L * MIB);
        properties.getHttp().setUpstreamTimeout(Duration.ofSeconds(7));
        GatewayEngineConfiguration configuration =
                new GatewayEngineConfiguration();

        var defaults = configuration.gatewayTransportDefaults(properties);
        var safety = configuration.gatewayTransportSafetyLimits(
                configuration.gatewayHttpEngineProperties(properties)
        );

        assertEquals(3L * MIB, defaults.maxRequestBodyBytes());
        assertEquals(4L * MIB,
                defaults.maxResponseBodyBytes().orElseThrow());
        assertEquals(Duration.ofSeconds(30), defaults.connectTimeout());
        assertEquals(Duration.ofSeconds(7),
                defaults.responseHeaderTimeout());
        assertEquals(Duration.ofSeconds(7), defaults.streamIdleTimeout());
        assertTrue(defaults.totalTimeout().isEmpty());
        assertEquals(1024L * MIB, safety.maxRequestBodyBytes());
    }

    @Test
    void rejectsDefaultRequestLimitAboveNodeAbsoluteLimit() {
        GatewayEngineRuntimeProperties properties =
                new GatewayEngineRuntimeProperties();
        enableDevelopmentPlaintext(properties);
        properties.getHttp().setAbsoluteMaxRequestBodyBytes(MIB);

        assertThrows(
                IllegalArgumentException.class,
                () -> new GatewayEngineConfiguration()
                        .gatewayHttpEngineProperties(properties)
        );
    }

    @Test
    void boundLegacyAggregatedLimitStillRejectsSixtyFiveMib() {
        GatewayEngineRuntimeProperties properties = new Binder(
                new MapConfigurationPropertySource(Map.of(
                        "egon.cola.component.gateway.engine.http.max-body-bytes",
                        Long.toString(65L * MIB),
                        "egon.cola.component.gateway.engine.http.absolute-max-request-body-bytes",
                        Long.toString(1024L * MIB)
                ))
        ).bind(
                "egon.cola.component.gateway.engine",
                Bindable.of(GatewayEngineRuntimeProperties.class)
        ).get();
        enableDevelopmentPlaintext(properties);

        assertThrows(
                IllegalArgumentException.class,
                () -> new GatewayEngineConfiguration()
                        .gatewayHttpEngineProperties(properties)
        );
    }

    @Test
    void legacyHttpEnginePropertiesConstructorRejectsSixtyFiveMib() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GatewayHttpEngineProperties(
                        new GatewayHttpEngineProperties.Listener(
                                true,
                                "127.0.0.1",
                                0
                        ),
                        new GatewayHttpEngineProperties.Listener(
                                false,
                                "127.0.0.1",
                                0
                        ),
                        128,
                        64 * 1024,
                        65L * MIB,
                        Duration.ofSeconds(30),
                        Duration.ofSeconds(10),
                        512,
                        1024
                )
        );
    }

    @Test
    void legacyHttpEnginePropertiesConstructorKeepsNewSafetyDefaults() {
        GatewayHttpEngineProperties properties =
                new GatewayHttpEngineProperties(
                        new GatewayHttpEngineProperties.Listener(
                                true,
                                "127.0.0.1",
                                0
                        ),
                        new GatewayHttpEngineProperties.Listener(
                                false,
                                "127.0.0.1",
                                0
                        ),
                        128,
                        64 * 1024,
                        2L * MIB,
                        Duration.ofSeconds(30),
                        Duration.ofSeconds(10),
                        512,
                        1024
                );

        assertEquals(1024L * MIB,
                properties.absoluteMaxRequestBodyBytes());
        assertEquals(8 * 1024, properties.bodyLogSampleBytes());
        assertEquals(64 * 1024,
                properties.absoluteMaxBodyLogSampleBytes());
    }

    @Test
    void qualifiesCompositeCompletionListenerAtDataPlaneInjectionPoints() {
        for (String methodName : List.of(
                "gatewayHttpServer",
                "gatewayRpcHandlerRegistry"
        )) {
            var method = Arrays.stream(
                            GatewayEngineConfiguration.class
                                    .getDeclaredMethods()
                    )
                    .filter(candidate -> candidate.getName().equals(
                            methodName
                    ))
                    .findFirst()
                    .orElseThrow();
            var parameter = Arrays.stream(method.getParameters())
                    .filter(candidate -> candidate.getType().equals(
                            top.egon.cola.component.gateway.engine
                                    .observability
                                    .GatewayCallCompletionListener.class
                    ))
                    .findFirst()
                    .orElseThrow();
            Qualifier qualifier = parameter.getAnnotation(Qualifier.class);

            assertNotNull(qualifier);
            assertEquals("gatewayCallCompletionListener", qualifier.value());
        }
    }

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

    @Test
    void doesNotCreateRateLimitExecutorFromUnrelatedApplicationClient() {
        try (AnnotationConfigApplicationContext context = context(
                Map.of("applicationRedissonClient", redissonClient()),
                true,
                GatewayEngineConfigurationWithoutRateLimitClient.class
        )) {
            assertFalse(context.containsBean("gatewayRedisTokenBucketExecutor"));
            assertFalse(context.getBeanProvider(RedisTokenBucketExecutor.class)
                    .iterator().hasNext());
        }
    }

    private AnnotationConfigApplicationContext context(
            String redissonClientName,
            RedissonClient redissonClient) {
        return context(Map.of(redissonClientName, redissonClient), false);
    }

    private void enableDevelopmentPlaintext(
            GatewayEngineRuntimeProperties properties) {
        properties.getHttp().getPublicTls().setDevelopmentPlaintext(true);
        properties.getHttp().getInternalTls().setDevelopmentPlaintext(true);
    }

    private AnnotationConfigApplicationContext context(
            Map<String, RedissonClient> redissonClients,
            boolean preserveRateLimitExecutor) {
        return context(
                redissonClients,
                preserveRateLimitExecutor,
                GatewayEngineConfiguration.class
        );
    }

    private AnnotationConfigApplicationContext context(
            Map<String, RedissonClient> redissonClients,
            boolean preserveRateLimitExecutor,
            Class<?> configurationType) {
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
        context.register(configurationType);
        context.addBeanFactoryPostProcessor(beanFactory -> {
            for (String beanName : beanFactory.getBeanDefinitionNames()) {
                if (beanName.startsWith("gateway")
                        && !beanName.equals("gatewayRateLimitRedissonClient")
                        && !beanName.equals("gatewayEngineConfiguration")
                        && !beanName.contains("GatewayEngineConfiguration")
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

    @Configuration(proxyBeanMethods = false)
    static class GatewayEngineConfigurationWithoutRateLimitClient
            extends GatewayEngineConfiguration {

        @Override
        @Bean(name = "gatewayRateLimitRedissonClient")
        @ConditionalOnProperty(
                prefix = "gateway.test",
                name = "rate-limit-client-producer",
                havingValue = "true"
        )
        public RedissonClient gatewayRateLimitRedissonClient(
                String address,
                int database,
                String password) {
            return null;
        }
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
