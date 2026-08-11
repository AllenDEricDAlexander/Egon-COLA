package top.egon.cola.component.ddc.http.registration;

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
import top.egon.cola.component.ddc.api.extension.DdcAdmissionTicketSupplier;
import top.egon.cola.component.ddc.service.registry.DdcServiceKeyFactory;

import java.util.LinkedHashMap;
import java.util.Map;

@AutoConfiguration(afterName =
        "top.egon.cola.component.ddc.autoconfigure.DdcRegistryAutoConfiguration")
@EnableConfigurationProperties({
        DdcHttpRegistrationProperties.class,
        DdcProperties.class
})
@ConditionalOnProperty(
        prefix = DdcHttpRegistrationProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class DdcHttpRegistrationAutoConfiguration {

    /**
     * 创建在 Web Server Ready 后执行准入注册的运行时。
     * / Creates the runtime that performs admitted registration after the web server is ready.
     *
     * @param registry DDC 服务注册客户端 / DDC service-registry client
     * @param serviceKeyFactory 服务键工厂 / service-key factory
     * @param contributors HTTP 注册信息贡献器 / HTTP registration contributors
     * @param properties HTTP 注册配置 / HTTP registration configuration
     * @param ddcProperties DDC 物理作用域配置 / DDC physical-scope configuration
     * @param ddcIdentity 可选 DDC 实例身份 / optional DDC instance identity
     * @param environment Spring 环境 / Spring environment
     * @param admissionTickets 准入票据端口 / admission-ticket port
     * @return HTTP 注册运行时 / HTTP registration runtime
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnBean(DdcServiceRegistryClient.class)
    @ConditionalOnMissingBean(DdcHttpRegistrationRuntime.class)
    public DdcHttpRegistrationRuntime ddcHttpRegistrationRuntime(
            DdcServiceRegistryClient registry,
            DdcServiceKeyFactory serviceKeyFactory,
            ObjectProvider<DdcHttpRegistrationContributor> contributors,
            DdcHttpRegistrationProperties properties,
            DdcProperties ddcProperties,
            ObjectProvider<DdcInstanceIdentity> ddcIdentity,
            Environment environment,
            DdcAdmissionTicketSupplier admissionTickets) {
        RegistrationContribution contribution = mergeContributions(
                contributors
        );
        applyDefaults(
                properties,
                ddcProperties,
                ddcIdentity.getIfAvailable(),
                contribution.serviceVersion(),
                environment
        );
        return new DdcHttpRegistrationRuntime(
                registry,
                serviceKeyFactory,
                properties.toRuntime(
                        contribution.serviceVersion(),
                        contribution.metadata(),
                        0
                ),
                admissionTickets
        );
    }

    @Bean
    @ConditionalOnBean(DdcServiceRegistryClient.class)
    @ConditionalOnMissingBean(name = "ddcHttpRegistrationServerReadyListener")
    public ApplicationListener<WebServerInitializedEvent>
            ddcHttpRegistrationServerReadyListener(
                    DdcHttpRegistrationRuntime runtime) {
        return event -> {
            String namespace = event.getApplicationContext()
                    .getServerNamespace();
            if (namespace == null || namespace.isBlank()) {
                runtime.onHttpServerReady(event.getWebServer().getPort());
            }
        };
    }

    private void applyDefaults(
            DdcHttpRegistrationProperties properties,
            DdcProperties ddcProperties,
            DdcInstanceIdentity ddcIdentity,
            String contributedVersion,
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
                    "spring.application.name",
                    ddcProperties.getAppCode()
            ));
        }
        if (blank(properties.getVersion())) {
            properties.setVersion(contributedVersion);
        }
    }

    private RegistrationContribution mergeContributions(
            ObjectProvider<DdcHttpRegistrationContributor> contributors) {
        String version = null;
        LinkedHashMap<String, String> metadata = new LinkedHashMap<>();
        for (DdcHttpRegistrationContributor contributor
                : contributors.orderedStream().toList()) {
            String contributedVersion = contributor.serviceVersion();
            if (!blank(contributedVersion)) {
                String normalizedVersion = contributedVersion.trim();
                if (version != null && !version.equals(normalizedVersion)) {
                    throw new IllegalArgumentException(
                            "HTTP registration contributors disagree on version"
                    );
                }
                version = normalizedVersion;
            }
            Map<String, String> contributedMetadata = contributor.metadata();
            if (contributedMetadata == null) {
                continue;
            }
            contributedMetadata.forEach((key, value) -> {
                String previous = metadata.putIfAbsent(key, value);
                if (previous != null && !previous.equals(value)) {
                    throw new IllegalArgumentException(
                            "HTTP registration contributors disagree on metadata: "
                                    + key
                    );
                }
            });
        }
        return new RegistrationContribution(version, Map.copyOf(metadata));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnBean(DdcServiceRegistryClient.class)
    static class HealthConfiguration {

        @Bean
        @ConditionalOnMissingBean(DdcHttpRegistrationHealthIndicator.class)
        DdcHttpRegistrationHealthIndicator
                ddcHttpRegistrationHealthIndicator(
                        DdcHttpRegistrationRuntime runtime) {
            return new DdcHttpRegistrationHealthIndicator(runtime);
        }
    }

    private record RegistrationContribution(
            String serviceVersion,
            Map<String, String> metadata) {
    }
}
