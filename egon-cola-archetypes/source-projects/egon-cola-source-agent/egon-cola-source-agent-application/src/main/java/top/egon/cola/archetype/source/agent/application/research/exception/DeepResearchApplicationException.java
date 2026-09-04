package top.egon.cola.archetype.source.agent.application.research.exception;

import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;

import java.util.List;
import java.util.Map;

/** Safe application failure carrying only the public research error vocabulary. */
public class DeepResearchApplicationException extends RuntimeException {

    private final ResearchErrorCodeEnum code;
    private final String traceId;
    private final Map<String, List<String>> fieldErrors;

    public DeepResearchApplicationException(ResearchErrorCodeEnum code, String traceId) {
        this(code, traceId, Map.of(), null);
    }

    public DeepResearchApplicationException(ResearchErrorCodeEnum code, String traceId, Throwable cause) {
        this(code, traceId, Map.of(), cause);
    }

    public DeepResearchApplicationException(ResearchErrorCodeEnum code, String traceId,
                                            Map<String, List<String>> fieldErrors, Throwable cause) {
        super(code == null ? ResearchErrorCodeEnum.RESEARCH_INTERNAL_ERROR.safeMessage() : code.safeMessage(), cause);
        this.code = code == null ? ResearchErrorCodeEnum.RESEARCH_INTERNAL_ERROR : code;
        this.traceId = traceId;
        this.fieldErrors = fieldErrors == null ? Map.of() : fieldErrors.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    public ResearchErrorCodeEnum code() {
        return code;
    }

    public String traceId() {
        return traceId;
    }

    public Map<String, List<String>> fieldErrors() {
        return fieldErrors;
    }
}
