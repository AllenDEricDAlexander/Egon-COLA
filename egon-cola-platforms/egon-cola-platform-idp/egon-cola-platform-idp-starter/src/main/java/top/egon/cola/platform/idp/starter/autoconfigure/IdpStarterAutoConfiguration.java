package top.egon.cola.platform.idp.starter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.idp.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.idp.starter.security.RetryingJwtDecoder;
import top.egon.cola.platform.idp.starter.state.IdentityUserStateReader;
import top.egon.cola.platform.idp.starter.state.RedisIdentityUserStateReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 为普通 Servlet 资源服务器装配统一 IdP 身份验证能力。
 * 本配置创建 JWT 解码器、用户实时状态读取器、身份验证器与 Bearer 过滤器；
 * 它只消费并验证 IdP 签发的访问令牌，不负责签发令牌或执行接口权限判断。
 *
 * <p>Auto-configures unified IdP identity verification for regular Servlet resource servers.
 * It creates the JWT decoder, current-user-state reader, identity verifier, and Bearer filter.
 * It consumes and validates access tokens issued by the IdP; it neither issues tokens nor makes
 * endpoint authorization decisions.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(IdpStarterProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.idp",
        name = "enabled",
        havingValue = "true")
public class IdpStarterAutoConfiguration {

    /**
     * 创建 IdP Starter 自动配置实例。
     *
     * <p>Creates the IdP Starter auto-configuration instance.</p>
     */
    public IdpStarterAutoConfiguration() {
    }

    /**
     * 创建支持 JWK 刷新的 IdP JWT 解码器。
     *
     * <p>Creates the IdP JWT decoder with one-time JWK refresh support.</p>
     *
     * @param properties IdP Starter 配置；IdP Starter settings
     * @return 名为 {@code idpJwtDecoder} 的 JWT 解码器；the JWT decoder named
     *         {@code idpJwtDecoder}
     */
    @Bean(name = "idpJwtDecoder")
    @ConditionalOnMissingBean(name = "idpJwtDecoder")
    public JwtDecoder idpJwtDecoder(IdpStarterProperties properties) {
        properties.validate();
        return new RetryingJwtDecoder(() -> decoder(properties));
    }

    /**
     * 创建从共享 Redis 键空间读取用户实时状态的端口实现。
     * 如果存在 RBAC3 专用客户端则优先复用，否则要求容器中只有一个可确定的 Redisson 客户端。
     *
     * <p>Creates the port implementation that reads current user state from the shared Redis key
     * space. The RBAC3 runtime client is preferred when present; otherwise a unique Redisson client
     * must be available.</p>
     *
     * @param redissonClients 容器中的 Redisson 客户端候选；Redisson client candidates
     * @param beanFactory 用于按名称选择客户端的 Bean 工厂；bean factory used for named lookup
     * @param objectMapper 用户状态 JSON 反序列化器；JSON mapper for user-state deserialization
     * @param properties IdP Starter 配置；IdP Starter settings
     * @return 用户实时状态读取器；current user-state reader
     */
    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean
    public IdentityUserStateReader identityUserStateReader(
            ObjectProvider<RedissonClient> redissonClients,
            ListableBeanFactory beanFactory,
            ObjectMapper objectMapper,
            IdpStarterProperties properties
    ) {
        properties.validate();
        return new RedisIdentityUserStateReader(
                identityStateRedisson(redissonClients, beanFactory),
                objectMapper,
                properties.getUserStateKeyPrefix());
    }

    /**
     * 创建共享的 IdP 访问令牌验证器。
     *
     * <p>Creates the shared IdP access-token verifier.</p>
     *
     * @param decoder JWT 解码器；JWT decoder
     * @param stateReader 用户实时状态读取器；current user-state reader
     * @param properties IdP Starter 配置；IdP Starter settings
     * @return IdP JWT 验证器；IdP JWT verifier
     */
    @Bean
    @ConditionalOnBean(IdentityUserStateReader.class)
    @ConditionalOnMissingBean
    public IdpJwtVerifier idpJwtVerifier(
            @Qualifier("idpJwtDecoder") JwtDecoder decoder,
            IdentityUserStateReader stateReader,
            IdpStarterProperties properties
    ) {
        properties.validate();
        return new IdpJwtVerifier(
                decoder,
                stateReader,
                properties.getAudiences(),
                properties.getClientIds());
    }

