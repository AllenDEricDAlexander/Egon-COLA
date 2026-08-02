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

@AutoConfiguration
@EnableConfigurationProperties(IdpGatewayAdapterProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.idp.gateway",
        name = "enabled",
        havingValue = "true")
@Import(IdpGatewayRedissonConfiguration.class)
public class IdpGatewayAdapterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdpReservedHeaderSanitizer idpReservedHeaderSanitizer() {
        return new IdpReservedHeaderSanitizer();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdpBearerCredentialExtractor idpBearerCredentialExtractor(
            IdpReservedHeaderSanitizer sanitizer
    ) {
        return new IdpBearerCredentialExtractor(sanitizer);
    }

    @Bean(name = "idpGatewayJwtDecoder")
    @ConditionalOnMissingBean(name = "idpGatewayJwtDecoder")
    public JwtDecoder idpGatewayJwtDecoder(
            IdpGatewayAdapterProperties properties
    ) {
        properties.validate();
        return new RetryingJwtDecoder(() -> decoder(properties));
    }

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

    @Bean
    @ConditionalOnBean(name = "idpGatewaySharedJwtVerifier")
    @ConditionalOnMissingBean
    public IdpGatewayJwtVerifier idpGatewayJwtVerifier(
            @Qualifier("idpGatewaySharedJwtVerifier") IdpJwtVerifier verifier
    ) {
        return new IdpGatewayJwtVerifier(verifier);
    }

    @Bean
    @ConditionalOnBean(IdpGatewayJwtVerifier.class)
    @ConditionalOnMissingBean
    public IdpIdentityAuthenticationProvider
            idpIdentityAuthenticationProvider(
                    IdpGatewayJwtVerifier verifier
            ) {
        return new IdpIdentityAuthenticationProvider(verifier);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdpTrustedIdentityMapper idpTrustedIdentityMapper() {
        return new IdpTrustedIdentityMapper();
    }

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
