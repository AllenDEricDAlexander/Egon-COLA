package ${package}.starter.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OrganizationActuatorConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> organizationMeterTags() {
        return registry -> registry.config().commonTags("application", "student-management-organization");
    }
}
