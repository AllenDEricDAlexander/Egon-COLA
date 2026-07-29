package top.egon.cola.component.bytecode.starter.accessguard;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.accessguard.adapter.aop.GuardBindingResolver;
import top.egon.cola.component.accessguard.core.GuardEngine;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.starter.BytecodeAutoConfiguration;
import top.egon.cola.component.bytecode.starter.BytecodeStartupValidator;
import top.egon.cola.component.bytecode.starter.methodextension.MethodMetadataResolver;

@AutoConfiguration(
        before = BytecodeAutoConfiguration.class,
        afterName = "top.egon.cola.component.accessguard.autoconfigure.AccessGuardCoreAutoConfiguration"
)
@ConditionalOnClass(name = "top.egon.cola.component.accessguard.core.GuardEngine")
@ConditionalOnProperty(
        prefix = "egon.cola.component.access-guard",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnProperty(
        prefix = "egon.cola.component.access-guard",
        name = "engine",
        havingValue = "agent"
)
public class AccessGuardAgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AccessGuardRuntimeAdapter accessGuardRuntimeAdapter(
            GuardEngine engine,
            GuardBindingResolver bindingResolver,
            ObjectProvider<MethodMetadataResolver> metadataResolvers,
            BytecodeStartupValidator startupValidator
    ) {
        startupValidator.requireAgentCapability(BridgeCapability.ACCESS_GUARD);
        return new AccessGuardRuntimeAdapter(
                engine,
                metadataResolvers.getIfAvailable(MethodMetadataResolver::new),
                bindingResolver);
    }

    @Bean
    public SmartInitializingSingleton accessGuardAgentReadiness(
            AccessGuardRuntimeAdapter adapter
    ) {
        return adapter::markReady;
    }
}
