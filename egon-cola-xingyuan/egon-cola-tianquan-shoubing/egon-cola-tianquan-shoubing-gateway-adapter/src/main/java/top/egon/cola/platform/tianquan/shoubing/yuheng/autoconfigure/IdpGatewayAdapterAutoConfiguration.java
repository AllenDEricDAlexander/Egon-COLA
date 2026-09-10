package top.egon.cola.platform.tianquan.shoubing.yuheng.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;
import top.egon.cola.platform.tianquan.shoubing.yuheng.runtime.IdpGatewayRedissonConfiguration;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.GatewayResourceServerResolver;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpGatewayJwtVerifier;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpIdentityAuthenticationProvider;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpRefreshClient;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpRefreshTokenStatusClient;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpReservedHeaderSanitizer;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpTrustedIdentityMapper;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpUserCookieCredentialExtractor;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpUserCredentialRecoveryProvider;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpUserOnlineStateProvider;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.ReactorNettyIdpRefreshClient;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.ReactorNettyIdpRefreshTokenStatusClient;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfiguration;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceOAuth2Client;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceTokenRequest;
import top.egon.cola.platform.tianquan.shoubing.starter.security.RetryingJwtDecoder;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.RedisIdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.RedisIdentityResourceServerStateReader;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 将统一 Tianquan-Shoubing 身份验证能力装配到非 Servlet 的 Gateway 安全扩展点。
 * 本配置串联凭据提取、保留头清理、共享 JWT 与用户状态校验、网关认证以及可信身份映射；
 * 它只确认调用者身份，不决定路由是否有权访问。
 *
 * <p>Auto-configures unified Tianquan-Shoubing identity verification for the non-Servlet Gateway security SPI.
 * It connects credential extraction, reserved-header sanitization, shared JWT and user-state
 * verification, Gateway authentication, and trusted-identity mapping. It confirms caller identity
 * only and does not decide whether that identity may access a route.</p>
 */
@AutoConfiguration(before = IdpStarterAutoConfiguration.class)
@EnableConfigurationProperties(IdpGatewayAdapterProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.tianquan.shoubing.yuheng",
        name = "enabled",
        havingValue = "true")
@Import(IdpGatewayRedissonConfiguration.class)
public class IdpGatewayAdapterAutoConfiguration {

    /**
     * 创建 Gateway Tianquan-Shoubing 自动配置实例。
     *
     * <p>Creates the Gateway Tianquan-Shoubing auto-configuration instance.</p>
     */
    public IdpGatewayAdapterAutoConfiguration() {
    }

    /**
     * 创建需要在身份映射前清除保留请求头的规则对象。
     *
     * <p>Creates the rules that remove reserved request headers before identity mapping.</p>
     *
     * @return Tianquan-Shoubing 保留头清理器；Tianquan-Shoubing reserved-header sanitizer
     */
    @Bean
    @ConditionalOnMissingBean
    public IdpReservedHeaderSanitizer idpReservedHeaderSanitizer() {
        return new IdpReservedHeaderSanitizer();
    }

    /**
     * 创建 Gateway Bearer 凭据提取器。
     *
     * <p>Creates the Gateway Bearer credential extractor.</p>
     *
     * @param sanitizer 保留头清理器；reserved-header sanitizer
     * @return Tianquan-Shoubing Bearer 凭据提取器；Tianquan-Shoubing Bearer credential extractor
     */
    @Bean
    @ConditionalOnMissingBean
    public IdpUserCookieCredentialExtractor idpUserCookieCredentialExtractor(
            IdpReservedHeaderSanitizer sanitizer,
            IdpGatewayAdapterProperties properties
    ) {
        properties.validate();
        return new IdpUserCookieCredentialExtractor(
                sanitizer,
                properties.getAccessTokenCookieName(),
                properties.getTrustedOrigins());
    }

    /**
     * 创建支持一次 JWK 刷新的 Gateway 专用 JWT 解码器。
     *
     * <p>Creates the Gateway-specific JWT decoder with one-time JWK refresh support.</p>
     *
     * @param properties Gateway Tianquan-Shoubing 适配器配置；Gateway Tianquan-Shoubing adapter settings
     * @return 名为 {@code idpGatewayJwtDecoder} 的 JWT 解码器；JWT decoder named
     *         {@code idpGatewayJwtDecoder}
     */
    @Bean(name = "idpGatewayJwtDecoder")
    @ConditionalOnMissingBean(name = "idpGatewayJwtDecoder")
    public JwtDecoder idpGatewayJwtDecoder(
            IdpGatewayAdapterProperties properties
    ) {
        properties.validate();
        return new RetryingJwtDecoder(() -> decoder(properties));
    }

