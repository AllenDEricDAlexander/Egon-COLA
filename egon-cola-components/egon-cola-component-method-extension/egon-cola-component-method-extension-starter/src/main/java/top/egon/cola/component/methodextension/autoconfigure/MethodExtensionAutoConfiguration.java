package top.egon.cola.component.methodextension.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import top.egon.cola.component.methodextension.aop.MethodExtensionAop;
import top.egon.cola.component.methodextension.event.MethodExtensionEventPublisher;
import top.egon.cola.component.methodextension.event.NoopMethodExtensionEventPublisher;
import top.egon.cola.component.methodextension.execution.MethodExtensionExecutionService;
import top.egon.cola.component.methodextension.handler.MethodExtensionHandlerResolver;
import top.egon.cola.component.methodextension.response.MethodExtensionResponseResolver;
import top.egon.cola.component.methodextension.support.MethodExtensionMethodResolver;

@AutoConfiguration
@EnableConfigurationProperties(MethodExtensionProperties.class)
public class MethodExtensionAutoConfiguration {

    @Bean
    @Conditional(MethodExtensionActiveCondition.class)
    @ConditionalOnMissingBean
    public MethodExtensionMethodResolver methodExtensionMethodResolver() {
        return new MethodExtensionMethodResolver();
    }

    @Bean
    @Conditional(MethodExtensionActiveCondition.class)
    @ConditionalOnMissingBean
    public MethodExtensionHandlerResolver methodExtensionHandlerResolver(ListableBeanFactory beanFactory) {
        return new MethodExtensionHandlerResolver(beanFactory);
    }

    @Bean
    @Conditional(MethodExtensionActiveCondition.class)
    @ConditionalOnMissingBean
    public MethodExtensionResponseResolver methodExtensionResponseResolver(ObjectProvider<ObjectMapper> objectMappers) {
        return new MethodExtensionResponseResolver(objectMappers);
    }

    @Bean
    @Conditional(MethodExtensionActiveCondition.class)
    @ConditionalOnMissingBean
    public MethodExtensionEventPublisher methodExtensionEventPublisher() {
        return new NoopMethodExtensionEventPublisher();
    }

    @Bean
    @Conditional(MethodExtensionActiveCondition.class)
    @ConditionalOnMissingBean
    public MethodExtensionExecutionService methodExtensionExecutionService(
            MethodExtensionMethodResolver methodResolver,
            MethodExtensionHandlerResolver handlerResolver,
            MethodExtensionResponseResolver responseResolver,
            MethodExtensionEventPublisher eventPublisher
    ) {
        return new MethodExtensionExecutionService(
                methodResolver, handlerResolver, responseResolver, eventPublisher);
    }

    @Bean
    @Conditional(MethodExtensionAopCondition.class)
    @ConditionalOnMissingBean
    public MethodExtensionAop methodExtensionAop(
            MethodExtensionProperties properties,
            MethodExtensionMethodResolver methodResolver,
            MethodExtensionExecutionService executionService
    ) {
        return new MethodExtensionAop(properties, methodResolver, executionService);
    }
}
