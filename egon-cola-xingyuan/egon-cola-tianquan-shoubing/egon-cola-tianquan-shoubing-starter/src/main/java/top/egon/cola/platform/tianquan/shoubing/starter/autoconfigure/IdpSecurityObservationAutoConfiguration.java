package top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityMetricsComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.observability.IdpSecurityObservationReadinessComponent;

/** Reuses host metrics and the existing audit Redis; no registry or Redis client is created. */
@Slf4j
@AutoConfiguration("idpSecurityObservationAutoConfiguration")
@AutoConfigureBefore(IdpStarterAutoConfiguration.class)
public class IdpSecurityObservationAutoConfiguration {
    /** The compatibility binding has no destroy callback; the host owns the Redis lifecycle. */
    @Bean(name = "idpAuditRedissonClient", destroyMethod = "")
    @ConditionalOnBean(name = "rbac3RuntimeRedissonClient")
    @ConditionalOnMissingBean(name = "idpAuditRedissonClient")
    RedissonClient idpAuditRedissonClient(@Qualifier("rbac3RuntimeRedissonClient") RedissonClient primary) {
        return primary;
    }

    /** Keeps optional Micrometer types out of contexts where observation is disabled. */
    @Configuration(value = "idpObservationMetricsConfiguration", proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "egon.cola.platform.tianquan.shoubing.usage-observation", name = "enabled", havingValue = "true")
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    static class MetricsConfiguration {
        @Bean(name = "idpMeterRegistrySupplier")
        @ConditionalOnMissingBean(name = "idpMeterRegistrySupplier")
        Supplier<MeterRegistry> idpMeterRegistrySupplier(ObjectProvider<MeterRegistry> registries) {
            registries.getObject();
            return registries::getObject;
        }

        @Bean(name = "idpSecurityMetricsRecorder")
        @ConditionalOnMissingBean(IdpSecurityMetricsRecorder.class)
        IdpSecurityMetricsRecorder idpSecurityMetricsRecorder(
                @Qualifier("idpMeterRegistrySupplier") Supplier<MeterRegistry> registries,
                @Qualifier("defaultValidator") Validator validator,
                @Qualifier("oauthUsageObservationProperties") OAuthUsageObservationProperties usage) {
            new ValidationUtils(validator).validate(usage);
            return new IdpSecurityMetricsComponent(registries, validator, usage);
        }
    }

    @Configuration(value = "idpObservationReadinessConfiguration", proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "egon.cola.platform.tianquan.shoubing.usage-observation", name = "enabled", havingValue = "true")
    static class ReadinessConfiguration {
        @Bean(name = "idpSecurityObservationReadinessComponent")
        IdpSecurityObservationReadinessComponent idpSecurityObservationReadinessComponent(
                @Qualifier("oauthAuditProperties") OAuthAuditProperties audit,
                @Qualifier("defaultValidator") Validator validator,
                @Qualifier("idpMeterRegistrySupplier") Supplier<?> registry,
                @Qualifier("idpSecurityMetricsRecorder") IdpSecurityMetricsRecorder metrics) {
            registry.get();
            Objects.requireNonNull(metrics, "metrics");
            return new IdpSecurityObservationReadinessComponent(audit, validator);
        }

        /** Producer preparation precedes the lease; the recorder never depends on the producer. */
        @Bean(name = {"idpAuditSpoolLease", "tianquanAuditSpoolLease"}, destroyMethod = "close")
        FileChannel idpAuditSpoolLease(
                @Qualifier("idpSecurityObservationReadinessComponent") IdpSecurityObservationReadinessComponent readiness,
                @Qualifier("tokenUsageAuditComponent") TokenUsageAuditComponent producer) throws IOException {
            Objects.requireNonNull(producer, "producer");
            return readiness.acquireSpoolLease();
        }

        @Bean(name = {"idpSecurityAuditReady", "tianquanSecurityAuditReady"})
        BooleanSupplier idpSecurityAuditReady(
                @Qualifier("idpSecurityObservationReadinessComponent") IdpSecurityObservationReadinessComponent readiness,
                @Qualifier("idpAuditSpoolLease") FileChannel lease) {
            BooleanSupplier ready = readiness.readiness(lease);
            if (!ready.getAsBoolean()) { throw new IllegalStateException("OAUTH_AUDIT_NOT_READY"); }
            return ready;
        }
    }
}
