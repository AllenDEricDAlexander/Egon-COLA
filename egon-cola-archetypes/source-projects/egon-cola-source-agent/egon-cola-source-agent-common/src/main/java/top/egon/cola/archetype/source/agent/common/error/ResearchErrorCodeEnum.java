package top.egon.cola.archetype.source.agent.common.error;

/** Stable, safe error codes shared by the Deep Research application and adapter. */
public enum ResearchErrorCodeEnum {

    RESEARCH_VALIDATION_ERROR("validation failed", false),
    RESEARCH_UNAUTHORIZED("research authorization failed", false),
    RESEARCH_NOT_ACCEPTABLE("requested representation is not acceptable", false),
    RESEARCH_UNSUPPORTED_MEDIA_TYPE("request media type is not supported", false),
    RESEARCH_CAPACITY_EXHAUSTED("research capacity is exhausted", true),
    RESEARCH_DEPENDENCY_UNAVAILABLE("research dependency is unavailable", true),
    RESEARCH_INTERNAL_ERROR("research execution failed", false),
    RESEARCH_TIMEOUT("research execution timed out", true);

    private final String safeMessage;
    private final boolean retryable;

    ResearchErrorCodeEnum(String safeMessage, boolean retryable) {
        this.safeMessage = safeMessage;
        this.retryable = retryable;
    }

    public String code() {
        return name();
    }

    public String safeMessage() {
        return safeMessage;
    }

    public boolean retryable() {
        return retryable;
    }
}
