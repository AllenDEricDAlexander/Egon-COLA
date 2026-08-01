package top.egon.cola.platform.rbac3.admin.integration.ddc;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.ddc.service.DdcConfigApplierRegistry;

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
            AtomicRbac3RuntimePolicy policy) {
        return () -> {
            register(registry, policy, AtomicRbac3RuntimePolicy.ACCESS_TOKEN_TTL_KEY, 0);
            register(registry, policy, AtomicRbac3RuntimePolicy.MAXIMUM_ACTIVE_ROOTS_KEY, 0);
            register(registry, policy, AtomicRbac3RuntimePolicy.REFRESH_TOKEN_TTL_KEY, 10);
            register(registry, policy, AtomicRbac3RuntimePolicy.SESSION_ABSOLUTE_TIMEOUT_KEY, 20);
            register(registry, policy, AtomicRbac3RuntimePolicy.SESSION_IDLE_TIMEOUT_KEY, 30);
        };
    }

    private void register(
            DdcConfigApplierRegistry registry,
            AtomicRbac3RuntimePolicy policy,
            String key,
            int priority) {
        registry.registerExact(key, new Rbac3DdcPolicyApplier(key, priority, policy));
    }
}