    /**
     * 创建使用 Gateway 专用 Redis 客户端的 Resource Server 状态读取器。
     *
     * <p>Creates the Resource Server state reader backed by the Gateway-specific Redis client.</p>
     *
     * @param redisson Gateway 专用 Redis 客户端；Gateway-specific Redis client
     * @param objectMapper Resource 状态 JSON 反序列化器；Resource-state JSON deserializer
     * @param properties Gateway Tianquan-Shoubing 适配器配置；Gateway Tianquan-Shoubing adapter settings
     * @return Resource Server 状态读取器；Resource Server state reader
     */
    @Bean(name = "idpGatewayResourceServerStateReader")
    @ConditionalOnBean(name = "idpGatewayRedissonClient")
    @ConditionalOnMissingBean(name = "idpGatewayResourceServerStateReader")
    public IdentityResourceServerStateReader idpGatewayResourceServerStateReader(
            @Qualifier("idpGatewayRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            IdpGatewayAdapterProperties properties
    ) {
        properties.validate();
        return new RedisIdentityResourceServerStateReader(
                redisson, objectMapper, properties.getResourceStateKeyPrefix());
    }

    /**
     * 创建使用 Gateway 专用 Redis 客户端的 OAuth Client 状态读取器。
     *
     * <p>Creates the OAuth Client state reader backed by the Gateway-specific Redis client.</p>
     *
     * @param redisson Gateway 专用 Redis 客户端；Gateway-specific Redis client
     * @param objectMapper OAuth Client 状态 JSON 反序列化器；OAuth Client-state JSON deserializer
     * @param properties Gateway Tianquan-Shoubing 适配器配置；Gateway Tianquan-Shoubing adapter settings
     * @return OAuth Client 状态读取器；OAuth Client state reader
     */
    @Bean(name = "idpGatewayOAuthClientStateReader")
    @ConditionalOnBean(name = "idpGatewayRedissonClient")
    @ConditionalOnMissingBean(name = "idpGatewayOAuthClientStateReader")
    public IdentityOAuthClientStateReader idpGatewayOAuthClientStateReader(
            @Qualifier("idpGatewayRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            IdpGatewayAdapterProperties properties
    ) {
        properties.validate();
        return new RedisIdentityOAuthClientStateReader(
                redisson, objectMapper, properties.getClientStateKeyPrefix());
    }

    /**
     * 创建从可信路由标识解析 Tianquan-Shoubing Resource Server 的解析器。
     *
     * <p>Creates the resolver that maps trusted route identity to an Tianquan-Shoubing Resource Server.</p>
     */
    @Bean
    @ConditionalOnBean(name = "idpGatewayRedissonClient")
    @ConditionalOnMissingBean
    public GatewayResourceServerResolver gatewayResourceServerResolver(
            @Qualifier("idpGatewayRedissonClient") RedissonClient redisson,
            @Qualifier("idpGatewayResourceServerStateReader")
            IdentityResourceServerStateReader resourceStates,
            IdpGatewayAdapterProperties properties
    ) {
        properties.validate();
        return new GatewayResourceServerResolver(
                redisson, resourceStates,
                properties.getResourceScopeKeyPrefix(),
                properties.getResourceUriKeyPrefix());
    }

    /**
     * 创建按可信路由动态绑定 Resource 的 Gateway JWT 验证适配器。
     *
     * <p>Creates the Gateway JWT adapter that dynamically binds verification to the trusted
     * route Resource.</p>
     */
    @Bean
    @ConditionalOnBean({GatewayResourceServerResolver.class})
    @ConditionalOnMissingBean
    public IdpGatewayJwtVerifier idpGatewayJwtVerifier(
            @Qualifier("idpGatewayJwtDecoder") JwtDecoder decoder,
            @Qualifier("idpGatewayResourceServerStateReader")
            IdentityResourceServerStateReader resourceStates,
            @Qualifier("idpGatewayOAuthClientStateReader")
            IdentityOAuthClientStateReader clientStates,
            GatewayResourceServerResolver resources,
            IdpGatewayAdapterProperties properties
    ) {
        return new IdpGatewayJwtVerifier(
                decoder,
                resourceStates,
                clientStates,
                resources,
                properties.getPlatformAudience(),
                java.time.Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    public IdpRefreshClient idpRefreshClient(
            IdpGatewayAdapterProperties properties) {
        properties.validate();
        return new ReactorNettyIdpRefreshClient(
                properties.getTianquanShoubingRefreshUri(),
                properties.getRefreshTokenCookieName(),
                properties.getAccessTokenCookieName(),
                Duration.ofSeconds(2));
    }

    @Bean
    @ConditionalOnMissingBean
    public IdpRefreshTokenStatusClient idpRefreshTokenStatusClient(
            IdpGatewayAdapterProperties properties,
            ObjectProvider<IdpServiceOAuth2Client> serviceClients,
            ObjectProvider<IdpStarterProperties> starterProperties,
            ObjectMapper objectMapper) {
        properties.validate();
        Supplier<String> serviceAccessToken = () -> {
            IdpServiceOAuth2Client serviceClient = serviceClients.getIfAvailable();
            IdpStarterProperties starter = starterProperties.getIfAvailable();
            if (serviceClient == null || starter == null) {
                return "";
            }
            IdpStarterProperties.ServiceClient client = starter.getServiceClient();
            client.validate();
            return serviceClient.authorize(new IdpServiceTokenRequest(
                    client.getRegistrationId(),
                    client.getAppId(),
                    properties.getRefreshStatusResourceUri(),
                    ServiceTokenContext.PLATFORM,
                    null,
                    properties.getRefreshStatusScopes()
            )).getTokenValue();
        };
        return new ReactorNettyIdpRefreshTokenStatusClient(
                statusUri(properties.getTianquanShoubingRefreshUri()),
                serviceAccessToken,
                objectMapper,
                Duration.ofMillis(800));
    }

    @Bean
    @ConditionalOnMissingBean
    public IdpUserOnlineStateProvider idpUserOnlineStateProvider(
            IdpRefreshTokenStatusClient client,
            IdpGatewayAdapterProperties properties) {
        return new IdpUserOnlineStateProvider(
                client,
                properties.getRefreshTokenCookieName(),
                properties.getAccessTokenCookieName());
    }

    @Bean
    @ConditionalOnMissingBean
    public IdpUserCredentialRecoveryProvider idpUserCredentialRecoveryProvider(
            IdpRefreshClient client,
            IdpGatewayJwtVerifier verifier,
            IdpReservedHeaderSanitizer sanitizer,
            IdpGatewayAdapterProperties properties,
            IdpUserOnlineStateProvider onlineStateProvider) {
        return new IdpUserCredentialRecoveryProvider(
                client,
                verifier,
                sanitizer,
                properties.getRefreshTokenCookieName(),
                properties.getAccessTokenCookieName(),
                onlineStateProvider);
    }

    /**
     * 创建 Gateway 身份认证提供者。
     *
     * <p>Creates the Gateway identity authentication provider.</p>
     *
     * @param verifier Gateway JWT 验证端口；Gateway JWT verification port
     * @return Tianquan-Shoubing 身份认证提供者；Tianquan-Shoubing identity authentication provider
     */
    @Bean
    @ConditionalOnBean(IdpGatewayJwtVerifier.class)
    @ConditionalOnMissingBean
    public IdpIdentityAuthenticationProvider
            idpIdentityAuthenticationProvider(
                    IdpGatewayJwtVerifier verifier
            ) {
        return new IdpIdentityAuthenticationProvider(verifier);
    }

    /**
     * 创建把验证结果映射为后端可信身份头的映射器。
     *
     * <p>Creates the mapper that turns verified identity into trusted downstream headers.</p>
     *
     * @return Tianquan-Shoubing 可信身份映射器；Tianquan-Shoubing trusted-identity mapper
     */
    @Bean
    @ConditionalOnMissingBean
    public IdpTrustedIdentityMapper idpTrustedIdentityMapper() {
        return new IdpTrustedIdentityMapper();
    }

    /**
     * 根据 JWK Set 地址创建 Nimbus 解码器，并校验签发方。
     *
     * <p>Creates a Nimbus decoder from the JWK Set endpoint and validates the issuer. The exact
     * audience is validated dynamically against the trusted route Resource.</p>
     *
     * @param properties 已完成校验的 Gateway Tianquan-Shoubing 配置；validated Gateway Tianquan-Shoubing settings
     * @return 配置完成的 JWT 解码器；configured JWT decoder
     */
    private JwtDecoder decoder(IdpGatewayAdapterProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(
                        properties.getJwkSetUri().trim())
                .validateType(false)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(
                properties.getIssuer().trim()));
        return decoder;
    }

    private String statusUri(String refreshUri) {
        try {
            return java.net.URI.create(refreshUri)
                    .resolve("/internal/v1/oauth2/refresh-token/validate")
                    .toString();
        } catch (RuntimeException exception) {
            throw new IllegalStateException("invalid Tianquan-Shoubing refresh URI", exception);
        }
    }
}
