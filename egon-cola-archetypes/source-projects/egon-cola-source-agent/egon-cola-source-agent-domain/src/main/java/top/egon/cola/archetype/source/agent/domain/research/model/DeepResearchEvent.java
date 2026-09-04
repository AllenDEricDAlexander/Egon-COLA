package top.egon.cola.archetype.source.agent.domain.research.model;

import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;

import java.time.Instant;

/** Validated allowlist event shared by the gateway, application and SSE adapter. */
public record DeepResearchEvent(
        String runId,
        long sequence,
        ResearchEventTypeEnum type,
        ResearchStageEnum stage,
        Instant occurredAt,
        String traceId,
        String agentName,
        String delta,
        String reportMarkdown,
        ResearchErrorCodeEnum errorCode,
        String errorMessage,
        Boolean retryable) {

    public DeepResearchEvent {
        runId = requireText(runId, "runId");
        if (sequence < 1 || type == null || stage == null || occurredAt == null) {
            throw new IllegalArgumentException("research event identity values are invalid");
        }
        traceId = requireText(traceId, "traceId");
        if (traceId.length() > 128 || !traceId.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException("traceId format is invalid");
        }
        agentName = optional(agentName);
        delta = optional(delta);
        reportMarkdown = optional(reportMarkdown);
        errorMessage = optional(errorMessage);
        validatePayload(type, stage, agentName, delta, reportMarkdown, errorCode, errorMessage, retryable);
    }

    public static DeepResearchEvent started(String runId, String traceId, Instant occurredAt) {
        return new DeepResearchEvent(runId, 1, ResearchEventTypeEnum.STARTED,
                ResearchStageEnum.PLANNING, occurredAt, traceId, null, null, null, null, null, null);
    }

    public static DeepResearchEvent progress(String runId, long sequence, ResearchStageEnum stage,
                                             String agentName, String delta, Instant occurredAt, String traceId) {
        return new DeepResearchEvent(runId, sequence, ResearchEventTypeEnum.PROGRESS,
                stage, occurredAt, traceId, agentName, delta, null, null, null, null);
    }

    public static DeepResearchEvent completed(String runId, long sequence, String reportMarkdown,
                                              Instant occurredAt, String traceId) {
        return new DeepResearchEvent(runId, sequence, ResearchEventTypeEnum.COMPLETED,
                ResearchStageEnum.COMPLETED, occurredAt, traceId, null, null, reportMarkdown, null, null, null);
    }

    public static DeepResearchEvent failed(String runId, long sequence, ResearchErrorCodeEnum errorCode,
                                           Instant occurredAt, String traceId) {
        return new DeepResearchEvent(runId, sequence, ResearchEventTypeEnum.FAILED,
                ResearchStageEnum.FAILED, occurredAt, traceId, null, null, null, errorCode,
                errorCode == null ? null : errorCode.safeMessage(), errorCode == null ? null : errorCode.retryable());
    }

    public boolean isTerminal() {
        return type == ResearchEventTypeEnum.COMPLETED || type == ResearchEventTypeEnum.FAILED;
    }

    private static void validatePayload(ResearchEventTypeEnum type, ResearchStageEnum stage,
                                        String agentName, String delta, String reportMarkdown,
                                        ResearchErrorCodeEnum errorCode, String errorMessage, Boolean retryable) {
        switch (type) {
            case STARTED -> {
                require(stage == ResearchStageEnum.PLANNING, "started stage must be PLANNING");
                requireEmpty(agentName, "started agentName");
                requireEmpty(delta, "started delta");
                requireEmpty(reportMarkdown, "started reportMarkdown");
                require(errorCode == null && errorMessage == null && retryable == null,
                        "started error values must be absent");
            }
            case PROGRESS -> {
                require(stage != ResearchStageEnum.COMPLETED && stage != ResearchStageEnum.FAILED,
                        "progress stage must not be terminal");
                require(agentName != null || delta != null, "progress requires agentName or delta");
                requireEmpty(reportMarkdown, "progress reportMarkdown");
                require(errorCode == null && errorMessage == null && retryable == null,
                        "progress error values must be absent");
            }
            case COMPLETED -> {
                require(stage == ResearchStageEnum.COMPLETED, "completed stage must be COMPLETED");
                require(reportMarkdown != null, "completed reportMarkdown is required");
                require(agentName == null && delta == null && errorCode == null
                                && errorMessage == null && retryable == null,
                        "completed non-report values must be absent");
            }
            case FAILED -> {
                require(stage == ResearchStageEnum.FAILED, "failed stage must be FAILED");
                require(errorCode != null && errorMessage != null && retryable != null,
                        "failed error values are required");
                require(agentName == null && delta == null && reportMarkdown == null,
                        "failed progress/report values must be absent");
            }
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static String optional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireEmpty(String value, String field) {
        require(value == null, field + " must be absent");
    }
}
