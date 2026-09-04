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
        if (maxConcurrentRuns < 1 || maxConcurrentRuns > 32) {
            throw new IllegalArgumentException("maxConcurrentRuns must be between 1 and 32");
        }
        maxDuration = maxDuration == null ? Duration.ofMinutes(5) : maxDuration;
        if (maxDuration.isZero() || maxDuration.isNegative() || maxDuration.compareTo(Duration.ofMinutes(30)) > 0) {
            throw new IllegalArgumentException("maxDuration is outside the allowed range");
        }
        if (maxSources < 3 || maxSources > 20) {
            throw new IllegalArgumentException("maxSources must be between 3 and 20");
        }
        heartbeatInterval = heartbeatInterval == null ? Duration.ofSeconds(15) : heartbeatInterval;
        if (heartbeatInterval.isZero() || heartbeatInterval.isNegative()
                || heartbeatInterval.compareTo(maxDuration) >= 0) {
            throw new IllegalArgumentException("heartbeatInterval is outside the allowed range");
        }
    }
}
