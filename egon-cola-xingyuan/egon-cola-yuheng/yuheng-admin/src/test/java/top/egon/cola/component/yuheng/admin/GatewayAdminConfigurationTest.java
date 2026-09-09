package top.egon.cola.component.yuheng.admin.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import top.egon.cola.component.yuheng.admin.config.properties.GatewayAdminOpenApiProperties;
import top.egon.cola.component.tianshu.api.client.DdcManagementClient;
import top.egon.cola.component.rpc.tianshu.client.DdcRpcClientFactory;
import top.egon.cola.component.rpc.tianshu.client.DdcRpcClientHandle;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayAdminConfigurationTest {

    @Test
    void projectionClockIsNamedAndIndependentOfOpenApiFlag() throws Exception {
        Method factory = GatewayAdminConfiguration.class.getDeclaredMethod("gatewayProjectionClock");
        assertThat(factory.getAnnotation(org.springframework.context.annotation.Bean.class).value())
                .containsExactly("gatewayProjectionClock");
        assertThat(factory.getAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class))
                .isNull();
        assertThat(new GatewayAdminConfiguration().gatewayProjectionClock().getZone())
                .isEqualTo(java.time.ZoneOffset.UTC);
    }

    @Test
    void projectionConstructorQualifiesClockAndValidatesGroupBoundary() {
        Class<?> type = top.egon.cola.component.yuheng.admin.runtime.service.GatewayProjectionService.class;
        assertThat(type.getConstructors()).hasSize(1);
        assertThat(Arrays.stream(type.getConstructors()[0].getParameters())
                .map(parameter -> parameter.getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class).value()))
                .contains("gatewayProjectionClock", "gatewayEngineRoleConsistencyStrategy", "ddcManagementClient");
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(type,
                        top.egon.cola.component.yuheng.admin.runtime.service.GatewayEngineRoleConsistencyStrategy.class)
                .withBean("gatewayGroupRepository", top.egon.cola.component.yuheng.admin.group.repository.GatewayGroupRepository.class,
                        () -> mock(top.egon.cola.component.yuheng.admin.group.repository.GatewayGroupRepository.class))
                .withBean("gatewayReleaseService", top.egon.cola.component.yuheng.admin.release.service.GatewayReleaseService.class,
                        () -> mock(top.egon.cola.component.yuheng.admin.release.service.GatewayReleaseService.class))
                .withBean("jdbcGatewayReleasePublicationRepository",
                        top.egon.cola.component.yuheng.admin.release.repository.GatewayReleasePublicationRepository.class,
                        () -> mock(top.egon.cola.component.yuheng.admin.release.repository.GatewayReleasePublicationRepository.class))
                .withBean("gatewayProjectionClock", java.time.Clock.class,
                        () -> new GatewayAdminConfiguration().gatewayProjectionClock())
                .withBean("unrelatedClock", java.time.Clock.class, java.time.Clock::systemDefaultZone)
                .withBean("gateway.admin-top.egon.cola.component.yuheng.admin.config.GatewayAdminProperties",
                        top.egon.cola.component.yuheng.admin.config.GatewayAdminProperties.class,
                        top.egon.cola.component.yuheng.admin.config.GatewayAdminProperties::new)
                .withBean(org.springframework.validation.beanvalidation.MethodValidationPostProcessor.class,
                        org.springframework.validation.beanvalidation.MethodValidationPostProcessor::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var service = context.getBean(
                            top.egon.cola.component.yuheng.admin.runtime.service.GatewayProjectionService.class);
                    org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.runtimeConsistency(" "))
                            .isInstanceOf(jakarta.validation.ConstraintViolationException.class);
                });
    }

    @Test
    void createsDdcManagementClientThroughTheDirectRpcFactory() {
        GatewayAdminConfiguration configuration =
                new GatewayAdminConfiguration();
        DdcRpcClientFactory factory = mock(DdcRpcClientFactory.class);
        DdcManagementClient client = mock(DdcManagementClient.class);
        DdcRpcClientHandle<DdcManagementClient> expected =
                new DdcRpcClientHandle<>(client, () -> {
                });
        when(factory.managementClient()).thenReturn(expected);

        DdcRpcClientHandle<DdcManagementClient> handle = configuration
                .gatewayDdcManagementClientHandle(factory);

        assertThat(handle).isSameAs(expected);
        assertThat(configuration.ddcManagementClient(handle)).isSameAs(client);
        verify(factory).managementClient();
    }

    @Test
    void consumesTheGatewayEngineDefaultCallEventTopic() {
        Method consumerFactory = Arrays.stream(
                        GatewayAdminConfiguration.class.getDeclaredMethods()
                )
                .filter(method -> method.getName().equals(
                        "gatewayKafkaCallEventConsumer"
                ))
                .findFirst()
                .orElseThrow();

        assertThat(Arrays.stream(consumerFactory.getParameters())
                .map(parameter -> parameter.getAnnotation(Value.class))
                .filter(java.util.Objects::nonNull)
                .map(Value::value))
                .contains(
                        "${gateway.admin.observability.kafka.topic:"
                                + "egon.gateway.call.v1}"
                );
    }

    @Test
    void enablesTheTypedOpenApiConfigurationContract() {
        EnableConfigurationProperties annotation =
                GatewayAdminConfiguration.class.getAnnotation(
                        EnableConfigurationProperties.class
                );

        assertThat(annotation).isNotNull();
        assertThat(annotation.value())
                .contains(GatewayAdminOpenApiProperties.class);
    }

    @Test
    void providesOpenApiMapperWithJavaTimeSupport() throws Exception {
        ObjectMapper mapper = new GatewayAdminConfiguration()
                .gatewayOpenApiObjectMapper();

        assertThat(mapper.getRegisteredModuleIds()).isNotEmpty();
        var value = mapper.readTree(mapper.writeValueAsString(
                Instant.parse("2026-08-27T00:00:00Z")));
        assertThat(value.isTextual()).isTrue();
        assertThat(value.asText()).isEqualTo("2026-08-27T00:00:00Z");
    }
}
