package top.egon.cola.archetype.source.agent.application.research.config;

import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import top.egon.cola.archetype.source.agent.application.research.service.ResearchCapacityService;
import top.egon.cola.component.common.core.validation.ValidationUtils;

import java.time.Clock;

/** Application-layer wiring for validation, process-local capacity and deterministic time. */
@Configuration(proxyBeanMethods = false)
public class DeepResearchApplicationConfiguration {

    @Bean(name = {"agentClock", "deepResearchClock"})
    @ConditionalOnMissingBean(name = "agentClock")
    public Clock deepResearchClock() {
        return Clock.systemUTC();
    }

    @Bean(name = {"agentValidationUtils", "deepResearchValidationUtils"})
    @ConditionalOnMissingBean(name = "agentValidationUtils")
    public ValidationUtils deepResearchValidationUtils(Validator validator) {
        return new ValidationUtils(validator);
    }

    @Bean(name = "researchCapacityService")
    @ConditionalOnMissingBean(name = "researchCapacityService")
    public ResearchCapacityService researchCapacityService(
            @Qualifier("deepResearchRuntimeProperties") DeepResearchRuntimeProperties properties) {
        return new ResearchCapacityService(properties.maxConcurrentRuns());
    }

    @Bean(name = "deepResearchRuntimeProperties")
    @ConditionalOnMissingBean(name = "deepResearchRuntimeProperties")
    public DeepResearchRuntimeProperties deepResearchRuntimeProperties(Environment environment) {
        return Binder.get(environment).bindOrCreate(
                "agent.deep-research.runtime", Bindable.of(DeepResearchRuntimeProperties.class));
    }
}
