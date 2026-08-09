package top.egon.cola.component.ddc.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;
import top.egon.cola.component.ddc.admin.rpc.provider.DdcConfigRpcProvider;
import top.egon.cola.component.ddc.admin.rpc.provider.DdcManagementRpcProvider;
import top.egon.cola.component.ddc.admin.rpc.provider.DdcRegistryRpcProvider;
import top.egon.cola.component.ddc.admin.security.rpc.DdcHmacCredentialRegistry;
import top.egon.cola.component.ddc.admin.security.rpc.DdcRpcSecurityConfiguration;
import top.egon.cola.component.ddc.admin.service.config.DdcConfigFacade;
import top.egon.cola.component.ddc.admin.service.management.DdcManagementFacade;
import top.egon.cola.component.ddc.admin.service.registry.DdcRegistryFacade;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.rpc.config.EgonRpcAutoConfig;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.ddc.autoconfigure.DdcRpcAutoConfiguration;
import top.egon.cola.component.rpc.ddc.autoconfigure.DdcRpcProperties;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientHandle;
import top.egon.cola.component.rpc.provider.RpcProviderLifecycle;
import top.egon.cola.component.rpc.provider.RpcProviderRegistrationMode;
import top.egon.cola.component.rpc.provider.RpcProviderRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Guards the shipped {@code application.yml} against placeholders that no property source
 * can resolve.
 *
 * <p>The Actuator info version is the load-bearing case: {@code info.app.version: ${sdk.version}}
 * only resolves because {@code application.yml} declares
 * {@code spring.config.import: classpath:META-INF/egon-cola-ddc.properties}, and that file is
 * Maven-filtered in the starter module. Drop the import, rename the key, or lose the filtering
 * and the admin stops starting — with no other test in this module noticing, since they all
 * either override the property or use a {@code @WebMvcTest} slice.
 *
 * <p>Scope: this loads Boot's configuration machinery against the real YAML, not the full bean
 * graph — the admin's Redis-backed beans are mandatory and cannot start without a live Redis.
 * {@code Holder} is deliberately un-annotated so that component scanning cannot pick it up and
 * so that it is not a second {@code @SpringBootConfiguration} candidate for the other tests.
 */
@SpringBootTest(classes = DdcAdminContextSmokeTest.Holder.class)
@ActiveProfiles("test")
@Import({InfoContributorAutoConfiguration.class, InfoEndpointAutoConfiguration.class})
class DdcAdminContextSmokeTest {

    static class Holder {
    }

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private InfoEndpoint infoEndpoint;

    @Test
    void actuatorInfoVersionResolvesToAConcreteValue() {
        String version = environment.getProperty(
                "info.app.version"
        );

        assertThat(version)
                .as("Actuator info version must be supplied by the build, not left as a placeholder")
                .isNotBlank()
                .doesNotContain("${")
                .doesNotContain("@");
        assertThat(environment.getProperty("management.info.env.enabled", Boolean.class))
                .as("Actuator environment info contributor must expose info.app.version")
                .isTrue();
        assertThat(infoEndpoint.info())
                .containsEntry("app", Map.of(
                        "name", "egon-cola-ddc-admin",
                        "version", version
                ));
    }

    @Test
    void everyShippedPropertyResolves() {
        List<String> unresolved = new ArrayList<>();

        for (PropertySource<?> source : environment.getPropertySources()) {
            if (!(source instanceof EnumerablePropertySource<?> enumerable)
                    || !source.getName().contains("application")) {
                continue;
            }
            for (String key : enumerable.getPropertyNames()) {
                try {
                    String value = environment.getProperty(key);
                    if (value != null && value.contains("${")) {
                        unresolved.add(key + " -> " + value);
                    }
                } catch (IllegalArgumentException ex) {
                    unresolved.add(key + " -> " + ex.getMessage());
                }
            }
        }

        assertThat(unresolved)
                .as("shipped configuration must not contain unresolvable placeholders")
                .isEmpty();
    }

    @Test
    void adminRpcServerStartsWithoutDiscoveringOrRegisteringItself() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DdcRpcAutoConfiguration.class,
                        EgonRpcAutoConfig.class
                ))
                .withUserConfiguration(AdminRpcTestConfiguration.class)
                .withPropertyValues(
                        "spring.application.name=egon-cola-ddc-admin",
                        "egon.cola.component.ddc.enabled=false",
                        "egon.cola.component.ddc.registry.enabled=false",
                        "egon.cola.component.ddc.admin.security.local-dev=true",
                        "egon.cola.component.ddc.admin.rpc.signature-enabled=false",
                        "egon.cola.component.rpc.enabled=true",
                        "egon.cola.component.rpc.provider.enabled=true",
                        "egon.cola.component.rpc.provider.port=0",
                        "egon.cola.component.rpc.provider.registration-mode=DISABLED",
                        "egon.cola.component.rpc.consumer.enabled=false",
                        "egon.cola.component.rpc.tls.development-plaintext=true"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RpcProviderLifecycle.class);
                    assertThat(context.getBean(RpcProviderLifecycle.class)
                            .boundPort()).isPositive();
                    assertThat(context).doesNotHaveBean(RpcProviderRegistry.class);
                    assertThat(context).doesNotHaveBean(DdcConfigClient.class);
                    assertThat(context).doesNotHaveBean(
                            DdcServiceRegistryClient.class);
                    assertThat(context).doesNotHaveBean(DdcRpcClientHandle.class);
                    assertThat(context.getEnvironment().getProperty(
                            "egon.cola.component.ddc.rpc.target")).isNull();
                    EgonRpcProperties rpc = context.getBean(
                            EgonRpcProperties.class);
                    assertThat(rpc.getProvider().getRegistrationMode())
                            .isEqualTo(RpcProviderRegistrationMode.DISABLED);
                    assertThat(rpc.getConsumer().isEnabled()).isFalse();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
            DdcAdminProperties.class,
            DdcProperties.class,
            DdcRpcProperties.class
    })
    @Import(DdcRpcSecurityConfiguration.class)
    static class AdminRpcTestConfiguration {

        @Bean
        DdcHmacCredentialRegistry ddcHmacCredentialRegistry(
                DdcAdminProperties properties) {
            return new DdcHmacCredentialRegistry(properties);
        }

        @Bean
        DdcConfigFacade ddcConfigFacade() {
            return mock(DdcConfigFacade.class);
        }

        @Bean
        DdcRegistryFacade ddcRegistryFacade() {
            return mock(DdcRegistryFacade.class);
        }

        @Bean
        DdcManagementFacade ddcManagementFacade() {
            return mock(DdcManagementFacade.class);
        }

        @Bean
        DdcConfigRpcProvider ddcConfigRpcProvider(
                DdcConfigFacade facade,
                DdcProperties ddcProperties,
                DdcRpcProperties rpcProperties) {
            return new DdcConfigRpcProvider(
                    facade, ddcProperties, rpcProperties);
        }

        @Bean
        DdcRegistryRpcProvider ddcRegistryRpcProvider(
                DdcRegistryFacade facade,
                DdcRpcProperties rpcProperties) {
            return new DdcRegistryRpcProvider(facade, rpcProperties);
        }

        @Bean
        DdcManagementRpcProvider ddcManagementRpcProvider(
                DdcManagementFacade facade,
                DdcProperties ddcProperties,
                DdcRpcProperties rpcProperties) {
            return new DdcManagementRpcProvider(
                    facade, ddcProperties, rpcProperties);
        }
    }
}
