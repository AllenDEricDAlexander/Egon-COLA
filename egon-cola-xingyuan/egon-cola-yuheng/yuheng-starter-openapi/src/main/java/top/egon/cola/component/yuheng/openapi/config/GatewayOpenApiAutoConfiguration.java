package top.egon.cola.component.yuheng.openapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springdoc.core.customizers.OperationCustomizer;
import top.egon.cola.component.tianshu.http.registration.DdcHttpRegistrationContributor;
import top.egon.cola.component.yuheng.openapi.customizer.EgonOpenApiCustomizer;
import top.egon.cola.component.yuheng.openapi.customizer.EgonOperationCustomizer;
import top.egon.cola.component.yuheng.openapi.registration.GatewayOpenApiRegistrationContributor;

/**
 * Auto-configures framework-neutral OpenAPI governance extensions.
 */
@AutoConfiguration
@ConditionalOnClass({OpenAPI.class, OperationCustomizer.class})
@ConditionalOnProperty(
        prefix = "egon.cola.component.gateway.openapi",
        name = "enabled",
        havingValue = "true"
)
public class GatewayOpenApiAutoConfiguration {

    @Bean(name = "gatewayOpenApiProperties")
    @ConditionalOnMissingBean(name = "gatewayOpenApiProperties")
    @ConfigurationProperties("egon.cola.component.gateway.openapi")
    public GatewayOpenApiProperties gatewayOpenApiProperties() {
        return new GatewayOpenApiProperties();
    }

    @Bean(name = "egonOperationCustomizer")
    @ConditionalOnMissingBean(name = "egonOperationCustomizer")
    public OperationCustomizer egonOperationCustomizer(
            @Qualifier("gatewayOpenApiProperties")
            GatewayOpenApiProperties properties) {
        return new EgonOperationCustomizer(properties);
    }

    @Bean(name = "egonOpenApiCustomizer")
    @ConditionalOnMissingBean(name = "egonOpenApiCustomizer")
    public OpenApiCustomizer egonOpenApiCustomizer(
            @Qualifier("gatewayOpenApiProperties")
            GatewayOpenApiProperties properties) {
        return new EgonOpenApiCustomizer(properties);
    }

    @Bean(name = "gatewayOpenApiRegistrationContributor")
    @ConditionalOnMissingBean(name = "gatewayOpenApiRegistrationContributor")
    public DdcHttpRegistrationContributor
            gatewayOpenApiRegistrationContributor(
            @Qualifier("gatewayOpenApiProperties")
            GatewayOpenApiProperties properties) {
        return new GatewayOpenApiRegistrationContributor(properties);
    }
}
