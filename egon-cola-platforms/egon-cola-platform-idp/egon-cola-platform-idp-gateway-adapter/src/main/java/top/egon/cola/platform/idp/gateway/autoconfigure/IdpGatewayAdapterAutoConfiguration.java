package top.egon.cola.platform.idp.gateway.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
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
import top.egon.cola.platform.idp.gateway.runtime.IdpGatewayRedissonConfiguration;
import top.egon.cola.platform.idp.gateway.security.GatewayResourceServerResolver;
import top.egon.cola.platform.idp.gateway.security.IdpGatewayJwtVerifier;
import top.egon.cola.platform.idp.gateway.security.IdpIdentityAuthenticationProvider;
import top.egon.cola.platform.idp.gateway.security.IdpRefreshClient;
import top.egon.cola.platform.idp.gateway.security.IdpReservedHeaderSanitizer;
import top.egon.cola.platform.idp.gateway.security.IdpTrustedIdentityMapper;
import top.egon.cola.platform.idp.gateway.security.IdpUserCookieCredentialExtractor;
import top.egon.cola.platform.idp.gateway.security.IdpUserCredentialRecoveryProvider;
import top.egon.cola.platform.idp.gateway.security.ReactorNettyIdpRefreshClient;
import top.egon.cola.platform.idp.starter.security.RetryingJwtDecoder;
import top.egon.cola.platform.idp.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.idp.starter.state.IdentityResourceServerStateReader;
import top.egon.cola.platform.idp.starter.state.RedisIdentityOAuthClientStateReader;
import top.egon.cola.platform.idp.starter.state.RedisIdentityResourceServerStateReader;

import java.time.Duration;

/**
 * 将统一 IdP 身份验证能力装配到非 Servlet 的 Gateway 安全扩展点。
 * 本配置串联凭据提取、保留头清理、共享 JWT 与用户状态校验、网关认证以及可信身份映射；
 * 它只确认调用者身份，不决定路由是否有权访问。
 *
 * <p>Auto-configures unified IdP identity verification for the non-Servlet Gateway security SPI.
 * It connects credential extraction, reserved-header sanitization, shared JWT and user-state
 * verification, Gateway authentication, and trusted-identity mapping. It confirms caller identity
 * only and does not decide whether that identity may access a route.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(IdpGatewayAdapterProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.idp.gateway",
        name = "enabled",
        havingValue = "true")
@Import(IdpGatewayRedissonConfiguration.class)
public class IdpGatewayAdapterAutoConfiguration {

    /**
     * 创建 Gateway IdP 自动配置实例。
     *
     * <p>Creates the Gateway IdP auto-configuration instance.</p>
     */
    public IdpGatewayAdapterAutoConfiguration() {
    }

    /**
     * 创建需要在身份映射前清除保留请求头的规则对象。
     *
     * <p>Creates the rules that remove reserved request headers before identity mapping.</p>
     *
     * @return IdP 保留头清理器；IdP reserved-header sanitizer
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
     * @return IdP Bearer 凭据提取器；IdP Bearer credential extractor
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
     * @param properties Gateway IdP 适配器配置；Gateway IdP adapter settings
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
     * @param properties Gateway IdP 适配器配置；Gateway IdP adapter settings
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
     * @param properties Gateway IdP 适配器配置；Gateway IdP adapter settings
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
     * 创建从可信路由标识解析 IdP Resource Server 的解析器。
     *
     * <p>Creates the resolver that maps trusted route identity to an IdP Resource Server.</p>
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
                properties.getIdpRefreshUri(),
                properties.getRefreshTokenCookieName(),
                properties.getAccessTokenCookieName(),
                Duration.ofSeconds(2));
    }

    @Bean
    @ConditionalOnMissingBean
    public IdpUserCredentialRecoveryProvider idpUserCredentialRecoveryProvider(
            IdpRefreshClient client,
            IdpGatewayJwtVerifier verifier,
            IdpReservedHeaderSanitizer sanitizer,
            IdpGatewayAdapterProperties properties) {
        return new IdpUserCredentialRecoveryProvider(
                client,
                verifier,
                sanitizer,
                properties.getRefreshTokenCookieName(),
                properties.getAccessTokenCookieName());
    }

    /**
     * 创建 Gateway 身份认证提供者。
     *
     * <p>Creates the Gateway identity authentication provider.</p>
     *
     * @param verifier Gateway JWT 验证端口；Gateway JWT verification port
     * @return IdP 身份认证提供者；IdP identity authentication provider
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
     * @return IdP 可信身份映射器；IdP trusted-identity mapper
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
     * @param properties 已完成校验的 Gateway IdP 配置；validated Gateway IdP settings
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
}
