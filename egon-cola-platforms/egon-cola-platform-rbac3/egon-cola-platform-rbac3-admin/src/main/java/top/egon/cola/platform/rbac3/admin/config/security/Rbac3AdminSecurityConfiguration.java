package top.egon.cola.platform.rbac3.admin.config.security;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.rbac3.starter.security.Rbac3BearerAuthenticationFilter;
import top.egon.cola.platform.rbac3.admin.tenant.controller.filter.TenantContextFilter;
import top.egon.cola.platform.rbac3.admin.tenant.service.TenantContextResolver;

/**
 * 类型 `Rbac3AdminSecurityConfiguration` 位于当前包内，是类型，用于承载 `Rbac3 Admin Security Configuration` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3AdminSecurityConfiguration` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Admin Security Configuration`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `Rbac3AdminSecurityConfiguration` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `Rbac3AdminSecurityConfiguration` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class Rbac3AdminSecurityConfiguration {

    /**
     * 方法 `annotationTemplateExpressionDefaults` 按照 `Rbac3AdminSecurityConfiguration` 的职责处理输入，完成 `annotation Template Expression Defaults` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `annotationTemplateExpressionDefaults` processes its inputs according to `Rbac3AdminSecurityConfiguration`'s responsibility, performs the `annotation Template Expression Defaults` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `annotationTemplateExpressionDefaults` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `annotationTemplateExpressionDefaults`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    static AnnotationTemplateExpressionDefaults annotationTemplateExpressionDefaults() {
        return new AnnotationTemplateExpressionDefaults();
    }

    /**
     * 方法 `tenantContextResolver` 按照 `Rbac3AdminSecurityConfiguration` 的职责处理输入，完成 `tenant Context Resolver` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantContextResolver` processes its inputs according to `Rbac3AdminSecurityConfiguration`'s responsibility, performs the `tenant Context Resolver` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenantContextResolver` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenantContextResolver`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    TenantContextResolver tenantContextResolver() {
        return new TenantContextResolver();
    }

    /**
     * 方法 `tenantContextFilter` 按照 `Rbac3AdminSecurityConfiguration` 的职责处理输入，完成 `tenant Context Filter` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantContextFilter` processes its inputs according to `Rbac3AdminSecurityConfiguration`'s responsibility, performs the `tenant Context Filter` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenantContextFilter` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenantContextFilter`, then continue the business flow using its result, exception, or side effect.
     *
     * @param resolver 输入参数 `resolver`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    TenantContextFilter tenantContextFilter(TenantContextResolver resolver) {
        return new TenantContextFilter(resolver);
    }

    /**
     * 方法 `rbac3AdminPrincipalFilter` 按照 `Rbac3AdminSecurityConfiguration` 的职责处理输入，完成 `rbac3 Admin Principal Filter` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3AdminPrincipalFilter` processes its inputs according to `Rbac3AdminSecurityConfiguration`'s responsibility, performs the `rbac3 Admin Principal Filter` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3AdminPrincipalFilter` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3AdminPrincipalFilter`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    Rbac3AdminPrincipalFilter rbac3AdminPrincipalFilter() {
        return new Rbac3AdminPrincipalFilter();
    }

    /**
     * 方法 `rbac3InternalSecurityFilterChain` 按照 `Rbac3AdminSecurityConfiguration` 的职责处理输入，完成 `rbac3 Internal Security Filter Chain` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3InternalSecurityFilterChain` processes its inputs according to `Rbac3AdminSecurityConfiguration`'s responsibility, performs the `rbac3 Internal Security Filter Chain` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3InternalSecurityFilterChain` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3InternalSecurityFilterChain`, then continue the business flow using its result, exception, or side effect.
     *
     * @param http 输入参数 `http`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantFilter 输入参数 `tenantFilter`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idpFilter 输入参数 `idpFilter`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     * @throws Exception 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
     */
    @Bean
    @Order(1)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    SecurityFilterChain rbac3InternalSecurityFilterChain(
            HttpSecurity http,
            TenantContextFilter tenantFilter,
            IdpBearerAuthenticationFilter idpFilter)
            throws Exception {
        return http
                .securityMatcher("/internal/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated())
                .addFilterBefore(idpFilter, AnonymousAuthenticationFilter.class)
                .addFilterAfter(tenantFilter, IdpBearerAuthenticationFilter.class)
                .build();
    }

    /**
     * 方法 `rbac3SecurityFilterChain` 按照 `Rbac3AdminSecurityConfiguration` 的职责处理输入，完成 `rbac3 Security Filter Chain` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3SecurityFilterChain` processes its inputs according to `Rbac3AdminSecurityConfiguration`'s responsibility, performs the `rbac3 Security Filter Chain` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3SecurityFilterChain` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3SecurityFilterChain`, then continue the business flow using its result, exception, or side effect.
     *
     * @param http 输入参数 `http`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantFilter 输入参数 `tenantFilter`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param authenticationConverter 输入参数 `authenticationConverter`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principalFilter 输入参数 `principalFilter`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idpFilters 输入参数 `idpFilters`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param rbac3Filters 输入参数 `rbac3Filters`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     * @throws Exception 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
     */
    @Bean
    @Order(2)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    SecurityFilterChain rbac3SecurityFilterChain(
            HttpSecurity http,
            TenantContextFilter tenantFilter,
            Rbac3JwtAuthenticationConverter authenticationConverter,
            Rbac3AdminPrincipalFilter principalFilter,
            ObjectProvider<IdpBearerAuthenticationFilter> idpFilters,
            ObjectProvider<Rbac3BearerAuthenticationFilter> rbac3Filters
    ) throws Exception {
        IdpBearerAuthenticationFilter idpFilter = idpFilters.getIfAvailable();
        Rbac3BearerAuthenticationFilter rbac3Filter = rbac3Filters.getIfAvailable();
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/liveness",
                                "/actuator/health/readiness")
                        .permitAll()
                        .anyRequest().authenticated());
        if (idpFilter != null && rbac3Filter != null) {
            http.addFilterBefore(idpFilter, AnonymousAuthenticationFilter.class);
            http.addFilterAfter(rbac3Filter, IdpBearerAuthenticationFilter.class);
            http.addFilterAfter(principalFilter, Rbac3BearerAuthenticationFilter.class);
            http.addFilterAfter(tenantFilter, Rbac3AdminPrincipalFilter.class);
        } else if (idpFilter == null && rbac3Filter == null) {
            http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(
                    jwt -> jwt.jwtAuthenticationConverter(authenticationConverter)));
            http.addFilterAfter(tenantFilter, AnonymousAuthenticationFilter.class);
        } else {
            throw new IllegalStateException(
                    "IdP and RBAC3 authentication filters must be configured together");
        }
        return http.build();
    }

    /**
     * 方法 `failOnUnknownJsonFields` 按照 `Rbac3AdminSecurityConfiguration` 的职责处理输入，完成 `fail On Unknown Json Fields` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `failOnUnknownJsonFields` processes its inputs according to `Rbac3AdminSecurityConfiguration`'s responsibility, performs the `fail On Unknown Json Fields` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `failOnUnknownJsonFields` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `failOnUnknownJsonFields`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    Jackson2ObjectMapperBuilderCustomizer failOnUnknownJsonFields() {
        return builder -> builder.featuresToEnable(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
