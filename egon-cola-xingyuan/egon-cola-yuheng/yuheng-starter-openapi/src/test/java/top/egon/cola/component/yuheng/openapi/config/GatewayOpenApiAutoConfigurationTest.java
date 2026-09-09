package top.egon.cola.component.yuheng.openapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.cola.component.tianshu.http.registration.DdcHttpRegistrationContributor;
import top.egon.cola.component.yuheng.openapi.customizer.EgonOpenApiCustomizer;
import top.egon.cola.component.yuheng.openapi.customizer.EgonOperationCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOpenApiAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(GatewayOpenApiAutoConfiguration.class)
                    .withPropertyValues(
                            "egon.cola.component.yuheng.openapi.enabled=true",
                            "egon.cola.component.yuheng.openapi.biz-code=trade",
                            "egon.cola.component.yuheng.openapi.application-code=order-service",
                            "egon.cola.component.yuheng.openapi.resource-uri=https://order-service.example.test",
                            "egon.cola.component.yuheng.openapi.artifact-version=1.0.0",
                            "egon.cola.component.yuheng.openapi.build-id=build-1",
                            "egon.cola.component.yuheng.openapi.published-groups[0]=orders"
                    );

    @Test
    void exposesNamedGovernanceAndDdcContributorBeansWhenEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(GatewayOpenApiProperties.class);
            assertThat(context).hasSingleBean(EgonOperationCustomizer.class);
            assertThat(context).hasSingleBean(EgonOpenApiCustomizer.class);
            assertThat(context).hasSingleBean(
                    DdcHttpRegistrationContributor.class
            );
            assertThat(context).hasBean("egonOperationCustomizer");
            assertThat(context).hasBean("egonOpenApiCustomizer");
            assertThat(context).hasBean(
                    "gatewayOpenApiRegistrationContributor"
            );
        });
    }
}
