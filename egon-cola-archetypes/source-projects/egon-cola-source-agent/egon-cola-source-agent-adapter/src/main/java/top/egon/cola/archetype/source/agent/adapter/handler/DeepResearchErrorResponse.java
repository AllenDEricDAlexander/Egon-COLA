package top.egon.cola.archetype.source.agent.adapter.handler;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Stable JSON error body that excludes causes, stack traces and provider messages. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "DeepResearchErrorResponse")
public record DeepResearchErrorResponse(
        @Schema(example = "RESEARCH_VALIDATION_ERROR") String code,
        @Schema(example = "validation failed") String message,
        @Schema(example = "trace-1") String traceId,
        @Schema(format = "date-time") Instant timestamp,
        Map<String, List<String>> fieldErrors) {

    public DeepResearchErrorResponse {
        fieldErrors = fieldErrors == null ? Map.of() : fieldErrors.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }
}
