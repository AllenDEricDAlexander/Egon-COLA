package top.egon.cola.platform.rbac3.admin.integration.ddc;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.ddc.api.refresh.DdcConfigApplierRegistry;
import top.egon.cola.component.ddc.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportingState;

/**
 * Registers RBAC3 policy adapters before the DDC applier registry is frozen.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "egon.cola.component.ddc",
        name = "enabled",
        havingValue = "true")
public class Rbac3DdcPolicyConfiguration {

    @Bean
    public Rbac3DdcValueDeclarations rbac3DdcValueDeclarations() {
        return new Rbac3DdcValueDeclarations();
    }

    @Bean
    public InitializingBean rbac3DdcPolicyRegistrar(
            DdcConfigApplierRegistry registry,
            AtomicRbac3RuntimePolicy policy,
            ObjectProvider<Rbac3IntegrationMetrics> metrics) {
        Rbac3IntegrationMetrics available = metrics.getIfAvailable();
        Rbac3DdcPolicyApplier.ApplyObserver observer = available == null
                ? Rbac3DdcPolicyApplier.ApplyObserver.noop()
                : available;
        return registrar(registry, policy, observer);
    }

    InitializingBean rbac3DdcPolicyRegistrar(
            DdcConfigApplierRegistry registry,
            AtomicRbac3RuntimePolicy policy) {
        return registrar(registry, policy, Rbac3DdcPolicyApplier.ApplyObserver.noop());
    }

    @Bean
    DdcConfigClientStatusService ddcConfigClientStatusService(
            ObjectProvider<DdcRuntimeCoordinator> coordinator,
            AtomicRbac3RuntimePolicy policy) {
        return new DdcConfigClientStatusService(coordinator, policy);
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    Rbac3IntegrationMetrics rbac3IntegrationMetrics(
            MeterRegistry registry,
            AtomicRbac3RuntimePolicy policy,
            ObjectProvider<DdcRuntimeCoordinator> coordinator,
            ObjectProvider<GatewayReportingState> reportingState) {
        return new Rbac3IntegrationMetrics(
                registry, policy, coordinator, reportingState);
    }

    private InitializingBean registrar(
            DdcConfigApplierRegistry registry,
            AtomicRbac3RuntimePolicy policy,
            Rbac3DdcPolicyApplier.ApplyObserver observer) {
        return () -> {
            register(registry, policy, observer,
                    AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, 0);
            register(registry, policy, observer,
                    AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY, 0);
            register(registry, policy, observer,
                    AtomicRbac3RuntimePolicy.REFRESH_TOKEN_TTL_KEY, 10);
            register(registry, policy, observer,
                    AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY, 20);
            register(registry, policy, observer,
                    AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY, 30);
        };
    }

    private void register(
            DdcConfigApplierRegistry registry,
            AtomicRbac3RuntimePolicy policy,
            Rbac3DdcPolicyApplier.ApplyObserver observer,
            String key,
            int priority) {
        registry.registerExact(
                key, new Rbac3DdcPolicyApplier(key, priority, policy, observer));
    }
}
