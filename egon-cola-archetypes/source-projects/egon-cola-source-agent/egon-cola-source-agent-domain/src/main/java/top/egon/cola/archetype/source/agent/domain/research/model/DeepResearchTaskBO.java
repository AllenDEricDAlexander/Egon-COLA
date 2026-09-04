package top.egon.cola.archetype.source.agent.domain.research.model;

import java.time.Instant;

/** Immutable server-owned task context passed to the Deep Research gateway. */
public record DeepResearchTaskBO(
        String runId,
        ResearchTopicBO topic,
        ReportLanguageEnum language,
        int maxSources,
        Instant deadline,
        String traceId) {

    public DeepResearchTaskBO {
        runId = requireText(runId, "runId");
        if (topic == null || language == null || deadline == null) {
            throw new IllegalArgumentException("research task values must be present");
        }
        if (maxSources < 3 || maxSources > 20) {
            throw new IllegalArgumentException("maxSources must be between 3 and 20");
        }
        traceId = requireTraceId(traceId);
    }

    public static DeepResearchTaskBO create(String runId, String topic, ReportLanguageEnum language,
                                            int maxSources, Instant deadline, String traceId) {
        return new DeepResearchTaskBO(runId, ResearchTopicBO.create(topic), language,
                maxSources, deadline, traceId);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String requireTraceId(String value) {
        String normalized = requireText(value, "traceId");
        if (normalized.length() > 128 || !normalized.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException("traceId format is invalid");
        }
        return normalized;
    }
}
