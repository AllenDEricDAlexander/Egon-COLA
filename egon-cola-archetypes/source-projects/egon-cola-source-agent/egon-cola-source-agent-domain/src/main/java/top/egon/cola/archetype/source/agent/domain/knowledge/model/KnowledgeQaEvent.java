package top.egon.cola.archetype.source.agent.domain.knowledge.model;

import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;

import java.time.Instant;
import java.util.List;

/**
 * Validated allowlist event shared by the answer gateway, the use case and the SSE adapter.
 *
 * <p>One shape covers all four variants, and the compact constructor refuses a combination the
 * variant does not allow — a progress event carrying an answer, a started event carrying an error
 * code — so the SSE layer never has to guess which fields are meaningful. The trace id is required
 * on every variant the contract documents it for; a progress event may leave it out, exactly as the
 * event payload table does.
 */
public record KnowledgeQaEvent(
        String answerId,
        long sequence,
        KnowledgeQaEventTypeEnum type,
        List<KnowledgeRetrievedChunkBO> retrievals,
        String delta,
        String answer,
        KnowledgeErrorCodeEnum errorCode,
        String errorMessage,
        Boolean retryable,
        Instant occurredAt,
        String traceId) {

    public KnowledgeQaEvent {
        answerId = requireText(answerId, "answerId");
        if (sequence < 1 || type == null || occurredAt == null) {
            throw new IllegalArgumentException("qa event identity values are invalid");
        }
        retrievals = retrievals == null ? List.of() : List.copyOf(retrievals);
        delta = optional(delta);
        answer = optional(answer);
        errorMessage = optional(errorMessage);
        traceId = optional(traceId);
        if (traceId != null && (traceId.length() > 128 || !traceId.matches("[A-Za-z0-9._:-]+"))) {
            throw new IllegalArgumentException("traceId format is invalid");
        }
        validatePayload(type, retrievals, delta, answer, errorCode, errorMessage, retryable, traceId);
    }

    /** First event of a stream: the retrieval outcome, before the model is called. */
    public static KnowledgeQaEvent started(String answerId, long sequence,
                                           List<KnowledgeRetrievedChunkBO> retrievals,
                                           Instant occurredAt, String traceId) {
        return new KnowledgeQaEvent(answerId, sequence, KnowledgeQaEventTypeEnum.STARTED, retrievals,
                null, null, null, null, null, occurredAt, traceId);
    }

    /** One increment of the answer text. */
    public static KnowledgeQaEvent progress(String answerId, long sequence, String delta, Instant occurredAt) {
        return new KnowledgeQaEvent(answerId, sequence, KnowledgeQaEventTypeEnum.PROGRESS, List.of(),
                delta, null, null, null, null, occurredAt, null);
    }

    /** Terminal success; the references are the ones {@code started} carried. */
    public static KnowledgeQaEvent completed(String answerId, long sequence, String answer,
                                             List<KnowledgeRetrievedChunkBO> retrievals,
                                             Instant occurredAt, String traceId) {
        return new KnowledgeQaEvent(answerId, sequence, KnowledgeQaEventTypeEnum.COMPLETED, retrievals,
                null, answer, null, null, null, occurredAt, traceId);
    }

    /** Terminal failure; the message and the retry decision come from the code's safe mapping. */
    public static KnowledgeQaEvent failed(String answerId, long sequence, KnowledgeErrorCodeEnum errorCode,
                                          Instant occurredAt, String traceId) {
        return new KnowledgeQaEvent(answerId, sequence, KnowledgeQaEventTypeEnum.FAILED, List.of(),
                null, null, errorCode, errorCode == null ? null : errorCode.safeMessage(),
                errorCode == null ? null : errorCode.retryable(), occurredAt, traceId);
    }

    /** A terminal event ends the stream: nothing may follow it. */
    public boolean isTerminal() {
        return type == KnowledgeQaEventTypeEnum.COMPLETED || type == KnowledgeQaEventTypeEnum.FAILED;
    }

    private static void validatePayload(KnowledgeQaEventTypeEnum type, List<KnowledgeRetrievedChunkBO> retrievals,
                                        String delta, String answer, KnowledgeErrorCodeEnum errorCode,
                                        String errorMessage, Boolean retryable, String traceId) {
        switch (type) {
            case STARTED -> {
                require(delta == null && answer == null, "started text values must be absent");
                requireNoError(errorCode, errorMessage, retryable, "started");
                require(traceId != null, "started traceId is required");
            }
            case PROGRESS -> {
                require(delta != null, "progress delta is required");
                require(retrievals.isEmpty() && answer == null, "progress carries only its delta");
                requireNoError(errorCode, errorMessage, retryable, "progress");
            }
            case COMPLETED -> {
                require(answer != null, "completed answer is required");
                require(delta == null, "completed delta must be absent");
                requireNoError(errorCode, errorMessage, retryable, "completed");
                require(traceId != null, "completed traceId is required");
            }
            case FAILED -> {
                require(errorCode != null && errorMessage != null && retryable != null,
                        "failed error values are required");
                require(delta == null && answer == null && retrievals.isEmpty(),
                        "failed text and reference values must be absent");
                require(traceId != null, "failed traceId is required");
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

    private static void requireNoError(KnowledgeErrorCodeEnum errorCode, String errorMessage, Boolean retryable,
                                       String variant) {
        require(errorCode == null && errorMessage == null && retryable == null,
                variant + " error values must be absent");
    }
}
