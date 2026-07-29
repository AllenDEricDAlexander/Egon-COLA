package top.egon.cola.component.accessguard.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.accessguard.core.plan.GuardPlanProperties;
import top.egon.cola.component.accessguard.core.plan.GuardPlanResolver;
import top.egon.cola.component.accessguard.observability.CompositeGuardEventPublisher;
import top.egon.cola.component.accessguard.observability.GuardEventListener;
import top.egon.cola.component.accessguard.observability.GuardEventPublisher;
import top.egon.cola.component.accessguard.observability.LoggingGuardEventListener;
import top.egon.cola.component.accessguard.store.local.LocalPenaltyStore;
import top.egon.cola.component.accessguard.store.local.LocalRateLimitBackend;

import java.util.List;
import java.util.function.IntSupplier;

@AutoConfiguration(after = AccessGuardCoreAutoConfiguration.class)
@EnableConfigurationProperties(GuardPlanProperties.class)
@ConditionalOnProperty(
        prefix = GuardPlanProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AccessGuardObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LoggingGuardEventListener accessGuardLoggingEventListener() {
        return new LoggingGuardEventListener();
    }

    @Bean
    @ConditionalOnMissingBean(GuardEventPublisher.class)
    public CompositeGuardEventPublisher accessGuardEventPublisher(List<GuardEventListener> listeners) {
        return new CompositeGuardEventPublisher(listeners);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    static class MicrometerConfiguration {

        @Bean
        @ConditionalOnBean(type = "io.micrometer.core.instrument.MeterRegistry")
        @ConditionalOnMissingBean(type = "top.egon.cola.component.accessguard.observability.MicrometerGuardEventListener")
        top.egon.cola.component.accessguard.observability.MicrometerGuardEventListener
                accessGuardMicrometerEventListener(
                        io.micrometer.core.instrument.MeterRegistry registry,
                        ObjectProvider<LocalPenaltyStore> penaltyStore,
                        ObjectProvider<LocalRateLimitBackend> rateLimitBackend
                ) {
            LocalPenaltyStore penalty = penaltyStore.getIfAvailable();
            LocalRateLimitBackend rateLimit = rateLimitBackend.getIfAvailable();
            return new top.egon.cola.component.accessguard.observability.MicrometerGuardEventListener(
                    registry,
                    sizeSupplier(penalty),
                    sizeSupplier(rateLimit));
        }

        private static IntSupplier sizeSupplier(LocalPenaltyStore store) {
            return store == null ? null : store::size;
        }

        private static IntSupplier sizeSupplier(LocalRateLimitBackend backend) {
            return backend == null ? null : backend::size;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
    static class ActuatorConfiguration {

        @Bean
        @ConditionalOnBean(GuardPlanResolver.class)
        @ConditionalOnMissingBean(type = "top.egon.cola.component.accessguard.observability.AccessGuardEndpoint")
        top.egon.cola.component.accessguard.observability.AccessGuardEndpoint accessGuardEndpoint(
                GuardPlanProperties properties,
                GuardPlanResolver resolver,
                ObjectProvider<LocalPenaltyStore> penaltyStore,
                ObjectProvider<LocalRateLimitBackend> rateLimitBackend
        ) {
            LocalPenaltyStore penalty = penaltyStore.getIfAvailable();
            LocalRateLimitBackend rateLimit = rateLimitBackend.getIfAvailable();
            return new top.egon.cola.component.accessguard.observability.AccessGuardEndpoint(
                    properties,
                    resolver,
                    penalty == null ? () -> 0 : penalty::size,
                    rateLimit == null ? () -> 0 : rateLimit::size);
        }
    }
}
