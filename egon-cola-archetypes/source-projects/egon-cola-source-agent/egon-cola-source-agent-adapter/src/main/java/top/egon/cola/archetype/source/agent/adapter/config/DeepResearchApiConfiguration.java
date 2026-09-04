package top.egon.cola.archetype.source.agent.adapter.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the adapter-owned API key properties for constructor-bound validation. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DeepResearchApiProperties.class)
public class DeepResearchApiConfiguration {
}
