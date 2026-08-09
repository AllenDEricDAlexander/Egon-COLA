package top.egon.cola.component.gateway.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.ddc.autoconfigure.DdcProperties;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;
import top.egon.cola.component.gateway.provider.GatewayHttpProviderAutoConfiguration;
import top.egon.cola.component.gateway.provider.GatewayHttpProviderProperties;
import top.egon.cola.component.gateway.provider.HttpProviderLeaseRuntime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GatewayAdminApplicationConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            GatewayHttpProviderAutoConfiguration.class
                    ))
                    .withUserConfiguration(DdcRegistryTestConfiguration.class)
                    .withPropertyValues(
                            "egon.cola.component.ddc.biz-code=infra",
                            "egon.cola.component.ddc.env=local",
                            "egon.cola.component.ddc.app-code=ga",
                            "egon.cola.component.gateway.provider.http.enabled=true",
                            "egon.cola.component.gateway.provider.http.service-name=egon-cola-gateway-admin",
                            "egon.cola.component.gateway.provider.http.version=5.3.2",
                            "egon.cola.component.gateway.provider.http.advertised-host=127.0.0.1",
                            "egon.cola.component.gateway.provider.http.port=8080",
                            "egon.cola.component.gateway.provider.http.metadata.gateway.component=admin"
                    );

    @Test
    void excludesGenericRedissonSpringDataAutoConfiguration() {
        YamlPropertiesFactoryBean loader = new YamlPropertiesFactoryBean();
        loader.setResources(new ClassPathResource("application.yml"));

        assertEquals(
                "org.redisson.spring.starter.RedissonAutoConfigurationV2",
                loader.getObject().getProperty(
                        "spring.autoconfigure.exclude[0]"
                )
        );
    }

    @Test
    void registersGatewayAdminAsInfraGaHttpProvider() {
        contextRunner.run(context -> {
            assertEquals(1, context.getBeansOfType(
                    HttpProviderLeaseRuntime.class
            ).size());
            DdcProperties properties = context.getBean(DdcProperties.class);
            assertEquals("infra", properties.getBizCode());
            assertEquals("local", properties.getEnv());
            assertEquals("ga", properties.getAppCode());
            GatewayHttpProviderProperties provider = context.getBean(
                    GatewayHttpProviderProperties.class
            );
            assertEquals("egon-cola-gateway-admin", provider.getServiceName());
            assertEquals(
                    "admin",
                    provider.getMetadata().get("gateway.component")
            );
        });
    }

    @Test
    void defaultsAdminAndPublicationToInfraGaAndInfraGe() {
        YamlPropertiesFactoryBean loader = new YamlPropertiesFactoryBean();
        loader.setResources(new ClassPathResource("application.yml"));

        assertEquals("${DDC_BIZ_CODE:infra}", loader.getObject()
                .getProperty("egon.cola.component.ddc.biz-code"));
        assertEquals("${DDC_ENV:local}", loader.getObject()
                .getProperty("egon.cola.component.ddc.env"));
        assertEquals("${DDC_APP_CODE:ga}", loader.getObject()
                .getProperty("egon.cola.component.ddc.app-code"));
        assertEquals("${GATEWAY_ADMIN_DDC_TARGET_BIZ_CODE:infra}",
                loader.getObject().getProperty(
                        "gateway.admin.ddc.target-biz-code"
                ));
        assertEquals("${GATEWAY_ADMIN_DDC_TARGET_APP_CODE:ge}",
                loader.getObject().getProperty(
                        "gateway.admin.ddc.target-app-code"
                ));
    }

    @Configuration(proxyBeanMethods = false)
    static class DdcRegistryTestConfiguration {

        @Bean
        DdcServiceRegistryClient ddcServiceRegistryClient() {
            return mock(DdcServiceRegistryClient.class);
        }

        @Bean
        DdcServiceKeyFactory ddcServiceKeyFactory(
                DdcProperties properties) {
            return new DdcServiceKeyFactory(properties);
        }

        @Bean
        DdcInstanceIdentity ddcInstanceIdentity() {
            return new DdcInstanceIdentity(
                    "admin-instance",
                    "infra",
                    "ga",
                    "local",
                    "127.0.0.1",
                    8080,
                    "1",
                    "5.3.2"
            );
        }
    }
}
