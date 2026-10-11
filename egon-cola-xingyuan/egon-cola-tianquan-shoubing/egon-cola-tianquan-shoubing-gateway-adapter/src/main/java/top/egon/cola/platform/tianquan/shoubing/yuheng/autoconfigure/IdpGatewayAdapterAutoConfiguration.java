package top.egon.cola.platform.tianquan.shoubing.yuheng.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.context.annotation.Configuration;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpSecurityObservationAutoConfiguration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import java.time.Clock;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TenantIssuerAuthenticationManagerComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.VerifiedPrincipalConverter;
import top.egon.cola.platform.tianquan.shoubing.starter.security.LegacyIssuerCutoverPolicy;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import top.egon.cola.platform.tianquan.shoubing.yuheng.runtime.IdpGatewayRedissonConfiguration;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.GatewayResourceServerResolver;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpGatewayJwtVerifier;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpIdentityAuthenticationProvider;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpOAuthScopeAuthorizationProvider;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpReservedHeaderSanitizer;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpTrustedIdentityMapper;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpUserCookieCredentialExtractor;
import top.egon.cola.platform.tianquan.shoubing.yuheng.security.IdpUserCredentialRecoveryProvider;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterAutoConfiguration;
import top.egon.cola.platform.tianquan.shoubing.starter.client.IdpServiceTokenRequest;
import top.egon.cola.platform.tianquan.shoubing.starter.security.RetryingJwtDecoder;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.IdentityResourceServerStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.RedisIdentityOAuthClientStateReader;
import top.egon.cola.platform.tianquan.shoubing.starter.state.RedisIdentityResourceServerStateReader;


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
@Slf4j
@AutoConfiguration(value = "idpGatewayAdapterAutoConfiguration", before = {IdpStarterAutoConfiguration.class, IdpSecurityObservationAutoConfiguration.class})
@EnableConfigurationProperties(IdpGatewayAdapterProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.tianquan.shoubing.yuheng",
        name = "enabled",
        havingValue = "true")
@Import(IdpGatewayRedissonConfiguration.class)
public class IdpGatewayAdapterAutoConfiguration {

    /** OAuth security state and audit use the existing OAuth PRIMARY client, independently of RBAC projection Redis. */
    @Configuration(value = "idpGatewayPrimaryObservationConfiguration", proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "egon.cola.platform.tianquan.shoubing.usage-observation", name = "enabled", havingValue = "true")
    static class PrimaryObservationConfiguration {
        @Bean(name = "idpAuditRedissonClient", destroyMethod = "")
        @ConditionalOnMissingBean(name = "idpAuditRedissonClient")
        RedissonClient auditPrimary(@Qualifier("idpGatewayRedissonClient") RedissonClient primary) { return primary; }

        @Bean(name = "identityResourceServerStateReader")
        @ConditionalOnMissingBean(name = "identityResourceServerStateReader")
        IdentityResourceServerStateReader resourceReader(@Qualifier("idpGatewayResourceServerStateReader") IdentityResourceServerStateReader reader) { return reader; }

        @Bean(name = "identityOAuthClientStateReader")
        @ConditionalOnMissingBean(name = "identityOAuthClientStateReader")
        IdentityOAuthClientStateReader clientReader(@Qualifier("idpGatewayOAuthClientStateReader") IdentityOAuthClientStateReader reader) { return reader; }

        @Bean(name = "oauthRevocationReader")
        @ConditionalOnMissingBean(OAuthRevocationReader.class)
        OAuthRevocationReader revocations(@Qualifier("idpGatewayRedissonClient") RedissonClient primary,
                @Qualifier("trustedIssuerRegistry") top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry issuers,
                @Qualifier("jacksonObjectMapper") ObjectMapper mapper,
                @Qualifier("idpStarterProperties") top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties properties) {
            return new top.egon.cola.platform.tianquan.shoubing.starter.security.impl.RedisOAuthRevocationReader(primary, issuers, mapper, properties);
        }

