package top.egon.cola.platform.idp.admin.integration.ddc;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.egon.cola.component.ddc.api.refresh.DdcConfigApplierRegistry;

@Configuration(proxyBeanMethods = false)
public class IdpDdcPolicyConfiguration {

    @Bean
    AtomicIdpRuntimePolicy idpRuntimePolicy() {
        return new AtomicIdpRuntimePolicy();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.ddc",
            name = "enabled",
            havingValue = "true"
    )
    IdpDdcValueDeclarations idpDdcValueDeclarations() {
        return new IdpDdcValueDeclarations();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.ddc",
            name = "enabled",
            havingValue = "true"
    )
    InitializingBean idpDdcPolicyRegistrar(
            DdcConfigApplierRegistry registry,
            AtomicIdpRuntimePolicy policy
    ) {
        return () -> {
            register(registry, policy,
                    AtomicIdpRuntimePolicy.ACCESS_TOKEN_TTL_KEY, 0);
            register(registry, policy,
                    AtomicIdpRuntimePolicy.MAXIMUM_LOGIN_FAILURES_KEY, 0);
            register(registry, policy,
                    AtomicIdpRuntimePolicy.REFRESH_TOKEN_TTL_KEY, 10);
            register(registry, policy,
                    AtomicIdpRuntimePolicy.AUTHORIZATION_CODE_TTL_KEY, 10);
            register(registry, policy,
                    AtomicIdpRuntimePolicy.LOGIN_LOCK_DURATION_KEY, 20);
            register(registry, policy,
                    AtomicIdpRuntimePolicy.PASSWORD_MAXIMUM_CONCURRENCY_KEY,
                    30);
        };
    }

    private void register(
            DdcConfigApplierRegistry registry,
            AtomicIdpRuntimePolicy policy,
            String key,
            int priority
    ) {
        registry.registerExact(
                key,
                new IdpDdcPolicyApplier(key, priority, policy)
        );
    }
}
