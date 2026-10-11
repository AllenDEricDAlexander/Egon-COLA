package top.egon.cola.platform.tianquan.jianshen.yuheng.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpObservedVerificationComponent;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import top.egon.cola.platform.tianquan.jianshen.core.runtime.Rbac3RuntimeKeyFactory;
import top.egon.cola.platform.tianquan.jianshen.yuheng.runtime.Rbac3GatewayRedissonConfiguration;
import top.egon.cola.platform.tianquan.jianshen.yuheng.runtime.Rbac3GatewayScopeSnapshotReader;
import top.egon.cola.platform.tianquan.jianshen.yuheng.security.Rbac3BizAppScopeAuthorizationProvider;
import top.egon.cola.platform.tianquan.jianshen.yuheng.security.Rbac3ReservedHeaderSanitizer;

import java.time.Clock;

/**
 * Wires the Tianquan-Jianshen Gateway authorization adapter.
 *
 * <p>Authentication is deliberately absent here. The Tianquan-Shoubing Gateway adapter
 * authenticates the Tianquan-Shoubing-issued access token and supplies a
 * {@code GatewayPrincipal}; this adapter only reads Tianquan-Jianshen's published runtime
 * authorization scope projection and evaluates the target BIZ and APP.</p>
 */
@Slf4j
@AutoConfiguration("rbac3GatewayAdapterAutoConfiguration")
@EnableConfigurationProperties(Rbac3GatewayAdapterProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.platform.tianquan.jianshen.yuheng",
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

    @Bean(name = "rbac3BizAppScopeAuthorizationProvider")
    @ConditionalOnBean(Rbac3GatewayScopeSnapshotReader.class)
    @ConditionalOnMissingBean
    public Rbac3BizAppScopeAuthorizationProvider
            rbac3BizAppScopeAuthorizationProvider(
                    @Qualifier("rbac3GatewayScopeSnapshotReader") Rbac3GatewayScopeSnapshotReader runtime,
                    @Qualifier("idpObservedVerificationComponent") ObjectProvider<IdpObservedVerificationComponent> observations,
                    @Qualifier("idpSecurityMetricsRecorder") ObjectProvider<IdpSecurityMetricsRecorder> metrics,
                    @Qualifier("defaultValidator") ObjectProvider<jakarta.validation.Validator> validators
            ) {
        var observer = observations.getIfAvailable();
        if (observer == null) { return new Rbac3BizAppScopeAuthorizationProvider(runtime::authorize); }
        return new Rbac3BizAppScopeAuthorizationProvider(runtime::authorize, observer,
                metrics.getObject(), validators.getObject());
    }
}
