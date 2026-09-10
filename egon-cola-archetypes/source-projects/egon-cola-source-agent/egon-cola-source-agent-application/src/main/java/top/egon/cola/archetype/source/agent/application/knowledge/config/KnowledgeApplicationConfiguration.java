package top.egon.cola.archetype.source.agent.application.knowledge.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import top.egon.cola.archetype.source.agent.application.knowledge.service.KnowledgeQaCapacityService;

/** Application-layer wiring for the knowledge limits and their process-local capacity. */
@Configuration(proxyBeanMethods = false)
public class KnowledgeApplicationConfiguration {

    @Bean(name = "knowledgeRuntimeProperties")
    @ConditionalOnMissingBean(name = "knowledgeRuntimeProperties")
    public KnowledgeRuntimeProperties knowledgeRuntimeProperties(Environment environment) {
        return Binder.get(environment).bindOrCreate(
                "agent.knowledge", Bindable.of(KnowledgeRuntimeProperties.class));
    }

    @Bean(name = "knowledgeQaCapacityService")
    @ConditionalOnMissingBean(name = "knowledgeQaCapacityService")
    public KnowledgeQaCapacityService knowledgeQaCapacityService(
            @Qualifier("knowledgeRuntimeProperties") KnowledgeRuntimeProperties properties) {
        return new KnowledgeQaCapacityService(properties.runtime().qaMaxConcurrent());
    }
}
