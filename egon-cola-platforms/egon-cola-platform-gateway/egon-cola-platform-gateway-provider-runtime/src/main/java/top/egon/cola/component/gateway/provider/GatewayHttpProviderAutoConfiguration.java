package top.egon.cola.component.gateway.provider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;
import top.egon.cola.component.gateway.contract.definition
        .GatewayDefinitionIdentity;

@AutoConfiguration(afterName = {
        "top.egon.cola.component.ddc.autoconfigure.DdcRegistryAutoConfiguration",
        "top.egon.cola.component.gateway.starter."
                + "GatewayReportingAutoConfiguration"
})
@EnableConfigurationProperties({
        GatewayHttpProviderProperties.class,
        DdcProperties.class
})
@ConditionalOnProperty(
        prefix = GatewayHttpProviderProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class GatewayHttpProviderAutoConfiguration {

    private static final String REPORTING_APPLICATION_CODE =
            "egon.cola.component.gateway.reporting.application-code";

    @Bean(destroyMethod = "close")
    @ConditionalOnBean(DdcServiceRegistryClient.class)
    @ConditionalOnMissingBean(HttpProviderLeaseRuntime.class)
    public HttpProviderLeaseRuntime httpProviderLeaseRuntime(
            DdcServiceRegistryClient registry,
            DdcServiceKeyFactory serviceKeyFactory,
            ObjectProvider<GatewayDefinitionIdentity> definitionIdentity,
            GatewayHttpProviderProperties properties,
            DdcProperties ddcProperties,
            ObjectProvider<DdcInstanceIdentity> ddcIdentity,
            Environment environment) {
        GatewayDefinitionIdentity identity =
                definitionIdentity.getIfAvailable();
        applyDefaults(
                properties,
                ddcProperties,
                ddcIdentity.getIfAvailable(),
                identity,
                environment
        );
        return new HttpProviderLeaseRuntime(
                registry,
                serviceKeyFactory,
                properties.toRuntime(identity, 0)
        );
    }

    @Bean
    @ConditionalOnBean(DdcServiceRegistryClient.class)
    @ConditionalOnMissingBean(name = "gatewayHttpProviderServerReadyListener")
    public ApplicationListener<WebServerInitializedEvent>
            gatewayHttpProviderServerReadyListener(
                    HttpProviderLeaseRuntime runtime) {
        return event -> {
            String namespace = event.getApplicationContext()
                    .getServerNamespace();
            if (namespace == null || namespace.isBlank()) {
                runtime.onHttpServerReady(event.getWebServer().getPort());
            }
        };
    }

    private void applyDefaults(
            GatewayHttpProviderProperties properties,
            DdcProperties ddcProperties,
            DdcInstanceIdentity ddcIdentity,
            GatewayDefinitionIdentity definitionIdentity,
            Environment environment) {
        if (blank(properties.getEnv())) {
            properties.setEnv(ddcProperties.getEnv());
        }
        if (blank(properties.getNamespace())) {
            properties.setNamespace(ddcProperties.getNamespace());
        }
        if (blank(properties.getInstanceId()) && ddcIdentity != null) {
            properties.setInstanceId(ddcIdentity.instanceId());
        }
        if (blank(properties.getServiceName())) {
            properties.setServiceName(environment.getProperty(
                    REPORTING_APPLICATION_CODE,
                    ddcProperties.getAppCode()
            ));
        }
        if (blank(properties.getVersion())) {
            properties.setVersion(definitionIdentity == null
                    ? null
                    : definitionIdentity.artifactVersion());
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnBean(DdcServiceRegistryClient.class)
    static class HealthConfiguration {

        @Bean
        @ConditionalOnMissingBean(GatewayHttpProviderHealthIndicator.class)
        GatewayHttpProviderHealthIndicator
                gatewayHttpProviderHealthIndicator(
                        HttpProviderLeaseRuntime runtime) {
            return new GatewayHttpProviderHealthIndicator(runtime);
        }
    }
}
