package top.egon.cola.component.bytecode.starter.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.bytecode.starter.BytecodeProperties;
import top.egon.cola.component.bytecode.starter.BytecodeStartupValidator;

@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(BytecodeStartupValidator.class)
@ConditionalOnProperty(
        prefix = "egon.cola.component.bytecode.executor",
        name = "metrics",
        havingValue = "true",
        matchIfMissing = true
)
public class BytecodeMetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MicrometerExecutorEventSink micrometerExecutorEventSink(
            MeterRegistry registry,
            BytecodeProperties properties
    ) {
        return new MicrometerExecutorEventSink(
                registry, properties.getExecutor().getSamplingRate());
    }
}
