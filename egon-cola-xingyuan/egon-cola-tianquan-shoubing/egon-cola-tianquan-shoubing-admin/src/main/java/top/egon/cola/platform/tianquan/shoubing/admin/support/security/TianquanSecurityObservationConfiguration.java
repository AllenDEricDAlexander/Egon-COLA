package top.egon.cola.platform.tianquan.shoubing.admin.support.security;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Validator;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthUsageObservationProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;

/** Uses Boot's unique or primary registry; transport/readiness checks are added by the deployment gate. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "egon.cola.platform.tianquan.shoubing.usage-observation", name = "enabled", havingValue = "true")
public class TianquanSecurityObservationConfiguration {
    @Bean(name = "idpMeterRegistrySupplier")
    Supplier<MeterRegistry> idpMeterRegistrySupplier(ObjectProvider<MeterRegistry> registries) { return registries::getObject; }

    @Bean(name = "idpSecurityMetricsRecorder")
    IdpSecurityMetricsRecorder idpSecurityMetricsRecorder(@Qualifier("idpMeterRegistrySupplier") Supplier<MeterRegistry> registries,
            @Qualifier("defaultValidator") Validator validator) {
        registries.get();
        return new TianquanSecurityMetricsComponent(registries, validator);
    }
    /** Separate from the port factory to avoid a producer-to-recorder initialization cycle. */
    @Bean(name = "tianquanSecurityObservationStartupCheck")
    Object tianquanSecurityObservationStartupCheck(@Qualifier("tokenUsageAuditComponent") TokenUsageAuditComponent producer,
            @Qualifier("oauthUsageObservationProperties") OAuthUsageObservationProperties properties,
            @Qualifier("defaultValidator") Validator validator,
            @Qualifier("idpMeterRegistrySupplier") Supplier<MeterRegistry> registries) {
        new ValidationUtils(validator).validate(properties);
        registries.get();
        java.util.Objects.requireNonNull(producer, "producer");
        return new Object();
    }
}
