package top.egon.cola.archetype.source.agent.application.research.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import top.egon.cola.archetype.source.agent.domain.research.model.ReportLanguageEnum;

/** Normalized application command for the single Deep Research use case. */
public record StartDeepResearchCommand(
        @NotBlank @Size(min = 3, max = 500) String topic,
        @NotNull ReportLanguageEnum reportLanguage,
        @Min(3) @Max(20) int maxSources,
        @NotBlank @Size(min = 1, max = 128) String traceId) {

    public StartDeepResearchCommand {
        topic = normalize(topic);
        traceId = normalize(traceId);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
