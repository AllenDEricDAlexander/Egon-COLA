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
import top.egon.cola.component.bytecode.starter.observation.ObservationMetadataValidator;

@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(BytecodeStartupValidator.class)
public class BytecodeMetricsAutoConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.bytecode.executor",
            name = "metrics",
            havingValue = "true",
            matchIfMissing = true
    )
    @ConditionalOnMissingBean
    public MicrometerExecutorEventSink micrometerExecutorEventSink(
            MeterRegistry registry,
            BytecodeProperties properties
    ) {
        return new MicrometerExecutorEventSink(
                registry, properties.getExecutor().getSamplingRate());
    }

    @Bean
    @ConditionalOnMissingBean
    public ObservationMetadataValidator observationMetadataValidator() {
        return new ObservationMetadataValidator();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "egon.cola.component.bytecode.observation",
            name = "metrics-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    @ConditionalOnMissingBean
    public MicrometerObservationEventSink micrometerObservationEventSink(
            MeterRegistry registry,
            ObservationMetadataValidator metadataValidator
    ) {
        return new MicrometerObservationEventSink(registry, metadataValidator);
    }
}