        @Bean(name = "legacyIssuerCutoverPolicy")
        @ConditionalOnMissingBean(name = "legacyIssuerCutoverPolicy")
        LegacyIssuerCutoverPolicy cutover(@Qualifier("idpGatewayRedissonClient") RedissonClient primary,
                @Qualifier("idpStarterProperties") top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties properties) {
            return new LegacyIssuerCutoverPolicy(primary, properties);
        }
    }

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
        return IdpUserCookieCredentialExtractor.createConfigured(sanitizer);
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
    @Bean("idpGatewayJwtVerifier")
    @ConditionalOnBean({GatewayResourceServerResolver.class})
    @ConditionalOnMissingBean
    public IdpGatewayJwtVerifier idpGatewayJwtVerifier(
            @Qualifier("idpGatewayJwtDecoder") JwtDecoder decoder,
            @Qualifier("idpGatewayResourceServerStateReader")
            IdentityResourceServerStateReader resourceStates,
            @Qualifier("idpGatewayOAuthClientStateReader")
            IdentityOAuthClientStateReader clientStates,
            @Qualifier("gatewayResourceServerResolver") GatewayResourceServerResolver resources,
            @Qualifier("egon.cola.platform.tianquan.shoubing.yuheng-top.egon.cola.platform.tianquan.shoubing.yuheng.autoconfigure.IdpGatewayAdapterProperties")
            IdpGatewayAdapterProperties properties,
            @Qualifier("tenantIssuerAuthenticationManagerComponent") TenantIssuerAuthenticationManagerComponent issuerManager,
            @Qualifier("oauthRevocationReader") ObjectProvider<OAuthRevocationReader> revocationReaders,
            @Qualifier("verifiedPrincipalConverter") VerifiedPrincipalConverter principalConverter,
            @Qualifier("legacyIssuerCutoverPolicy") ObjectProvider<LegacyIssuerCutoverPolicy> legacyIssuerCutoverPolicies,
            @Qualifier("idpClock") Clock clock,
            @Qualifier("idpObservedVerificationComponent") ObjectProvider<IdpObservedVerificationComponent> observations
    ) {
        properties.validate();
        return IdpGatewayJwtVerifier.createConfigured(decoder, resourceStates, clientStates, resources,
                properties.getPlatformAudience(), clock, issuerManager, revocationReaders.getIfAvailable(),
                principalConverter, legacyIssuerCutoverPolicies.getIfAvailable(), observations.getIfAvailable());
    }

    @Bean("idpUserCredentialRecoveryProvider")
    @ConditionalOnMissingBean
    public IdpUserCredentialRecoveryProvider idpUserCredentialRecoveryProvider(
            IdpReservedHeaderSanitizer sanitizer, IdpGatewayAdapterProperties properties) {
        properties.validate();
        return IdpUserCredentialRecoveryProvider.createConfigured(sanitizer,
                properties.getRefreshTokenCookieName(), properties.getAccessTokenCookieName());
    }

    /**
     * 创建 Gateway 身份认证提供者。
     *
     * <p>Creates the Gateway identity authentication provider.</p>
     *
     * @param verifier Gateway JWT 验证端口；Gateway JWT verification port
     * @return Tianquan-Shoubing 身份认证提供者；Tianquan-Shoubing identity authentication provider
     */
    @Bean(name = "idpIdentityAuthenticationProvider")
    @ConditionalOnBean(IdpGatewayJwtVerifier.class)
    @ConditionalOnMissingBean
    public IdpIdentityAuthenticationProvider
            idpIdentityAuthenticationProvider(
                    @Qualifier("idpGatewayJwtVerifier") IdpGatewayJwtVerifier verifier,
                    @Qualifier("defaultValidator") jakarta.validation.Validator validator
            ) {
        return IdpIdentityAuthenticationProvider.createConfigured(verifier, validator);
    }

    @Bean(name = "idpOAuthScopeAuthorizationProvider")
    @ConditionalOnMissingBean(name = "idpOAuthScopeAuthorizationProvider")
    IdpOAuthScopeAuthorizationProvider idpOAuthScopeAuthorizationProvider(
            @Qualifier("idpObservedVerificationComponent") IdpObservedVerificationComponent observations,
            @Qualifier("idpSecurityMetricsRecorder") IdpSecurityMetricsRecorder metrics,
            @Qualifier("defaultValidator") jakarta.validation.Validator validator) {
        return new IdpOAuthScopeAuthorizationProvider(observations, metrics, validator);
    }

    /**
     * 创建把验证结果映射为后端可信身份头的映射器。
     *
     * <p>Creates the mapper that turns verified identity into trusted downstream headers.</p>
     *
     * @return Tianquan-Shoubing 可信身份映射器；Tianquan-Shoubing trusted-identity mapper
     */
    @Bean(name = "idpTrustedIdentityMapper")
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

}
