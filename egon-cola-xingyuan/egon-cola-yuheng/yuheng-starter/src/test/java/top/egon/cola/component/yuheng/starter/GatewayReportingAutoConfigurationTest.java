package top.egon.cola.component.yuheng.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.tianshu.http.registration
        .DdcHttpRegistrationContributor;
import top.egon.cola.component.yuheng.contract.reporting.GatewayDefinitionIdentity;
import top.egon.cola.component.yuheng.starter.reporting.GatewayReportHttpClient;
import top.egon.cola.component.yuheng.starter.reporting.GatewayReportingCoordinator;
import top.egon.cola.component.rpc.config.EgonRpcAutoConfig;
import top.egon.cola.component.rpc.provider.metadata.RpcProviderMetadataContributor;
import top.egon.cola.component.rpc.provider.metadata.RpcProviderMetadataMerger;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GatewayReportingAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            GatewayReportingAutoConfiguration.class
                    ))
                    .withBean(
                            GatewayReportingCoordinator.class,
                            () -> mock(GatewayReportingCoordinator.class)
                    );

    @Test
    void remainsDisabledByDefault() {
        runner.run(context -> assertThat(context)
                .doesNotHaveBean(GatewayDefinitionIdentity.class)
                .doesNotHaveBean(DdcHttpRegistrationContributor.class)
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
                        "egon.cola.component.yuheng.reporting.enabled=true",
                        "egon.cola.component.yuheng.reporting.admin-base-url="
                                + "http://127.0.0.1:18080",
                        "egon.cola.component.yuheng.reporting.biz-code=test-biz",
                        "egon.cola.component.yuheng.reporting."
                                + "application-code=inventory",
                        "egon.cola.component.yuheng.reporting."
                                + "application-name=Inventory",
                        "egon.cola.component.yuheng.reporting.env=test",
                        "egon.cola.component.yuheng.reporting.namespace=default",
                        "egon.cola.component.yuheng.reporting."
                                + "artifact-version=1.0.0",
                        "egon.cola.component.yuheng.reporting.build-id=build-1",
                        "egon.cola.component.yuheng.reporting.access-key=ak",
                        "egon.cola.component.yuheng.reporting.secret-key=sk"
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
                            DdcHttpRegistrationContributor.class
                    );
                    assertThat(context.getBean(
                            GatewayDefinitionIdentity.class
                    ).buildId()).isEqualTo("build-1");
                    DdcHttpRegistrationContributor httpContributor =
                            context.getBean(
                                    DdcHttpRegistrationContributor.class
                            );
                    assertThat(httpContributor.serviceVersion())
                            .isEqualTo("1.0.0");
                    assertThat(httpContributor.metadata()).containsEntry(
                            "yuheng.definition-set-id",
                            context.getBean(GatewayDefinitionIdentity.class)
                                    .definitionSetId()
                    );
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
                            "yuheng.definition-set-id",
                            identity.definitionSetId()
                    ).containsEntry(
                            "yuheng.artifact-version",
                            "1.0.0"
                    ).containsEntry(
                            "yuheng.build-id",
                            "build-1"
                    );
                });
    }

    private GatewayReportingProperties enabledProperties() {
        GatewayReportingProperties properties =
                new GatewayReportingProperties();
        properties.setEnabled(true);
        properties.setAdminBaseUrl("http://127.0.0.1:18080");
        properties.setApplicationCode("inventory");
        properties.setBizCode("test-biz");
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
