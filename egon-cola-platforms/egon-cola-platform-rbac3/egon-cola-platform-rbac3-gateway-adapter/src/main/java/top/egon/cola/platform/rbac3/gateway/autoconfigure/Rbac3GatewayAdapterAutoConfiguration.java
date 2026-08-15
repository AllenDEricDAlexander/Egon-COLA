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
import top.egon.cola.platform.rbac3.gateway.runtime.Rbac3GatewayScopeSnapshotReader;
import top.egon.cola.platform.rbac3.gateway.security.Rbac3BizAppScopeAuthorizationProvider;
import top.egon.cola.platform.rbac3.gateway.security.Rbac3ReservedHeaderSanitizer;

import java.time.Clock;

/**
 * Wires the RBAC3 Gateway authorization adapter.
 *
 * <p>Authentication is deliberately absent here. The IdP Gateway adapter
 * authenticates the IdP-issued access token and supplies a
 * {@code GatewayPrincipal}; this adapter only reads RBAC3's published runtime
 * authorization scope projection and evaluates the target BIZ and APP.</p>
 */
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
    @ConditionalOnBean(name = "rbac3RuntimeRedissonClient")
    @ConditionalOnMissingBean
    public Rbac3GatewayScopeSnapshotReader rbac3GatewayScopeSnapshotReader(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3RuntimeKeyFactory keyFactory,
            @Qualifier("rbac3GatewayClock") Clock clock
    ) {
        return new Rbac3GatewayScopeSnapshotReader(
                redisson, objectMapper, keyFactory, clock);
    }

    @Bean
    @ConditionalOnBean(Rbac3GatewayScopeSnapshotReader.class)
    @ConditionalOnMissingBean
    public Rbac3BizAppScopeAuthorizationProvider
            rbac3BizAppScopeAuthorizationProvider(
                    Rbac3GatewayScopeSnapshotReader runtime
            ) {
        return new Rbac3BizAppScopeAuthorizationProvider(runtime::authorize);
    }
}
