package top.egon.cola.archetype.source.agent.application.research.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

/** Application-owned limits for local Deep Research execution. */
public record DeepResearchRuntimeProperties(
        @Min(1) @Max(32) int maxConcurrentRuns,
        @NotNull Duration maxDuration,
        @Min(3) @Max(20) int maxSources,
        @NotNull Duration heartbeatInterval) {

    public DeepResearchRuntimeProperties {
        maxDuration = maxDuration == null ? Duration.ofMinutes(5) : maxDuration;
        heartbeatInterval = heartbeatInterval == null ? Duration.ofSeconds(15) : heartbeatInterval;
    }
}
