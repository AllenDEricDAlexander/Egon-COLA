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
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import top.egon.cola.platform.idp.starter.client.IdpClientCredentialsRequestEntityConverter;
import top.egon.cola.platform.idp.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.idp.starter.security.IdpEndpointAuthenticationPolicy;
import top.egon.cola.platform.idp.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.idp.starter.security.ServiceAccessTokenVerifier;
import top.egon.cola.platform.idp.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.idp.starter.security.RetryingJwtDecoder;
import top.egon.cola.platform.idp.starter.security.ServiceScopeAuthorization;
import top.egon.cola.platform.idp.starter.security.rpc.IdpRpcBearerServerInterceptor;
import top.egon.cola.platform.idp.starter.security.rpc.IdpRpcClientCredentialInterceptorFactory;
import top.egon.cola.platform.idp.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerStateReader;
import top.egon.cola.platform.idp.starter.state.RedisIdentityOAuthClientStateReader;
import top.egon.cola.platform.idp.starter.state.RedisIdentityResourceServerStateReader;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * 为普通 Servlet 资源服务器装配统一 IdP 身份验证能力。
 * 本配置创建 Spring OAuth2 Client SERVICE facade、JWT 解码器、用户实时状态读取器、身份验证器与
 * Bearer 过滤器；它不签发 OAuth Access Token，也不执行接口权限判断。
 *
 * <p>Auto-configures unified IdP identity verification for regular Servlet resource servers.
 * It creates the OAuth2 Client facade, JWT decoder, service-state readers, identity
 * verifier, and Bearer filter. It neither issues OAuth access tokens nor makes endpoint
 * authorization decisions.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties({
        IdpStarterProperties.class
})
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

    /** Creates the request converter used by the standard client-credentials provider. */
    @Bean
    @ConditionalOnBean(ClientRegistrationRepository.class)
    @ConditionalOnMissingBean
    public IdpClientCredentialsRequestEntityConverter
            idpClientCredentialsRequestEntityConverter() {
        return new IdpClientCredentialsRequestEntityConverter();
    }

    /** Provides the default in-memory authorized-client store for service tokens. */
    @Bean
    @ConditionalOnBean(ClientRegistrationRepository.class)
    @ConditionalOnMissingBean
    public OAuth2AuthorizedClientService idpOAuth2AuthorizedClientService(
            ClientRegistrationRepository registrations
    ) {
        return new InMemoryOAuth2AuthorizedClientService(registrations);
    }

    /**
     * Configures Spring's client-credentials provider with the IdP extension converter.
     */
    @Bean
    @ConditionalOnBean({
            ClientRegistrationRepository.class,
            OAuth2AuthorizedClientService.class
    })
    @ConditionalOnMissingBean(OAuth2AuthorizedClientManager.class)
    @ConditionalOnProperty(
            prefix = "egon.cola.platform.idp.service-client",
            name = {"app-id", "registration-id"}
    )
    public OAuth2AuthorizedClientManager idpOAuth2AuthorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService authorizedClients,
            IdpClientCredentialsRequestEntityConverter converter,
            IdpStarterProperties properties
    ) {
        properties.getServiceClient().validate();
        RestClientClientCredentialsTokenResponseClient responseClient =
                new RestClientClientCredentialsTokenResponseClient();
        responseClient.setHeadersConverter(converter::convertHeaders);
        responseClient.setParametersConverter(converter::convertParameters);
        ClientCredentialsOAuth2AuthorizedClientProvider provider =
                new ClientCredentialsOAuth2AuthorizedClientProvider();
        provider.setClockSkew(
                properties.getServiceClient().getRenewalSkew()
        );
        provider.setAccessTokenResponseClient(responseClient);
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        registrations,
                        authorizedClients
                );
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    /** Creates the one recommended SERVICE-token API for biz services. */
    @Bean
    @ConditionalOnBean(OAuth2AuthorizedClientManager.class)
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "egon.cola.platform.idp.service-client",
            name = {"app-id", "registration-id"}
    )
    public IdpServiceOAuth2Client idpServiceOAuth2Client(
            OAuth2AuthorizedClientManager manager,
            IdpClientCredentialsRequestEntityConverter converter,
            IdpStarterProperties properties
    ) {
        properties.getServiceClient().validate();
        return new IdpServiceOAuth2Client(
                manager,
                converter,
                Clock.systemUTC(),
                properties.getServiceClient().getRenewalSkew()
        );
    }

    /** Fails explicitly when retired private-key/Admission settings remain configured. */
    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.platform.idp",
            name = "enabled",
            havingValue = "true"
    )
    public Object idpLegacyConfigurationMigrationGuard(
            Environment environment
    ) {
        List<String> retired = List.of(
                "egon.cola.platform.idp.admission.private-key-path",
                "egon.cola.platform.idp.admission.management-client-id",
                "egon.cola.platform.idp.admission.rpc-target",
                "egon.idp.rbac3.service-token.private-key-file",
                "egon.idp.rbac3.service-token.key-id",
                "egon.idp.oauth.client-assertion-key-prefix"
        );
        List<String> configured = retired.stream()
                .filter(environment::containsProperty)
                .toList();
        if (!configured.isEmpty()) {
            throw new IllegalStateException(
                    "IdP OAuth2 Client migration required; retired properties configured: "
                            + String.join(", ", configured)
            );
        }
        return new Object();
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
     * 创建从共享 Redis 键空间读取 Resource Server 运行态投影的端口实现。
     *
     * <p>Creates the port implementation that reads Resource Server runtime projections from the
     * shared Redis key space.</p>
     *
     * @param redissonClients 容器中的 Redisson 客户端候选；Redisson client candidates
     * @param beanFactory 用于按名称选择客户端的 Bean 工厂；bean factory used for named lookup
     * @param objectMapper Resource 投影 JSON 反序列化器；Resource projection JSON mapper
     * @param properties IdP Starter 配置；IdP Starter settings
     * @return Resource Server 状态读取器；Resource Server state reader
     */
    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean
    public IdentityResourceServerStateReader
            identityResourceServerStateReader(
                    ObjectProvider<RedissonClient> redissonClients,
                    ListableBeanFactory beanFactory,
                    ObjectMapper objectMapper,
                    IdpStarterProperties properties
            ) {
        properties.validate();
        return new RedisIdentityResourceServerStateReader(
                identityStateRedisson(redissonClients, beanFactory),
                objectMapper,
                properties.getResourceStateKeyPrefix()
        );
    }

    /**
     * 创建从共享 Redis 键空间读取 OAuth Client 运行态投影的端口实现。
     *
     * <p>Creates the port implementation that reads OAuth Client runtime projections from the
     * shared Redis key space.</p>
     *
     * @param redissonClients 容器中的 Redisson 客户端候选；Redisson client candidates
     * @param beanFactory 用于按名称选择客户端的 Bean 工厂；bean factory used for named lookup
     * @param objectMapper Client 投影 JSON 反序列化器；Client projection JSON mapper
     * @param properties IdP Starter 配置；IdP Starter settings
     * @return OAuth Client 状态读取器；OAuth Client state reader
     */
    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean
    public IdentityOAuthClientStateReader identityOAuthClientStateReader(
            ObjectProvider<RedissonClient> redissonClients,
            ListableBeanFactory beanFactory,
            ObjectMapper objectMapper,
            IdpStarterProperties properties
    ) {
        properties.validate();
        return new RedisIdentityOAuthClientStateReader(
                identityStateRedisson(redissonClients, beanFactory),
                objectMapper,
                properties.getOauthClientStateKeyPrefix()
        );
    }

    /**
     * 创建共享的 IdP 访问令牌验证器。
     *
     * <p>Creates the shared IdP access-token verifier.</p>
     *
     * @param decoder JWT 解码器；JWT decoder
     * @param resourceStates Resource Server 状态读取器；Resource Server state reader
     * @param clientStates OAuth Client 状态读取器；OAuth Client state reader
     * @param properties IdP Starter 配置；IdP Starter settings
     * @return IdP JWT 验证器；IdP JWT verifier
     */
    @Bean
    @ConditionalOnBean({
            IdentityResourceServerStateReader.class,
            IdentityOAuthClientStateReader.class
    })
    @ConditionalOnMissingBean
    public IdpJwtVerifier idpJwtVerifier(
            @Qualifier("idpJwtDecoder") JwtDecoder decoder,
            IdentityResourceServerStateReader resourceStates,
            IdentityOAuthClientStateReader clientStates,
            IdpStarterProperties properties
    ) {
        properties.validate();
        return new IdpJwtVerifier(
                decoder,
                resourceStates,
                clientStates,
                properties.getResourceServerId(),
                properties.getResourceUri(),
                properties.getPlatformAudience(),
                Clock.systemUTC());
    }

    /**
     * Exposes the stateless USER-token verifier to internal subject-token adapters.
     */
    @Bean
    @ConditionalOnBean(IdpJwtVerifier.class)
    @ConditionalOnMissingBean
    public UserAccessTokenVerifier userAccessTokenVerifier(IdpJwtVerifier verifier) {
        return new UserAccessTokenVerifier(verifier);
    }

    /**
     * Relays an already verified USER token from HTTP or RPC request context to downstream RPC.
     *
     * @return trusted RPC USER credential relay
     */
    @Bean
    @ConditionalOnBean(UserAccessTokenVerifier.class)
    @ConditionalOnMissingBean
    public IdpRpcClientCredentialInterceptorFactory
            idpRpcClientCredentialInterceptorFactory() {
        return new IdpRpcClientCredentialInterceptorFactory();
    }

    /**
     * Verifies an optional USER Bearer credential on RPC provider calls.
     *
     * @param verifier shared USER access-token verifier
     * @return RPC USER Bearer server interceptor
     */
    @Bean
    @ConditionalOnBean(UserAccessTokenVerifier.class)
    @ConditionalOnMissingBean
    public IdpRpcBearerServerInterceptor idpRpcBearerServerInterceptor(
            UserAccessTokenVerifier verifier
    ) {
        return new IdpRpcBearerServerInterceptor(verifier);
    }

    /**
     * Exposes the explicit SERVICE-token verifier used by SERVICE-only endpoint policies.
     */
    @Bean
    @ConditionalOnBean(IdpJwtVerifier.class)
    @ConditionalOnMissingBean
    public ServiceAccessTokenVerifier serviceAccessTokenVerifier(IdpJwtVerifier verifier) {
        return new ServiceAccessTokenVerifier(verifier);
    }

    /**
     * Provides a fail-closed-capable default endpoint policy for resource applications.
     */
    @Bean
    @ConditionalOnMissingBean
    public IdpEndpointAuthenticationPolicy idpEndpointAuthenticationPolicy() {
        return new IdpEndpointAuthenticationPolicy();
    }

    /**
     * 创建只读取 IdP SERVICE Token Scope 的本地授权判断器。
     *
     * <p>Creates the local authorization evaluator that reads only scopes from IdP SERVICE
     * tokens.</p>
     *
     * @return SERVICE Scope 判断器；SERVICE scope evaluator
     */
    @Bean
    @ConditionalOnMissingBean
    public ServiceScopeAuthorization serviceScopeAuthorization() {
        return new ServiceScopeAuthorization();
    }

    /**
     * 创建把已验证身份写入 Spring Security 上下文的 Bearer 过滤器。
     *
     * <p>Creates the Bearer filter that stores a verified identity in the Spring Security
     * context.</p>
     *
     * @param userAccessTokenVerifier USER access-token verifier
     * @param serviceAccessTokenVerifier SERVICE access-token verifier
     * @param endpointAuthenticationPolicy endpoint credential policy
     * @param objectMapper 认证失败响应的 JSON 序列化器；JSON mapper for authentication failures
     * @return IdP Bearer 身份过滤器；IdP Bearer identity filter
     */
    @Bean
    @ConditionalOnBean(IdpJwtVerifier.class)
    @ConditionalOnMissingBean
    public IdpBearerAuthenticationFilter idpBearerAuthenticationFilter(
            UserAccessTokenVerifier userAccessTokenVerifier,
            ServiceAccessTokenVerifier serviceAccessTokenVerifier,
            IdpEndpointAuthenticationPolicy endpointAuthenticationPolicy,
            ObjectMapper objectMapper
    ) {
        return new IdpBearerAuthenticationFilter(
                userAccessTokenVerifier,
                serviceAccessTokenVerifier,
                endpointAuthenticationPolicy,
                objectMapper);
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
     * 根据 JWK Set 地址构造 Nimbus 解码器，并校验签发方和时间窗口。
     *
     * <p>Builds a Nimbus decoder from the JWK Set endpoint and validates issuer and time claims.
     * USER and SERVICE verifiers apply their own exact audience policy after decoding.</p>
     *
     * @param properties 已完成校验的 IdP Starter 配置；validated IdP Starter settings
     * @return 配置完成的 JWT 解码器；configured JWT decoder
     */
    private JwtDecoder decoder(IdpStarterProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(
                        properties.getJwkSetUri().trim())
                .validateType(false)
                .build();
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefaultWithIssuer(
                properties.getIssuer().trim()));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    /**
     * 选择读取 IdP Resource/Client 运行态所用的 Redisson 客户端。
     *
     * <p>Selects the Redisson client used to read IdP Resource/Client runtime state.</p>
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
                    "IdP service-state Redis client is ambiguous");
        }
        return unique;
    }
}
