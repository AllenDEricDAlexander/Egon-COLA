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
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import top.egon.cola.platform.idp.gateway.runtime.IdpGatewayRedissonConfiguration;
import top.egon.cola.platform.idp.gateway.security.IdpBearerCredentialExtractor;
import top.egon.cola.platform.idp.gateway.security.IdpGatewayJwtVerifier;
import top.egon.cola.platform.idp.gateway.security.IdpIdentityAuthenticationProvider;
import top.egon.cola.platform.idp.gateway.security.IdpReservedHeaderSanitizer;
import top.egon.cola.platform.idp.gateway.security.IdpTrustedIdentityMapper;
import top.egon.cola.platform.idp.starter.security.IdpJwtVerifier;
import top.egon.cola.platform.idp.starter.security.RetryingJwtDecoder;
import top.egon.cola.platform.idp.starter.state.IdentityUserStateReader;
import top.egon.cola.platform.idp.starter.state.RedisIdentityUserStateReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    public IdpBearerCredentialExtractor idpBearerCredentialExtractor(
            IdpReservedHeaderSanitizer sanitizer
    ) {
        return new IdpBearerCredentialExtractor(sanitizer);
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
     * 创建使用 Gateway 专用 Redis 客户端的用户实时状态读取器。
     *
     * <p>Creates the current user-state reader backed by the Gateway-specific Redis client.</p>
     *
     * @param redisson Gateway 专用 Redis 客户端；Gateway-specific Redis client
     * @param objectMapper 用户状态 JSON 反序列化器；user-state JSON deserializer
     * @param properties Gateway IdP 适配器配置；Gateway IdP adapter settings
     * @return 用户实时状态读取器；current user-state reader
     */
    @Bean(name = "idpGatewayIdentityUserStateReader")
    @ConditionalOnBean(name = "idpGatewayRedissonClient")
    @ConditionalOnMissingBean(name = "idpGatewayIdentityUserStateReader")
    public IdentityUserStateReader idpGatewayIdentityUserStateReader(
            @Qualifier("idpGatewayRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            IdpGatewayAdapterProperties properties
    ) {
        properties.validate();
        return new RedisIdentityUserStateReader(
                redisson,
                objectMapper,
                properties.getUserStateKeyPrefix());
    }

    /**
     * 组合 Gateway JWT 解码器、用户状态读取器和允许范围，创建共享验证器。
     *
     * <p>Combines the Gateway JWT decoder, user-state reader, and accepted scopes into the shared
     * verifier.</p>
     *
     * @param decoder Gateway JWT 解码器；Gateway JWT decoder
     * @param stateReader 用户实时状态读取器；current user-state reader
     * @param properties Gateway IdP 适配器配置；Gateway IdP adapter settings
     * @return 共享 IdP JWT 验证器；shared IdP JWT verifier
     */
    @Bean(name = "idpGatewaySharedJwtVerifier")
    @ConditionalOnBean(name = "idpGatewayIdentityUserStateReader")
    @ConditionalOnMissingBean(name = "idpGatewaySharedJwtVerifier")
    public IdpJwtVerifier idpGatewaySharedJwtVerifier(
            @Qualifier("idpGatewayJwtDecoder") JwtDecoder decoder,
            @Qualifier("idpGatewayIdentityUserStateReader")
            IdentityUserStateReader stateReader,
            IdpGatewayAdapterProperties properties
    ) {
        return new IdpJwtVerifier(
                decoder,
                stateReader,
                properties.getAudiences(),
                properties.getClientIds());
    }

    /**
     * 创建从共享 IdP 验证器到 Gateway 验证端口的适配器。
     *
     * <p>Creates the adapter from the shared IdP verifier to the Gateway verification port.</p>
     *
     * @param verifier 共享 IdP JWT 验证器；shared IdP JWT verifier
     * @return Gateway JWT 验证适配器；Gateway JWT verification adapter
     */
    @Bean
    @ConditionalOnBean(name = "idpGatewaySharedJwtVerifier")
    @ConditionalOnMissingBean
    public IdpGatewayJwtVerifier idpGatewayJwtVerifier(
            @Qualifier("idpGatewaySharedJwtVerifier") IdpJwtVerifier verifier
    ) {
        return new IdpGatewayJwtVerifier(verifier);
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
     * 根据 JWK Set 地址创建 Nimbus 解码器，并校验签发方及受众。
     *
     * <p>Creates a Nimbus decoder from the JWK Set endpoint and validates issuer and audience.</p>
     *
     * @param properties 已完成校验的 Gateway IdP 配置；validated Gateway IdP settings
     * @return 配置完成的 JWT 解码器；configured JWT decoder
     */
    private JwtDecoder decoder(IdpGatewayAdapterProperties properties) {
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
}
