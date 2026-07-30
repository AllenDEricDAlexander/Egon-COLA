package top.egon.cola.platform.rbac3.gateway.autoconfigure;

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
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;
import top.egon.cola.platform.rbac3.gateway.runtime.Rbac3GatewayRedissonConfiguration;
import top.egon.cola.platform.rbac3.gateway.runtime.Rbac3GatewayRuntimeSnapshotReader;
import top.egon.cola.platform.rbac3.gateway.security.Rbac3BearerCredentialExtractor;
import top.egon.cola.platform.rbac3.gateway.security.Rbac3GatewayJwtVerifier;
import top.egon.cola.platform.rbac3.gateway.security.Rbac3JwtSessionAuthenticationProvider;
import top.egon.cola.platform.rbac3.gateway.security.Rbac3PermissionAuthorizationProvider;
import top.egon.cola.platform.rbac3.gateway.security.Rbac3ReservedHeaderSanitizer;
import top.egon.cola.platform.rbac3.gateway.security.Rbac3TrustedIdentityMapper;

import java.time.Clock;

@AutoConfiguration
@EnableConfigurationProperties(Rbac3GatewayAdapterProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.rbac3.gateway",
        name = "enabled",
        havingValue = "true")
@Import(Rbac3GatewayRedissonConfiguration.class)
public class Rbac3GatewayAdapterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "rbac3GatewayClock")
    public Clock rbac3GatewayClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    public Rbac3RuntimeKeyFactory rbac3GatewayRuntimeKeyFactory() {
        return new Rbac3RuntimeKeyFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public Rbac3ReservedHeaderSanitizer rbac3ReservedHeaderSanitizer() {
        return new Rbac3ReservedHeaderSanitizer();
    }

    @Bean
    @ConditionalOnMissingBean
    public Rbac3BearerCredentialExtractor rbac3BearerCredentialExtractor(
            Rbac3ReservedHeaderSanitizer sanitizer
    ) {
        return new Rbac3BearerCredentialExtractor(sanitizer);
    }

    @Bean
    @ConditionalOnBean(name = "rbac3RuntimeRedissonClient")
    @ConditionalOnMissingBean
    public Rbac3GatewayRuntimeSnapshotReader rbac3GatewayRuntimeSnapshotReader(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3RuntimeKeyFactory keyFactory,
            @Qualifier("rbac3GatewayClock") Clock clock
    ) {
        return new Rbac3GatewayRuntimeSnapshotReader(
                redisson, objectMapper, keyFactory, clock);
    }

    @Bean
    @ConditionalOnBean(name = "rbac3RuntimeRedissonClient")
    @ConditionalOnMissingBean
    public Rbac3GatewayJwtVerifier rbac3GatewayJwtVerifier(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3RuntimeKeyFactory keyFactory,
            @Qualifier("rbac3GatewayClock") Clock clock,
            Rbac3GatewayAdapterProperties properties
    ) {
        return new Rbac3GatewayJwtVerifier(
                redisson, objectMapper, keyFactory, clock,
                properties.getIssuer(), properties.getAudience(),
                properties.getClockSkew(), properties.getPublicKeyLkgTtl());
    }

    @Bean
    @ConditionalOnBean({Rbac3GatewayJwtVerifier.class,
            Rbac3GatewayRuntimeSnapshotReader.class})
    @ConditionalOnMissingBean
    public Rbac3JwtSessionAuthenticationProvider
            rbac3JwtSessionAuthenticationProvider(
                    Rbac3GatewayJwtVerifier verifier,
                    Rbac3GatewayRuntimeSnapshotReader runtime
            ) {
        return new Rbac3JwtSessionAuthenticationProvider(
                verifier, runtime::verifySession);
    }

    @Bean
    @ConditionalOnBean(Rbac3GatewayRuntimeSnapshotReader.class)
    @ConditionalOnMissingBean
    public Rbac3PermissionAuthorizationProvider
            rbac3PermissionAuthorizationProvider(
                    Rbac3GatewayRuntimeSnapshotReader runtime
            ) {
        return new Rbac3PermissionAuthorizationProvider(runtime::authorize);
    }

    @Bean
    @ConditionalOnMissingBean
    public Rbac3TrustedIdentityMapper rbac3TrustedIdentityMapper() {
        return new Rbac3TrustedIdentityMapper();
    }
}
