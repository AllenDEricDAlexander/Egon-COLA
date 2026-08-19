package top.egon.cola.component.outbox.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.outbox.observability.MicrometerOutboxMetrics;
import top.egon.cola.component.outbox.observability.OutboxMetrics;

import javax.sql.DataSource;

@AutoConfiguration(before = TransactionalOutboxAutoConfiguration.class)
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean({MeterRegistry.class, DataSource.class})
@ConditionalOnProperty(
        prefix = "egon.cola.component.transactional-outbox",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxMetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OutboxMetrics.class)
    MicrometerOutboxMetrics micrometerOutboxMetrics(MeterRegistry meterRegistry) {
        return new MicrometerOutboxMetrics(meterRegistry);
    }
}
