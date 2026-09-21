package top.egon.cola.archetype.source.agent.common.error;

import top.egon.cola.component.common.core.enums.ErrorStatus;

/**
 * Stable, safe error codes shared by the Deep Research application and adapter.
 *
 * <p>The String {@link #getStatus()} stays the constant name, so the published wire vocabulary is
 * unchanged by the common status contract.
 */
public enum ResearchErrorCodeEnum implements ErrorStatus {

    RESEARCH_VALIDATION_ERROR(0, "validation failed", false),
    RESEARCH_UNAUTHORIZED(1, "research authorization failed", false),
    RESEARCH_NOT_ACCEPTABLE(2, "requested representation is not acceptable", false),
    RESEARCH_UNSUPPORTED_MEDIA_TYPE(3, "request media type is not supported", false),
    RESEARCH_CAPACITY_EXHAUSTED(4, "research capacity is exhausted", true),
    RESEARCH_DEPENDENCY_UNAVAILABLE(5, "research dependency is unavailable", true),
    RESEARCH_INTERNAL_ERROR(6, "research execution failed", false),
    RESEARCH_TIMEOUT(7, "research execution timed out", true);

    private final int code;

    private final String safeMessage;

    private final boolean retryable;

    ResearchErrorCodeEnum(int code, String safeMessage, boolean retryable) {
        this.code = code;
        this.safeMessage = safeMessage;
        this.retryable = retryable;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return safeMessage;
    }

    @Override
    public String getStatus() {
        return name();
    }

    /** @return the stable String code published on the wire, unchanged by the common contract */
    public String code() {
        return getStatus();
    }

    /** @return the client-safe summary, which is also the common enum message */
    public String safeMessage() {
        return safeMessage;
    }

    /** @return whether the caller may retry the rejected request */
    public boolean retryable() {
        return retryable;
    }
}
