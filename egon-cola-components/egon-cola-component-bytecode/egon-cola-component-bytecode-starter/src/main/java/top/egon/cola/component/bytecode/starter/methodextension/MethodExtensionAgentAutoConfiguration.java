package top.egon.cola.component.bytecode.starter.methodextension;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.bytecode.bridge.BridgeCapability;
import top.egon.cola.component.bytecode.starter.BytecodeAutoConfiguration;
import top.egon.cola.component.bytecode.starter.BytecodeStartupValidator;
import top.egon.cola.component.methodextension.autoconfigure.MethodExtensionProperties;
import top.egon.cola.component.methodextension.execution.MethodExtensionExecutionService;

@AutoConfiguration(
        before = BytecodeAutoConfiguration.class,
        afterName = "top.egon.cola.component.methodextension.autoconfigure.MethodExtensionAutoConfiguration"
)
@ConditionalOnClass(name =
        "top.egon.cola.component.methodextension.execution.MethodExtensionExecutionService")
@ConditionalOnProperty(
        prefix = "egon.cola.component.method-extension",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnProperty(
        prefix = "egon.cola.component.method-extension",
        name = "engine",
        havingValue = "agent"
)
public class MethodExtensionAgentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MethodMetadataResolver methodMetadataResolver() {
        return new MethodMetadataResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public MethodExtensionRuntimeAdapter methodExtensionRuntimeAdapter(
            ObjectProvider<MethodExtensionExecutionService> executionServices,
            MethodMetadataResolver metadataResolver,
            MethodExtensionProperties properties,
            BytecodeStartupValidator startupValidator
    ) {
        startupValidator.requireAgentCapability(BridgeCapability.METHOD_EXTENSION);
        return new MethodExtensionRuntimeAdapter(
                executionServices::getIfAvailable,
                metadataResolver,
                properties.getNotReadyPolicy()
        );
    }

    @Bean
    public SmartInitializingSingleton methodExtensionAgentReadiness(
            MethodExtensionRuntimeAdapter adapter
    ) {
        return adapter::markReady;
    }
}
