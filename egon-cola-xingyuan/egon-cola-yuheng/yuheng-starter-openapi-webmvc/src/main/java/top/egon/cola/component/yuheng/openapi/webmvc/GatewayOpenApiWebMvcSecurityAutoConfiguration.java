package top.egon.cola.component.yuheng.openapi.webmvc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.webmvc.api.MultipleOpenApiWebMvcResource;
import org.springdoc.webmvc.api.OpenApiResource;
import top.egon.cola.component.yuheng.openapi.config.GatewayOpenApiAutoConfiguration;
import top.egon.cola.component.yuheng.openapi.config.GatewayOpenApiProperties;
import top.egon.cola.component.yuheng.openapi.customizer.EgonOpenApiCustomizer;
import top.egon.cola.component.yuheng.openapi.customizer.EgonOperationCustomizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpAuthenticationToken;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpEndpointAuthenticationPolicy;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpSecurityObservationFilter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.ServiceAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.TokenAuditProofBO;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Isolates the secured Spring MVC Springdoc resources from application routes.
 */
@Slf4j
@AutoConfiguration(after = GatewayOpenApiAutoConfiguration.class)
@ConditionalOnClass({
        HttpSecurity.class,
        SecurityFilterChain.class,
        OpenApiResource.class
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
        prefix = "egon.cola.component.yuheng.openapi",
        name = "enabled",
        havingValue = "true"
)
public class GatewayOpenApiWebMvcSecurityAutoConfiguration {

    /**
     * Protects only grouped Springdoc documents; application security chains
     * remain responsible for every other request.
     */
    @Bean(name = "gatewayOpenApiWebMvcSecurityFilterChain")
    @Order(Ordered.LOWEST_PRECEDENCE - 100)
    @ConditionalOnMissingBean(name = "gatewayOpenApiWebMvcSecurityFilterChain")
    SecurityFilterChain gatewayOpenApiWebMvcSecurityFilterChain(
            HttpSecurity http,
            @Qualifier("userAccessTokenVerifier") UserAccessTokenVerifier userVerifier,
            @Qualifier("serviceAccessTokenVerifier") ServiceAccessTokenVerifier serviceVerifier,
            @Qualifier("idpObservedVerificationComponent") IdpObservedVerificationComponent observations,
            @Qualifier("jacksonObjectMapper") ObjectMapper mapper) throws Exception {
        if (!observations.enabled()) {
            throw new IllegalStateException("OPENAPI_SECURITY_OBSERVATION_REQUIRED");
        }
        var policy = new IdpEndpointAuthenticationPolicy(List.of(), List.of("/v3/api-docs/**"), false);
        var bearer = new IdpBearerAuthenticationFilter(userVerifier, serviceVerifier, policy, mapper);
        var observation = new IdpSecurityObservationFilter(observations, policy);
        return http
                .securityMatcher("/v3/api-docs/**")
                .csrf(csrf -> csrf.disable())
                .requestCache(cache -> cache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .addFilterBefore(bearer, AnonymousAuthenticationFilter.class)
                .addFilterBefore(observation, IdpBearerAuthenticationFilter.class)
                .headers(headers -> headers.addHeaderWriter((request, response) -> {
                    response.setHeader("Cache-Control", "no-store");
                    if (response.getStatus() == 401) { response.setHeader("WWW-Authenticate", "Bearer"); }
                }))
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().access((authentication, context) -> {
                            var current = authentication.get();
                            return new AuthorizationDecision(current instanceof IdpAuthenticationToken
                                    && current.isAuthenticated() && current.getPrincipal() instanceof ServiceIdentityPrincipal
                                    && current.getDetails() instanceof TokenAuditProofBO
                                    && current.getAuthorities().stream().anyMatch(authority ->
                                            "SCOPE_yuheng.openapi.read".equals(authority.getAuthority())));
                        }))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, failure) -> {
                    var marker = IdpSecurityObservationFilter.authorizationObservationContext(request);
                    response.setStatus(marker != null && IdpSecurityObservationFilter.beforeAuthenticationFailure(request) ? 401 : 503);
                }).accessDeniedHandler((request, response, failure) -> {
                    var marker = IdpSecurityObservationFilter.authorizationObservationContext(request);
                    response.setStatus(marker != null && IdpSecurityObservationFilter.beforeAuthorizationDenied(request) ? 403 : 503);
                }))
                .build();
    }

    /**
     * Adds the common governance customizers to the provider-owned groups
     * after Springdoc has materialized both bean and property groups.
     */
    @Bean(name = "gatewayOpenApiWebMvcGroupedOpenApiCustomizer")
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(name = "gatewayOpenApiWebMvcGroupedOpenApiCustomizer")
    static BeanPostProcessor gatewayOpenApiWebMvcGroupedOpenApiCustomizer(
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
            if (bean instanceof MultipleOpenApiWebMvcResource) {
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