    /**
     * 创建把已验证身份写入 Spring Security 上下文的 Bearer 过滤器。
     *
     * <p>Creates the Bearer filter that stores a verified identity in the Spring Security
     * context.</p>
     *
     * @param verifier IdP JWT 验证器；IdP JWT verifier
     * @param objectMapper 认证失败响应的 JSON 序列化器；JSON mapper for authentication failures
     * @return IdP Bearer 身份过滤器；IdP Bearer identity filter
     */
    @Bean
    @ConditionalOnBean(IdpJwtVerifier.class)
    @ConditionalOnMissingBean
    public IdpBearerAuthenticationFilter idpBearerAuthenticationFilter(
            IdpJwtVerifier verifier,
            ObjectMapper objectMapper
    ) {
        return new IdpBearerAuthenticationFilter(verifier, objectMapper);
    }

    /**
     * 注册 IdP Bearer 过滤器并设置其 Servlet 执行顺序。
     * 是否启用注册由 {@code registerFilter} 配置控制。
     *
     * <p>Registers the IdP Bearer filter and assigns its Servlet ordering. Registration can be
     * disabled through the {@code registerFilter} setting.</p>
     *
     * @param filter IdP Bearer 身份过滤器；IdP Bearer identity filter
     * @param properties IdP Starter 配置；IdP Starter settings
     * @return Servlet 过滤器注册对象；Servlet filter registration
     */
    @Bean
    @ConditionalOnBean(IdpBearerAuthenticationFilter.class)
    @ConditionalOnMissingBean(name = "idpBearerFilterRegistration")
    public FilterRegistrationBean<IdpBearerAuthenticationFilter>
            idpBearerFilterRegistration(
                    IdpBearerAuthenticationFilter filter,
                    IdpStarterProperties properties) {
        FilterRegistrationBean<IdpBearerAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setName("idpBearerAuthenticationFilter");
        registration.setOrder(-102);
        registration.setEnabled(properties.isRegisterFilter());
        return registration;
    }

    /**
     * 根据 JWK Set 地址构造 Nimbus 解码器，并同时校验签发方和受众。
     *
     * <p>Builds a Nimbus decoder from the JWK Set endpoint and validates both issuer and
     * audience.</p>
     *
     * @param properties 已完成校验的 IdP Starter 配置；validated IdP Starter settings
     * @return 配置完成的 JWT 解码器；configured JWT decoder
     */
    private JwtDecoder decoder(IdpStarterProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(
                properties.getJwkSetUri().trim()).build();
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefaultWithIssuer(
                properties.getIssuer().trim()));
        Set<String> audiences = Set.copyOf(properties.getAudiences());
        validators.add(jwt -> jwt.getAudience().stream().anyMatch(
                audiences::contains)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "JWT audience is invalid", null)));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    /**
     * 选择读取 IdP 用户状态所用的 Redisson 客户端。
     *
     * <p>Selects the Redisson client used to read current IdP user state.</p>
     *
     * @param clients 容器中的 Redisson 客户端候选；Redisson client candidates
     * @param beanFactory 用于按名称查找客户端的 Bean 工厂；bean factory for named lookup
     * @return 已确定的 Redisson 客户端；resolved Redisson client
     * @throws IllegalStateException 当没有专用客户端且普通客户端不唯一时；when no dedicated
     *                               client exists and the general client is ambiguous
     */
    private RedissonClient identityStateRedisson(
            ObjectProvider<RedissonClient> clients,
            ListableBeanFactory beanFactory) {
        if (beanFactory.containsBean("rbac3RuntimeRedissonClient")) {
            return beanFactory.getBean(
                    "rbac3RuntimeRedissonClient", RedissonClient.class);
        }
        RedissonClient unique = clients.getIfUnique();
        if (unique == null) {
            throw new IllegalStateException(
                    "IdP user-state Redis client is ambiguous");
        }
        return unique;
    }
}
