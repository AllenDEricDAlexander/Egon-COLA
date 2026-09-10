package top.egon.cola.component.yuheng.admin;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.tianshu.api.client.DdcConfigClient;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.tianshu.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.tianshu.service.registry.DdcServiceKeyFactory;
import top.egon.cola.component.tianshu.http.registration.DdcHttpRegistrationAutoConfiguration;
import top.egon.cola.component.tianshu.http.registration.DdcHttpRegistrationProperties;
import top.egon.cola.component.tianshu.http.registration.DdcHttpRegistrationRuntime;
import top.egon.cola.component.rpc.tianshu.autoconfigure.DdcRpcAutoConfiguration;
import top.egon.cola.component.rpc.tianshu.client.config.RpcDdcConfigClient;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GatewayAdminApplicationConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            DdcHttpRegistrationAutoConfiguration.class
                    ))
                    .withUserConfiguration(DdcRegistryTestConfiguration.class)
                    .withPropertyValues(
                            "egon.cola.component.tianshu.biz-code=infra",
                            "egon.cola.component.tianshu.env=local",
                            "egon.cola.component.tianshu.app-code=ga",
                            "egon.cola.component.tianshu.registration-resource-uri=https://resource.example.test/yuheng-admin",
                            "egon.cola.component.tianshu.registry.http.enabled=true",
                            "egon.cola.component.tianshu.registry.http.service-name=egon-cola-yuheng-admin",
                            "egon.cola.component.tianshu.registry.http.version=5.3.2",
                            "egon.cola.component.tianshu.registry.http.advertised-host=127.0.0.1",
                            "egon.cola.component.tianshu.registry.http.port=8080",
                            "egon.cola.component.tianshu.registry.http.metadata.yuheng.component=admin"
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
                    DdcHttpRegistrationRuntime.class
            ).size());
            DdcProperties properties = context.getBean(DdcProperties.class);
            assertEquals("infra", properties.getBizCode());
            assertEquals("local", properties.getEnv());
            assertEquals("ga", properties.getAppCode());
            DdcHttpRegistrationProperties provider = context.getBean(
                    DdcHttpRegistrationProperties.class
            );
            assertEquals("egon-cola-yuheng-admin", provider.getServiceName());
            assertEquals(
                    "admin",
                    provider.getMetadata().get("yuheng.component")
            );
        });
    }

    @Test
    void defaultsAdminResourceIdentityAndPublicationTarget() {
        YamlPropertiesFactoryBean loader = new YamlPropertiesFactoryBean();
        loader.setResources(new ClassPathResource("application.yml"));
        Properties properties = loader.getObject();

        assertEquals("${YUHENG_ADMIN_RESOURCE_SERVER_ID}", properties
                .getProperty("egon.cola.platform.tianquan.shoubing.resource-server-id"));
        assertEquals("${YUHENG_ADMIN_RESOURCE_URI}", properties
                .getProperty("egon.cola.platform.tianquan.shoubing.resource-uri"));
        assertThat(properties.stringPropertyNames())
                .noneMatch(key -> key.startsWith("egon.cola.platform.tianquan.shoubing.admission."));
        assertEquals("${YUHENG_ADMIN_RESOURCE_BIZ_CODE:xingyuan}", properties
                .getProperty("egon.cola.component.tianshu.biz-code"));
        assertEquals("${DEPLOYMENT_ENV}", properties
                .getProperty("egon.cola.component.tianshu.env"));
        assertEquals("${YUHENG_ADMIN_RESOURCE_APP_CODE:yuheng-admin}", properties
                .getProperty("egon.cola.component.tianshu.app-code"));
        assertEquals("${TIANSHU_RPC_TARGET:dns:///tianshu-admin:19080}", properties.getProperty(
                        "egon.cola.component.tianshu.rpc.target"
                ));
        assertEquals("round_robin", properties.getProperty(
                "egon.cola.component.tianshu.rpc.load-balancing-policy"
        ));
        assertThat(properties.getProperty(
                "yuheng.admin.tianshu." + "endpoint"
        )).isNull();
        assertThat(properties.getProperty(
                "egon.cola.component.tianshu.admin." + "endpoint"
        )).isNull();
        assertEquals("${YUHENG_ADMIN_TIANSHU_API_RPC_BIZ_CODE:infra}",
                properties.getProperty(
                        "yuheng.admin.tianshu.api-rpc-biz-code"
                ));
        assertEquals("${YUHENG_ADMIN_TIANSHU_API_RPC_APP_CODE:ge}",
                properties.getProperty(
                        "yuheng.admin.tianshu.api-rpc-app-code"
                ));
        assertEquals("${YUHENG_ADMIN_TIANSHU_MCP_BIZ_CODE:infra}",
                properties.getProperty("yuheng.admin.tianshu.mcp-biz-code"));
        assertEquals("${YUHENG_ADMIN_TIANSHU_MCP_APP_CODE:gme}",
                properties.getProperty("yuheng.admin.tianshu.mcp-app-code"));
    }

    @Test
    void enabledDdcUsesTheDirectRpcConfigPort() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DdcRpcAutoConfiguration.class
                ))
                .withPropertyValues(
                        "spring.application.name=yuheng-admin-test",
                        "egon.cola.component.tianshu.enabled=true",
                        "egon.cola.component.tianshu.biz-code=infra",
                        "egon.cola.component.tianshu.env=test",
                        "egon.cola.component.tianshu.app-code=ga",
                        "egon.cola.component.tianshu.rpc.target=dns:///127.0.0.1:19080",
                        "egon.cola.component.tianshu.rpc.tls.development-plaintext=true",
                        "egon.cola.component.tianshu.rpc.auth.runtime.access-key=test",
                        "egon.cola.component.tianshu.rpc.auth.runtime.secret-key=test"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(DdcConfigClient.class);
                    assertThat(context.getBean(DdcConfigClient.class))
                            .isInstanceOf(RpcDdcConfigClient.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class DdcRegistryTestConfiguration {

        @Bean
        DdcServiceRegistryClient ddcServiceRegistryClient() {
            return mock(DdcServiceRegistryClient.class);
        }

        @Bean
        IdpServiceOAuth2Client idpServiceOAuth2Client() {
            return mock(IdpServiceOAuth2Client.class);
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
