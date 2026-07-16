package top.egon.cola.component.bytecode.starter;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.bytecode.api.executor.ContextCarrier;
import top.egon.cola.component.bytecode.api.executor.ExecutorEventSink;
import top.egon.cola.component.bytecode.runtime.DefaultBytecodeRuntimeDispatcher;
import top.egon.cola.component.bytecode.runtime.context.CompositeContextCarrier;
import top.egon.cola.component.bytecode.runtime.event.BoundedFailureStore;
import top.egon.cola.component.bytecode.runtime.event.RuntimeEventFanout;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorNameResolver;
import top.egon.cola.component.bytecode.runtime.executor.ExecutorTaskDecorator;
import top.egon.cola.component.bytecode.runtime.executor.RuntimeTaskDetector;
import top.egon.cola.component.bytecode.starter.context.MdcContextCarrier;
import top.egon.cola.component.bytecode.starter.dtp.DtpTaskDetector;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(BytecodeProperties.class)
@ConditionalOnProperty(
        prefix = "egon.cola.component.bytecode",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class BytecodeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BytecodeStartupValidator bytecodeStartupValidator() {
        return new BytecodeStartupValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public static SpringExecutorNameSource springExecutorNameSource() {
        return new SpringExecutorNameSource();
    }

    @Bean
    @ConditionalOnClass(name = "org.slf4j.MDC")
    @ConditionalOnProperty(
            prefix = "egon.cola.component.bytecode.executor",
            name = "propagate-mdc",
            havingValue = "true",
            matchIfMissing = true
    )
    @ConditionalOnMissingBean(name = "mdcContextCarrier")
    public MdcContextCarrier mdcContextCarrier() {
        return new MdcContextCarrier();
    }

    @Bean
    @ConditionalOnMissingBean
    public DtpTaskDetector dtpTaskDetector() {
        return new DtpTaskDetector();
    }

    @Bean
    @ConditionalOnMissingBean
    public BoundedFailureStore bytecodeRuntimeFailureStore(BytecodeProperties properties) {
        return new BoundedFailureStore(properties.getRuntime().getFailureCapacity());
    }

    @Bean
    @ConditionalOnMissingBean
    public RuntimeEventFanout bytecodeRuntimeEventFanout(
            ObjectProvider<ExecutorEventSink> sinks,
            BoundedFailureStore failureStore
    ) {
        return new RuntimeEventFanout(sinks.orderedStream().toList(), failureStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExecutorTaskDecorator bytecodeExecutorTaskDecorator(
            ObjectProvider<ContextCarrier> carriers,
            RuntimeEventFanout eventFanout,
            SpringExecutorNameSource springExecutorNameSource,
            BytecodeProperties properties
    ) {
        CompositeContextCarrier contextCarrier = new CompositeContextCarrier(
                carriers.orderedStream().toList());
        ExecutorNameResolver nameResolver = new ExecutorNameResolver(
                List.of(springExecutorNameSource), properties.getExecutor().getNames());
        return new ExecutorTaskDecorator(
                contextCarrier,
                eventFanout,
                new RuntimeTaskDetector(),
                nameResolver
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultBytecodeRuntimeDispatcher bytecodeRuntimeDispatcher(
            ExecutorTaskDecorator taskDecorator
    ) {
        return new DefaultBytecodeRuntimeDispatcher(taskDecorator);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.bytecode.executor",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    @ConditionalOnMissingBean
    public BytecodeRuntimeRegistrar bytecodeRuntimeRegistrar(
            ConfigurableListableBeanFactory beanFactory,
            DefaultBytecodeRuntimeDispatcher dispatcher,
            BytecodeStartupValidator startupValidator
    ) {
        ClassLoader loader = beanFactory.getBeanClassLoader();
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
        }
        String version = BytecodeAutoConfiguration.class.getPackage().getImplementationVersion();
        return new BytecodeRuntimeRegistrar(
                loader,
                version == null ? "development" : version,
                dispatcher
        );
    }
}
