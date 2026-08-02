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

@AutoConfiguration
@EnableConfigurationProperties(IdpStarterProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.idp",
        name = "enabled",
        havingValue = "true")
public class IdpStarterAutoConfiguration {

    @Bean(name = "idpJwtDecoder")
    @ConditionalOnMissingBean(name = "idpJwtDecoder")
    public JwtDecoder idpJwtDecoder(IdpStarterProperties properties) {
        properties.validate();
        return new RetryingJwtDecoder(() -> decoder(properties));
    }

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

    @Bean
    @ConditionalOnBean(IdpJwtVerifier.class)
    @ConditionalOnMissingBean
    public IdpBearerAuthenticationFilter idpBearerAuthenticationFilter(
            IdpJwtVerifier verifier,
            ObjectMapper objectMapper
    ) {
        return new IdpBearerAuthenticationFilter(verifier, objectMapper);
    }

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
