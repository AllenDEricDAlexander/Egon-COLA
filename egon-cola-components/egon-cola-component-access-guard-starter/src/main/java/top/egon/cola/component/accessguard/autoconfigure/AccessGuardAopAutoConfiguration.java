package top.egon.cola.component.accessguard.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import top.egon.cola.component.accessguard.adapter.aop.GuardBindingResolver;
import top.egon.cola.component.accessguard.adapter.aop.SpringAopAccessGuardAdvisor;
import top.egon.cola.component.accessguard.core.GuardEngine;
import top.egon.cola.component.accessguard.execution.async.CompletionStageGuardExecutor;
import top.egon.cola.component.accessguard.execution.reactive.ReactiveGuardExecutor;

@AutoConfiguration
@AutoConfigureAfter(AccessGuardCoreAutoConfiguration.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ConditionalOnProperty(
        prefix = AccessGuardProperties.PREFIX,
        name = "engine",
        havingValue = "AOP",
        matchIfMissing = true)
@ConditionalOnProperty(
        prefix = AccessGuardProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AccessGuardAopAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SpringAopAccessGuardAdvisor accessGuardAdvisor(
            GuardBindingResolver bindingResolver,
            GuardEngine engine,
            CompletionStageGuardExecutor completionStageExecutor,
            ObjectProvider<ReactiveGuardExecutor> reactiveExecutor
    ) {
        return new SpringAopAccessGuardAdvisor(
                bindingResolver,
                engine,
                completionStageExecutor,
                reactiveExecutor.getIfAvailable());
    }
}
