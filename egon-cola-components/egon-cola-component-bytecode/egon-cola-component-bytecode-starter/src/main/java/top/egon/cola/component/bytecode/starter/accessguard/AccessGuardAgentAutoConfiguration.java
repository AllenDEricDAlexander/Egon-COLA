package top.egon.cola.component.bytecode.starter.accessguard;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.accessguard.config.AccessGuardRuleResolver;
import top.egon.cola.component.accessguard.execution.AccessGuardExecutionService;
import top.egon.cola.component.accessguard.execution.AccessGuardFailureHandler;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.starter.BytecodeAutoConfiguration;
import top.egon.cola.component.bytecode.starter.BytecodeStartupValidator;
import top.egon.cola.component.bytecode.starter.methodextension.MethodMetadataResolver;

@AutoConfiguration(
        before = BytecodeAutoConfiguration.class,
        afterName = "top.egon.cola.component.accessguard.autoconfigure.AccessGuardAutoConfiguration"
)
@ConditionalOnClass(name =
        "top.egon.cola.component.accessguard.execution.AccessGuardExecutionService")
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
            ObjectProvider<AccessGuardExecutionService> executionServices,
            ObjectProvider<MethodMetadataResolver> metadataResolvers,
            AccessGuardRuleResolver ruleResolver,
            AccessGuardFailureHandler failureHandler,
            BytecodeStartupValidator startupValidator
    ) {
        startupValidator.requireAgentCapability(BridgeCapability.ACCESS_GUARD);
        MethodMetadataResolver metadataResolver = metadataResolvers.getIfAvailable(
                MethodMetadataResolver::new);
        return new AccessGuardRuntimeAdapter(
                executionServices::getIfAvailable,
                metadataResolver,
                ruleResolver,
                failureHandler
        );
    }

    @Bean
    public SmartInitializingSingleton accessGuardAgentReadiness(
            AccessGuardRuntimeAdapter adapter
    ) {
        return adapter::markReady;
    }
}
