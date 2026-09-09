package top.egon.cola.component.yuheng.engine;

import top.egon.cola.component.yuheng.engine.bootstrap.config.GatewayEngineConfiguration;
import top.egon.cola.component.yuheng.engine.common.config.GatewayEngineRuntimeProperties;
import top.egon.cola.component.yuheng.engine.rule.service.ApiRpcGatewayRuleCompilerStrategy;
import top.egon.cola.component.yuheng.core.transport.GatewayTransportDefaults;
import top.egon.cola.component.yuheng.core.transport.GatewayTransportSafetyLimits;
import top.egon.cola.component.yuheng.runtime.security.service.GatewaySecurityPolicyCompiler;
import top.egon.cola.component.yuheng.runtime.observability.service.GatewayCallCompletionListener;

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
import top.egon.cola.component.yuheng.runtime.http.domain.GatewayHttpEngineProperties;
import top.egon.cola.component.yuheng.runtime.traffic.service.RedisTokenBucketExecutor;
import top.egon.cola.component.yuheng.runtime.traffic.adapter.RedissonRedisTokenBucketExecutor;

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

    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path dataDirectory;

    @Test
    void startsApiRpcOnlyContextWithoutMcpOrDatasource() {
        var identity = org.mockito.Mockito.mock(
                top.egon.cola.component.tianshu.model.instance.DdcInstanceIdentity.class);
        org.mockito.Mockito.when(identity.instanceId()).thenReturn("api-test");
        String prefix = "egon.cola.component.yuheng.engine.";
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(GatewayEngineConfiguration.class)
                .withPropertyValues(prefix + "data-directory=" + dataDirectory,
                        prefix + "http.public-enabled=true", prefix + "http.public-port=0",
                        prefix + "http.public-host=127.0.0.1", prefix + "http.internal-enabled=false",
                        prefix + "http.public-tls.development-plaintext=true",
                        prefix + "http.internal-tls.development-plaintext=true",
                        prefix + "rpc.enabled=false", prefix + "rpc.tls.development-plaintext=true")
                .withBean("objectMapper", com.fasterxml.jackson.databind.ObjectMapper.class,
                        () -> new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules())
                .withBean("meterRegistry", io.micrometer.core.instrument.simple.SimpleMeterRegistry.class,
                        io.micrometer.core.instrument.simple.SimpleMeterRegistry::new)
                .withBean("observationRegistry", io.micrometer.observation.ObservationRegistry.class,
                        io.micrometer.observation.ObservationRegistry::create)
                .withBean("ddcServiceRegistryClient", top.egon.cola.component.tianshu.api.client.DdcServiceRegistryClient.class,
                        () -> org.mockito.Mockito.mock(top.egon.cola.component.tianshu.api.client.DdcServiceRegistryClient.class))
                .withBean("ddcConfigApplierRegistry", top.egon.cola.component.tianshu.api.refresh.DdcConfigApplierRegistry.class,
                        () -> org.mockito.Mockito.mock(top.egon.cola.component.tianshu.api.refresh.DdcConfigApplierRegistry.class))
                .withBean("ddcServiceKeyFactory", top.egon.cola.component.tianshu.service.registry.DdcServiceKeyFactory.class,
                        () -> org.mockito.Mockito.mock(top.egon.cola.component.tianshu.service.registry.DdcServiceKeyFactory.class))
                .withBean("ddcInstanceIdentity", top.egon.cola.component.tianshu.model.instance.DdcInstanceIdentity.class, () -> identity)
                .withBean("idpServiceOAuth2Client", top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client.class,
                        () -> org.mockito.Mockito.mock(top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client.class))
                .withBean("idpStarterProperties", top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties.class,
                        top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties::new)
                .run(context -> {
                    assertEquals(null, context.getStartupFailure());
                    assertTrue(context.containsBean("gatewayHttpServer"));
                    assertTrue(context.containsBean("gatewayRpcServer"));
                    assertTrue(context.containsBean("gatewayRpcSlotRuntime"));
                    assertSame(context.getBean("gatewayEngineRuntime"), context.getBean("apiRpcGatewayEngineRuntime"));
                    assertSame(context.getBean("gatewayRuleCompilerStrategy"), context.getBean("apiRpcGatewayRuleCompilerStrategy"));
                    assertTrue(context.getBeansOfType(javax.sql.DataSource.class).isEmpty());
                    assertFalse(context.containsBean("gatewayMcpHttpHandler"));
                    assertFalse(context.containsBean("gatewayMcpTaskService"));
                    var metadata = context.getBean("gatewayRuntimeMetadata",
                            top.egon.cola.component.tianshu.api.extension.DdcInstanceMetadataContributor.class).metadata();
                    assertEquals("API_RPC", metadata.get("yuheng.engine.role"));
                });
    }

    @Test
    void profilesHaveIdenticalApiOnlyKeys() {
        var base = new YamlPropertiesFactoryBean();
        base.setResources(new ClassPathResource("application.yml"));
        var operations = new YamlPropertiesFactoryBean();
        operations.setResources(new ClassPathResource("application-operations.yml"));
        assertEquals(base.getObject().stringPropertyNames(), operations.getObject().stringPropertyNames());
        assertFalse(base.getObject().stringPropertyNames().stream().anyMatch(
                key -> key.contains("yuheng.engine.mcp.") || key.startsWith("spring.datasource.")));
        assertFalse(base.getObject().values().stream().anyMatch(value -> value.toString().contains("YUHENG_MCP_")));
    }

    @Test
    void exposesOnlyApiRpcBeanOwnership() {
        assertTrue(Arrays.stream(GatewayEngineConfiguration.class.getDeclaredMethods())
                .noneMatch(method -> method.getReturnType().getName().contains(".mcp.")),
                "API_RPC configuration must not own MCP runtime beans");
        assertThrows(ClassNotFoundException.class, () -> Class.forName(
                "top.egon.cola.component.yuheng.mcp.rule.domain.CompiledMcpRules"));
    }

    private static final long MIB = 1024L * 1024L;

    @Test
    void compilerStrategyHasStableBeanNamesAndQualifiedConstruction() throws Exception {
        var factory = GatewayEngineConfiguration.class.getMethod("gatewayRuleCompilerStrategy",
                GatewaySecurityPolicyCompiler.class, GatewayTransportDefaults.class,
                GatewayTransportSafetyLimits.class);
        assertEquals(List.of("gatewayRuleCompilerStrategy", "apiRpcGatewayRuleCompilerStrategy"),
                Arrays.asList(factory.getAnnotation(Bean.class).name()));
        var constructor = ApiRpcGatewayRuleCompilerStrategy.class.getConstructor(
                GatewaySecurityPolicyCompiler.class, GatewayTransportDefaults.class,
                GatewayTransportSafetyLimits.class);
        List<String> qualifiers = List.of("gatewaySecurityPolicyCompiler",
                "gatewayTransportDefaults", "gatewayTransportSafetyLimits");
        assertEquals(qualifiers, Arrays.stream(factory.getParameters())
                .map(parameter -> parameter.getAnnotation(Qualifier.class).value()).toList());
        assertEquals(qualifiers, Arrays.stream(constructor.getParameters())
                .map(parameter -> parameter.getAnnotation(Qualifier.class).value()).toList());
    }

    @Test
    void configuresEngineDdcIdentityAsInfraLocalGe() {
        YamlPropertiesFactoryBean loader = new YamlPropertiesFactoryBean();
        loader.setResources(new ClassPathResource("application.yml"));

        assertEquals("${TIANSHU_BIZ_CODE:infra}", loader.getObject()
                .getProperty("egon.cola.component.tianshu.biz-code"));
        assertEquals("${TIANSHU_ENV:local}", loader.getObject()
                .getProperty("egon.cola.component.tianshu.env"));
        assertEquals("${TIANSHU_APP_CODE:ge}", loader.getObject()
                .getProperty("egon.cola.component.tianshu.app-code"));
        assertEquals("${TIANSHU_RPC_TARGET:dns:///tianshu-admin:19080}",
                loader.getObject().getProperty(
                        "egon.cola.component.tianshu.rpc.target"
                ));
        assertEquals("round_robin", loader.getObject().getProperty(
                "egon.cola.component.tianshu.rpc.load-balancing-policy"
        ));
        assertEquals("${YUHENG_ENGINE_TIANSHU_REGISTRATION_ENABLED:true}",
                loader.getObject().getProperty(
                        "egon.cola.component.tianshu.registry.http.enabled"));
        assertEquals("egon-cola-yuheng-biz-gateway", loader.getObject()
                .getProperty(
                        "egon.cola.component.tianshu.registry.http.service-name"));
        assertEquals("engine", loader.getObject().getProperty(
                "egon.cola.component.tianshu.registry.http.metadata.yuheng.component"));
    }

    @Test
    void bindsLegacyUpstreamTimeoutAlongsideIndependentSafetyDefaults() {
        GatewayEngineRuntimeProperties properties = new Binder(
                new MapConfigurationPropertySource(Map.of(
                        "egon.cola.component.yuheng.engine.http.upstream-timeout",
                        "PT7S"
                ))
        ).bind(
                "egon.cola.component.yuheng.engine",
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
                        "egon.cola.component.yuheng.engine.http.max-body-bytes",
                        Long.toString(65L * MIB),
                        "egon.cola.component.yuheng.engine.http.absolute-max-request-body-bytes",
                        Long.toString(1024L * MIB)
                ))
        ).bind(
                "egon.cola.component.yuheng.engine",
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
                            top.egon.cola.component.yuheng.runtime
                                    .observability.service
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
                new MapPropertySource("yuheng-rate-limit-test", Map.of(
                        "egon.cola.component.yuheng.engine.traffic.redis.enabled",
                        "true",
                        "egon.cola.component.yuheng.engine.traffic.redis.address",
                        "redis://127.0.0.1:6379"
                ))
        );
        redissonClients.forEach(context.getBeanFactory()::registerSingleton);
        context.register(configurationType);
        context.addBeanFactoryPostProcessor(beanFactory -> {
            for (String beanName : beanFactory.getBeanDefinitionNames()) {
                if ((beanName.startsWith("yuheng") || beanName.startsWith("apiRpcGateway"))
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
                prefix = "yuheng.test",
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
