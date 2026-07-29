package top.egon.cola.component.accessguard.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import top.egon.cola.component.accessguard.adapter.aop.GuardBindingResolver;
import top.egon.cola.component.accessguard.adapter.aop.SpringAopAccessGuardAdvisor;
import top.egon.cola.component.accessguard.core.GuardEngine;
import top.egon.cola.component.accessguard.core.plan.GuardPlanProperties;

@AutoConfiguration
@AutoConfigureAfter(AccessGuardCoreAutoConfiguration.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ConditionalOnProperty(
        prefix = GuardPlanProperties.PREFIX,
        name = "engine",
        havingValue = "AOP",
        matchIfMissing = true)
@ConditionalOnProperty(
        prefix = GuardPlanProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AccessGuardAopAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SpringAopAccessGuardAdvisor accessGuardAdvisor(
            GuardBindingResolver bindingResolver,
            GuardEngine engine
    ) {
        return new SpringAopAccessGuardAdvisor(bindingResolver, engine);
    }
}
