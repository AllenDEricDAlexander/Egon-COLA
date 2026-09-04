package top.egon.cola.archetype.source.agent.starter.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.archetype.source.agent.infrastructure.research.tool.McpResearchToolProperties;

import java.time.Duration;

/** Typed host configuration for model, search tools, runtime limits and documentation exposure. */
@Validated
@ConfigurationProperties(prefix = "agent.deep-research", ignoreUnknownFields = false)
public record DeepResearchConfigurationProperties(
        @Valid @NotNull RuntimeProperties runtime,
        @Valid @NotNull ModelProperties model,
        @Valid @NotNull SecurityProperties security,
        @Valid @NotNull SearchProperties search,
        @Valid @NotNull DocsProperties docs) {

    public DeepResearchConfigurationProperties {
        runtime = runtime == null
                ? new RuntimeProperties(4, Duration.ofMinutes(5), 20, Duration.ofSeconds(15))
                : runtime;
        model = model == null ? new ModelProperties(null, null, null) : model;
        security = security == null ? new SecurityProperties(null) : security;
        search = search == null ? new SearchProperties(null) : search;
        docs = docs == null ? new DocsProperties(false) : docs;
    }

    public record RuntimeProperties(
            int maxConcurrentRuns,
            Duration maxDuration,
            int maxSources,
            Duration heartbeatInterval) {

        public RuntimeProperties {
            maxConcurrentRuns = maxConcurrentRuns == 0 ? 4 : maxConcurrentRuns;
            maxDuration = maxDuration == null ? Duration.ofMinutes(5) : maxDuration;
            maxSources = maxSources == 0 ? 20 : maxSources;
            heartbeatInterval = heartbeatInterval == null ? Duration.ofSeconds(15) : heartbeatInterval;
        }
    }

    public record ModelProperties(
            @NotBlank String baseUrl,
            @NotBlank String apiKey,
            @NotBlank String modelName) {

        public ModelProperties {
            baseUrl = normalize(baseUrl);
            apiKey = normalize(apiKey);
            modelName = normalize(modelName);
        }
    }

    public record SecurityProperties(@NotBlank String apiKey) {

        public SecurityProperties {
            apiKey = normalize(apiKey);
        }
    }

    public record SearchProperties(@Valid @NotNull McpResearchToolProperties mcp) {

        public SearchProperties {
            mcp = mcp == null ? new McpResearchToolProperties(null, null, null, null) : mcp;
        }
    }

    public record DocsProperties(boolean enabled) {
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
