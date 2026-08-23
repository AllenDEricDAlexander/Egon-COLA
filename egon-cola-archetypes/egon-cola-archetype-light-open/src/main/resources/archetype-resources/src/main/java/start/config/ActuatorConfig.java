package ${package}.start.config;

import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ActuatorConfig {
    @Bean
    InfoContributor architectureInfoContributor() {
        return builder -> builder.withDetail("architecture", "large-monolith-light-domain");
    }
}
