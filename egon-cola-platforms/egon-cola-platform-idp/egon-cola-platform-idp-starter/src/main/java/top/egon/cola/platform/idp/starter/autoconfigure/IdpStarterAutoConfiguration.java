package top.egon.cola.platform.idp.starter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import top.egon.cola.component.ddc.api.extension.DdcAdmissionTicketSupplier;
import top.egon.cola.component.ddc.model.admission.DdcAdmissionRequest;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.config.RpcTransportSecurity;
import top.egon.cola.component.rpc.consumer.direct.RpcDirectClientFactory;
import top.egon.cola.component.rpc.consumer.direct.RpcDirectClientHandle;
import top.egon.cola.component.rpc.consumer.direct.RpcDirectClientSettings;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentityFactory;
import top.egon.cola.platform.idp.rpc.contract.ResourceServerAdmissionRpc;
import top.egon.cola.platform.idp.starter.admission.CachingDdcAdmissionTicketSupplier;
import top.egon.cola.platform.idp.starter.admission.OwnerOnlyPrivateKeyLoader;
import top.egon.cola.platform.idp.starter.admission.PrivateKeyJwtAssertionFactory;
import top.egon.cola.platform.idp.starter.admission.RpcResourceServerAdmissionClient;
import top.egon.cola.platform.idp.starter.security.IdpBearerAuthenticationFilter;
import top.egon.cola.platform.idp.starter.security.IdpEndpointAuthenticationPolicy;
import top.egon.cola.platform.idp.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.idp.starter.security.ServiceAccessTokenVerifier;
import top.egon.cola.platform.idp.starter.security.UserAccessTokenVerifier;
import top.egon.cola.platform.idp.starter.security.RetryingJwtDecoder;
import top.egon.cola.platform.idp.starter.security.ServiceScopeAuthorization;
import top.egon.cola.platform.idp.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerStateReader;
import top.egon.cola.platform.idp.starter.state.RedisIdentityOAuthClientStateReader;
import top.egon.cola.platform.idp.starter.state.RedisIdentityResourceServerStateReader;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * 为普通 Servlet 资源服务器装配统一 IdP 身份验证能力。
 * 本配置创建 Admission Ticket 供应器、JWT 解码器、用户实时状态读取器、身份验证器与
 * Bearer 过滤器；它不签发 OAuth Access Token，也不执行接口权限判断。
 *
 * <p>Auto-configures unified IdP identity verification for regular Servlet resource servers.
 * It creates the Admission Ticket supplier, JWT decoder, service-state readers, identity
 * verifier, and Bearer filter. It neither issues OAuth access tokens nor makes endpoint
 * authorization decisions.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties({
        IdpStarterProperties.class,
        EgonRpcProperties.class
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

    /**
     * 创建 owner-only 私钥、RPC Audience 绑定 Assertion 和静态直连 Egon-RPC 客户端。
     * 应用只能通过显式注入另一个 {@link DdcAdmissionTicketSupplier} 替换生产实现，普通配置中
     * 不提供关闭准入的开关。
     *
     * <p>Creates the statically targeted Egon-RPC client from an owner-only private key and
     * RPC-audience-bound assertions. Applications may replace the production implementation only
     * by explicitly providing another {@link DdcAdmissionTicketSupplier}; no ordinary
     * configuration switch disables admission.</p>
     *
     * @param properties IdP Starter 配置；IdP Starter settings
     * @param rpcProperties Egon-RPC 进程身份和传输安全配置；Egon-RPC process identity and
     *                      transport-security settings
     * @param environment Spring 运行环境；Spring environment
     * @return IdP Resource Server 准入 RPC 客户端；IdP Resource Server admission RPC client
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean({
            DdcAdmissionTicketSupplier.class,
            RpcResourceServerAdmissionClient.class
    })
    @Conditional(DdcAdmissionRequiredCondition.class)
    public RpcResourceServerAdmissionClient
            rpcResourceServerAdmissionClient(
                    IdpStarterProperties properties,
                    EgonRpcProperties rpcProperties,
                    Environment environment
    ) {
        properties.validate();
        properties.validateAdmission();
        IdpStarterProperties.Admission admission =
                properties.getAdmission();
        Clock clock = Clock.systemUTC();
        PrivateKeyJwtAssertionFactory assertions =
                new PrivateKeyJwtAssertionFactory(
                        admission.getManagementClientId(),
                        admission.getKid(),
                        ResourceServerAdmissionRpc.AUDIENCE,
                        new OwnerOnlyPrivateKeyLoader().load(
                                admission.getPrivateKeyPath()),
                        clock,
                        new SecureRandom()
                );
        EgonRpcProperties.Tls tls = rpcProperties.getTls();
        RpcTransportSecurity security = new RpcTransportSecurity(
                tls.isEnabled(),
                tls.isDevelopmentPlaintext(),
                tls.getCertificateChainPath(),
                tls.getPrivateKeyPath(),
                tls.getTrustCertificateCollectionPath()
        );
        RpcProcessIdentity processIdentity = new RpcProcessIdentityFactory(
                environment,
                rpcProperties
        ).create();
        RpcDirectClientHandle<ResourceServerAdmissionRpc> handle =
                new RpcDirectClientFactory().create(
                        ResourceServerAdmissionRpc.class,
                        RpcDirectClientSettings.defaults(
                                admission.getRpcTarget(),
                                processIdentity,
                                security,
                                admission.getRpcTimeout().toMillis()
                        ),
                        List.of()
                );
        return new RpcResourceServerAdmissionClient(
                handle,
                properties.getIssuer(),
                assertions
        );
    }

    /**
     * 创建带提前续签和未过期回退语义的 DDC 准入票据供应器。
     *
     * <p>Creates the DDC Admission Ticket supplier with renewal-ahead and unexpired-fallback
     * semantics.</p>
     *
     * @param properties IdP Starter 配置；IdP Starter settings
     * @param client IdP Resource Server 准入 RPC 客户端；IdP Resource Server admission RPC
     *               client
     * @return DDC Admission Ticket 供应器；DDC Admission Ticket supplier
     */
    @Bean
    @ConditionalOnBean(RpcResourceServerAdmissionClient.class)
    @ConditionalOnMissingBean(DdcAdmissionTicketSupplier.class)
    @Conditional(DdcAdmissionRequiredCondition.class)
    public DdcAdmissionTicketSupplier ddcAdmissionTicketSupplier(
            IdpStarterProperties properties,
            RpcResourceServerAdmissionClient client
    ) {
        IdpStarterProperties.Admission admission = properties.getAdmission();
        DdcAdmissionRequest expectedRequest = new DdcAdmissionRequest(
                properties.getResourceServerId(),
                properties.getResourceUri(),
                admission.getBizCode(),
                admission.getAppCode(),
                admission.getEnvironment(),
                admission.getInstanceId()
        );
        return new CachingDdcAdmissionTicketSupplier(
                client,
                expectedRequest,
                admission.getRenewalSkew(),
                Clock.systemUTC()
        );
    }

    /**
     * 仅在 DDC 配置客户端或注册中心参与运行时装配准入票据供应器。
     *
     * <p>Creates the admission-ticket supplier only when either the DDC configuration client or
     * service registry participates in the application runtime.</p>
     */
    static final class DdcAdmissionRequiredCondition
            extends AnyNestedCondition {

        /**
         * 创建按 Bean 注册阶段判断的任一条件组合。
         *
         * <p>Creates the any-match condition evaluated during bean registration.</p>
         */
        DdcAdmissionRequiredCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        /**
         * 匹配启用 DDC 配置客户端的应用。
         *
         * <p>Matches applications with the DDC configuration client enabled.</p>
         */
        @ConditionalOnProperty(
                prefix = "egon.cola.component.ddc",
                name = "enabled",
                havingValue = "true")
        static final class DdcConfigurationClientEnabled {
        }

        /**
         * 匹配启用 DDC 服务注册中心的应用。
         *
         * <p>Matches applications with the DDC service registry enabled.</p>
         */
        @ConditionalOnProperty(
                prefix = "egon.cola.component.ddc.registry",
                name = "enabled",
                havingValue = "true")
        static final class DdcServiceRegistryEnabled {
        }
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
