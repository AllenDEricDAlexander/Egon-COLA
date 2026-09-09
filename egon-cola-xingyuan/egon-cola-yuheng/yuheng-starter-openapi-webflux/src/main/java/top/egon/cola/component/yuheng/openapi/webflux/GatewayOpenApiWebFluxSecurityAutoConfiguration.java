package top.egon.cola.component.yuheng.openapi.webflux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.webflux.api.MultipleOpenApiWebFluxResource;
import org.springdoc.webflux.api.OpenApiResource;
import top.egon.cola.component.yuheng.openapi.config.GatewayOpenApiAutoConfiguration;
import top.egon.cola.component.yuheng.openapi.config.GatewayOpenApiProperties;
import top.egon.cola.component.yuheng.openapi.customizer.EgonOpenApiCustomizer;
import top.egon.cola.component.yuheng.openapi.customizer.EgonOperationCustomizer;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Isolates the secured Spring WebFlux Springdoc resources from application routes.
 */
@AutoConfiguration(after = GatewayOpenApiAutoConfiguration.class)
@ConditionalOnClass({
        ServerHttpSecurity.class,
        SecurityWebFilterChain.class,
        OpenApiResource.class
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(
        prefix = "egon.cola.component.gateway.openapi",
        name = "enabled",
        havingValue = "true"
)
public class GatewayOpenApiWebFluxSecurityAutoConfiguration {

    /**
     * Protects only grouped Springdoc documents; application security chains
     * remain responsible for every other request.
     */
    @Bean(name = "gatewayOpenApiWebFluxSecurityWebFilterChain")
    @Order(Ordered.LOWEST_PRECEDENCE - 100)
    @ConditionalOnMissingBean(name = "gatewayOpenApiWebFluxSecurityWebFilterChain")
    SecurityWebFilterChain gatewayOpenApiWebFluxSecurityWebFilterChain(
            ServerHttpSecurity http) {
        return http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers(
                        "/v3/api-docs/**"))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(authorize -> authorize
                        .anyExchange().hasAuthority("SCOPE_gateway.openapi.read"))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                        Customizer.withDefaults()))
                .build();
    }

    /**
     * Adds the common governance customizers to the provider-owned groups
     * after Springdoc has materialized both bean and property groups.
     */
    @Bean(name = "gatewayOpenApiWebFluxGroupedOpenApiCustomizer")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(name = "gatewayOpenApiWebFluxGroupedOpenApiCustomizer")
    static BeanPostProcessor gatewayOpenApiWebFluxGroupedOpenApiCustomizer(
            ConfigurableListableBeanFactory beanFactory) {
        return new GroupedOpenApiGovernancePostProcessor(beanFactory);
    }

    private static final class GroupedOpenApiGovernancePostProcessor
            implements BeanPostProcessor {

        private final ConfigurableListableBeanFactory beanFactory;
        private GatewayOpenApiProperties properties;
        private EgonOperationCustomizer operationCustomizer;
        private EgonOpenApiCustomizer openApiCustomizer;
        private final Set<GroupedOpenApi> customized = Collections.newSetFromMap(
                new IdentityHashMap<>());

        private GroupedOpenApiGovernancePostProcessor(
                ConfigurableListableBeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        @Override
        public Object postProcessBeforeInitialization(
                Object bean,
                String beanName) {
            if (bean instanceof MultipleOpenApiWebFluxResource) {
                ensureDelegates();
                beanFactory.getBeansOfType(GroupedOpenApi.class).values()
                        .forEach(this::customize);
            } else if (bean instanceof GroupedOpenApi groupedOpenApi) {
                ensureDelegates();
                customize(groupedOpenApi);
            }
            return bean;
        }

        private void ensureDelegates() {
            if (properties != null) {
                return;
            }
            properties = beanFactory.getBean(
                    "gatewayOpenApiProperties", GatewayOpenApiProperties.class);
            operationCustomizer = beanFactory.getBean(
                    "egonOperationCustomizer", EgonOperationCustomizer.class);
            openApiCustomizer = beanFactory.getBean(
                    "egonOpenApiCustomizer", EgonOpenApiCustomizer.class);
            properties.validate();
        }

        private void customize(GroupedOpenApi groupedOpenApi) {
            if (!properties.getPublishedGroups().contains(
                    groupedOpenApi.getGroup()) || !customized.add(
                    groupedOpenApi)) {
                return;
            }
            if (!groupedOpenApi.getOperationCustomizers().contains(
                    operationCustomizer)) {
                groupedOpenApi.addAllOperationCustomizer(
                        List.of(operationCustomizer));
            }
            groupedOpenApi.addAllOpenApiCustomizer(List.of(
                    new GroupRootOpenApiCustomizer(
                            openApiCustomizer,
                            groupedOpenApi.getGroup())));
        }
    }

    private record GroupRootOpenApiCustomizer(
            EgonOpenApiCustomizer delegate,
            String group) implements org.springdoc.core.customizers.OpenApiCustomizer {

        @Override
        public void customise(io.swagger.v3.oas.models.OpenAPI openAPI) {
            delegate.customise(openAPI, group);
        }
    }
}
