package top.egon.cola.component.bytecode.starter.actuator;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.bytecode.starter.BytecodeStartupValidator;
import top.egon.cola.component.bytecode.runtime.observation.ObservationRuntime;

@AutoConfiguration
@ConditionalOnClass(Endpoint.class)
@ConditionalOnBean(BytecodeStartupValidator.class)
@ConditionalOnProperty(
        prefix = "egon.cola.component.bytecode.endpoint",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class BytecodeEndpointAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EgonBytecodeEndpoint egonBytecodeEndpoint(
            ConfigurableListableBeanFactory beanFactory,
            ObjectProvider<ObservationRuntime> observationRuntime
    ) {
        ClassLoader loader = beanFactory.getBeanClassLoader();
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        return new EgonBytecodeEndpoint(loader, observationRuntime.getIfAvailable());
    }
}
