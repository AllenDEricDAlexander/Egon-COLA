package top.egon.cola.component.rpc.ddc.autoconfigure;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.api.extension.DdcAdmissionTicketSupplier;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionTicket;
import top.egon.cola.component.ddc.autoconfigure.DdcAutoConfiguration;
import top.egon.cola.component.ddc.autoconfigure.DdcRedisAutoConfiguration;
import top.egon.cola.component.ddc.autoconfigure.DdcRegistryAutoConfiguration;
import top.egon.cola.component.ddc.service.registry.DdcRegistrySnapshotLoader;
import top.egon.cola.component.rpc.consumer.gateway.RpcGatewayDirectory;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentityProvider;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.ddc.client.DdcRpcClientHandle;
import top.egon.cola.component.rpc.provider.registration.RpcProviderRegistry;

import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DdcRpcAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DdcRpcAutoConfiguration.class))
            .withBean(DdcAdmissionTicketSupplier.class, this::admissionTickets);

    private DdcAdmissionTicketSupplier admissionTickets() {
        return (bizCode, appCode, environment, instanceId) ->
                new DdcAdmissionTicket(
                        "test-admission-ticket",
                        Instant.parse("2099-01-01T00:00:00Z"),
                        "resource-test",
                        URI.create("urn:egon:resource:test"),
                        1L,
                        bizCode,
                        appCode,
                        environment,
                        instanceId,
                        "kid-test"
                );
    }

    @Test
    void disabledRuntimeAndRegistryCreateNoDirectClientAndRequireNoTarget() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(DdcRpcClientFactory.class);
            assertThat(context).doesNotHaveBean(DdcConfigClient.class);
            assertThat(context).doesNotHaveBean(DdcRpcClientHandle.class);
            assertThat(context.getStartupFailure()).isNull();
        });
    }

    @Test
    void enabledRuntimeFailsFastWhenDirectTargetIsMissing() {
        runner.withPropertyValues(
                "egon.cola.component.ddc.enabled=true",
                "egon.cola.component.ddc.rpc.auth.runtime.access-key=runtime-ak",
                "egon.cola.component.ddc.rpc.auth.runtime.secret-key=runtime-sk"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseMessage(
                            "egon.cola.component.ddc.rpc.target is required");
        });
    }

    @Test
    void applicationProvidedPortPreventsDirectHandleCreation() {
        runner.withUserConfiguration(PortOverride.class)
                .withPropertyValues("egon.cola.component.ddc.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DdcConfigClient.class);
                    assertThat(context).doesNotHaveBean(DdcRpcClientHandle.class);
                });
    }

    @Test
    void registrySwitchCreatesRegistryPortsWithOnlyRegistryCredential() {
        runner.withUserConfiguration(RedisOverride.class)
                .withPropertyValues(
                        "egon.cola.component.ddc.registry.enabled=true",
                        "egon.cola.component.ddc.rpc.target=localhost:65535",
                        "egon.cola.component.ddc.rpc.auth.registry.access-key=registry-ak",
                        "egon.cola.component.ddc.rpc.auth.registry.secret-key=registry-sk"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context)
                            .hasSingleBean(DdcServiceRegistryClient.class)
                            .hasSingleBean(DdcRegistrySnapshotLoader.class)
                            .hasSingleBean(RpcProviderRegistry.class)
                            .hasSingleBean(RpcGatewayDirectory.class)
                            .hasSingleBean(RpcProcessIdentityProvider.class);
                    assertThat(context).doesNotHaveBean(DdcConfigClient.class);
                });
    }

    @Test
    void adapterPortsSatisfyStarterFailFastRequirements() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DdcRedisAutoConfiguration.class,
                        DdcRpcAutoConfiguration.class,
                        DdcAutoConfiguration.class,
                        DdcRegistryAutoConfiguration.class
                ))
                .withUserConfiguration(RedisOverride.class)
                .withBean(
                        DdcAdmissionTicketSupplier.class,
                        this::admissionTickets
                )
                .withPropertyValues(
                        "egon.cola.component.ddc.enabled=true",
                        "egon.cola.component.ddc.redis.enabled=false",
                        "egon.cola.component.ddc.registry.enabled=true",
                        "egon.cola.component.ddc.rpc.target=localhost:65535",
                        "egon.cola.component.ddc.rpc.auth.runtime.access-key=runtime-ak",
                        "egon.cola.component.ddc.rpc.auth.runtime.secret-key=runtime-sk",
                        "egon.cola.component.ddc.rpc.auth.registry.access-key=registry-ak",
                        "egon.cola.component.ddc.rpc.auth.registry.secret-key=registry-sk"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context)
                            .hasSingleBean(DdcConfigClient.class)
                            .hasSingleBean(DdcServiceRegistryClient.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class PortOverride {
        @Bean
        DdcConfigClient ddcConfigClient() {
            return mock(DdcConfigClient.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class RedisOverride {
        @Bean("ddcRedissonClient")
        RedissonClient ddcRedissonClient() {
            return mock(RedissonClient.class);
        }
    }
}
