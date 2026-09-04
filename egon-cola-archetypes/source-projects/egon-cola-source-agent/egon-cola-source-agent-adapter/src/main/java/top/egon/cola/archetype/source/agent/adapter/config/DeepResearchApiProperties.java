package top.egon.cola.archetype.source.agent.adapter.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** API security settings owned by the HTTP adapter and never exposed in responses. */
@Validated
@ConfigurationProperties(prefix = "agent.deep-research.security", ignoreUnknownFields = false)
public record DeepResearchApiProperties(@NotBlank String apiKey) {
}
