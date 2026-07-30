package top.egon.cola.component.gateway.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.reactive.result.method.annotation
        .RequestMappingHandlerMapping;
import top.egon.cola.component.gateway.contract.reporting.GatewayDefinitionIdentity;
import top.egon.cola.component.gateway.starter.discovery
        .MvcGatewayDefinitionContributor;
import top.egon.cola.component.gateway.starter.discovery
        .WebFluxGatewayDefinitionContributor;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportHttpClient;
import top.egon.cola.component.rpc.config.EgonRpcAutoConfig;
import top.egon.cola.component.rpc.provider.RpcProviderMetadataContributor;
import top.egon.cola.component.rpc.provider.RpcProviderMetadataMerger;
import top.egon.cola.component.rpc.provider.RpcServiceIdentity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayReportingAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            GatewayReportingAutoConfiguration.class
                    ));

    @Test
    void remainsDisabledByDefault() {
        runner.run(context -> assertThat(context)
                .doesNotHaveBean(GatewayDefinitionIdentity.class)
                .doesNotHaveBean(RpcProviderMetadataContributor.class));
    }

    @Test
    void runsAfterRpcContractCatalogAutoConfiguration() {
        AutoConfigureAfter ordering = GatewayReportingAutoConfiguration.class
                .getAnnotation(AutoConfigureAfter.class);

        assertThat(ordering).isNotNull();
        assertThat(ordering.value()).contains(EgonRpcAutoConfig.class);
    }

    @Test
    void publishesStableIdentityWhenEnabled() {
        runner.withPropertyValues(
                        "egon.cola.component.gateway.reporting.enabled=true",
                        "egon.cola.component.gateway.reporting.admin-base-url="
                                + "http://127.0.0.1:18080",
                        "egon.cola.component.gateway.reporting."
                                + "application-code=inventory",
                        "egon.cola.component.gateway.reporting."
                                + "application-name=Inventory",
                        "egon.cola.component.gateway.reporting.env=test",
                        "egon.cola.component.gateway.reporting.namespace=default",
                        "egon.cola.component.gateway.reporting."
                                + "artifact-version=1.0.0",
                        "egon.cola.component.gateway.reporting.build-id=build-1",
                        "egon.cola.component.gateway.reporting.access-key=ak",
                        "egon.cola.component.gateway.reporting.secret-key=sk"
                )
                .withBean(
                        GatewayReportHttpClient.class,
                        () -> new GatewayReportHttpClient(
                                enabledProperties()
                        )
                )
                .run(context -> {
                    assertThat(context)
                            .hasSingleBean(GatewayDefinitionIdentity.class);
                    assertThat(context).hasSingleBean(
                            top.egon.cola.component.gateway.contract.definition
                                    .GatewayDefinitionIdentity.class
                    );
                    assertThat(context.getBean(
                            GatewayDefinitionIdentity.class
                    ).buildId()).isEqualTo("build-1");
                    assertThat(context.getBean(
                            top.egon.cola.component.gateway.contract.definition
                                    .GatewayDefinitionIdentity.class
                    ).definitionSetId()).isEqualTo(context.getBean(
                            GatewayDefinitionIdentity.class
                    ).definitionSetId());
                    Map<String, String> rpcMetadata =
                            new RpcProviderMetadataMerger(
                                    context.getBeansOfType(
                                            RpcProviderMetadataContributor.class
                                    ).values()
                            ).merge(
                                    new RpcServiceIdentity(
                                            "inventory",
                                            "default",
                                            "1.0.0"
                                    ),
                                    Map.of()
                            );
                    GatewayDefinitionIdentity identity = context.getBean(
                            GatewayDefinitionIdentity.class
                    );
                    assertThat(rpcMetadata).containsEntry(
                            "gateway.definition-set-id",
                            identity.definitionSetId()
                    ).containsEntry(
                            "gateway.artifact-version",
                            "1.0.0"
                    ).containsEntry(
                            "gateway.build-id",
                            "build-1"
                    );
                });
    }

    @Test
    void selectsApplicationWebFluxMappingsWhenActuatorMappingsAlsoExist() {
        runner.withPropertyValues(
                        "egon.cola.component.gateway.reporting.enabled=true",
                        "egon.cola.component.gateway.reporting.admin-base-url="
                                + "http://127.0.0.1:18080",
                        "egon.cola.component.gateway.reporting."
                                + "application-code=inventory",
                        "egon.cola.component.gateway.reporting."
                                + "application-name=Inventory",
                        "egon.cola.component.gateway.reporting.env=test",
                        "egon.cola.component.gateway.reporting.namespace=default",
                        "egon.cola.component.gateway.reporting."
                                + "artifact-version=1.0.0",
                        "egon.cola.component.gateway.reporting.build-id=build-1",
                        "egon.cola.component.gateway.reporting.access-key=ak",
                        "egon.cola.component.gateway.reporting.secret-key=sk"
                )
                .withBean(
                        "requestMappingHandlerMapping",
                        RequestMappingHandlerMapping.class,
                        RequestMappingHandlerMapping::new
                )
                .withBean(
                        "controllerEndpointHandlerMapping",
                        RequestMappingHandlerMapping.class,
                        RequestMappingHandlerMapping::new
                )
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .run(context -> assertThat(context).hasSingleBean(
                        WebFluxGatewayDefinitionContributor.class
                ));
    }

    @Test
    void selectsApplicationMvcMappingsWhenActuatorMappingsAlsoExist() {
        runner.withPropertyValues(
                        "egon.cola.component.gateway.reporting.enabled=true",
                        "egon.cola.component.gateway.reporting.admin-base-url="
                                + "http://127.0.0.1:18080",
                        "egon.cola.component.gateway.reporting."
                                + "application-code=inventory",
                        "egon.cola.component.gateway.reporting."
                                + "application-name=Inventory",
                        "egon.cola.component.gateway.reporting.env=test",
                        "egon.cola.component.gateway.reporting.namespace=default",
                        "egon.cola.component.gateway.reporting."
                                + "artifact-version=1.0.0",
                        "egon.cola.component.gateway.reporting.build-id=build-1",
                        "egon.cola.component.gateway.reporting.access-key=ak",
                        "egon.cola.component.gateway.reporting.secret-key=sk"
                )
                .withBean(
                        "requestMappingHandlerMapping",
                        org.springframework.web.servlet.mvc.method.annotation
                                .RequestMappingHandlerMapping.class,
                        org.springframework.web.servlet.mvc.method.annotation
                                .RequestMappingHandlerMapping::new
                )
                .withBean(
                        "controllerEndpointHandlerMapping",
                        org.springframework.web.servlet.mvc.method.annotation
                                .RequestMappingHandlerMapping.class,
                        org.springframework.web.servlet.mvc.method.annotation
                                .RequestMappingHandlerMapping::new
                )
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .run(context -> assertThat(context).hasSingleBean(
                        MvcGatewayDefinitionContributor.class
                ));
    }

    private GatewayReportingProperties enabledProperties() {
        GatewayReportingProperties properties =
                new GatewayReportingProperties();
        properties.setEnabled(true);
        properties.setAdminBaseUrl("http://127.0.0.1:18080");
        properties.setApplicationCode("inventory");
        properties.setApplicationName("Inventory");
        properties.setEnv("test");
        properties.setNamespace("default");
        properties.setArtifactVersion("1.0.0");
        properties.setBuildId("build-1");
        properties.setAccessKey("ak");
        properties.setSecretKey("sk");
        return properties;
    }
}
