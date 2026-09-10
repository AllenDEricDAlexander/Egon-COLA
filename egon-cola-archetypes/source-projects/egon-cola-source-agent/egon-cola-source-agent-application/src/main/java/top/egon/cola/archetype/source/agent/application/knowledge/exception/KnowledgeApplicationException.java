package top.egon.cola.archetype.source.agent.application.knowledge.exception;

import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Safe application failure carrying only the public knowledge error vocabulary.
 *
 * <p>The message is the code's fixed summary rather than the underlying failure's text: these
 * exceptions reach a response body, and a component or driver message could carry an endpoint, a key
 * or a fragment of the document.
 */
public class KnowledgeApplicationException extends RuntimeException {

    private final KnowledgeErrorCodeEnum code;
    private final String traceId;
    private final Map<String, List<String>> fieldErrors;

    public KnowledgeApplicationException(KnowledgeErrorCodeEnum code, String traceId) {
        this(code, traceId, Map.of(), null);
    }

    public KnowledgeApplicationException(KnowledgeErrorCodeEnum code, String traceId, Throwable cause) {
        this(code, traceId, Map.of(), cause);
    }

    public KnowledgeApplicationException(KnowledgeErrorCodeEnum code, String traceId,
                                         Map<String, List<String>> fieldErrors, Throwable cause) {
        super(code == null ? KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR.safeMessage() : code.safeMessage(), cause);
        this.code = code == null ? KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR : code;
        this.traceId = traceId;
        this.fieldErrors = fieldErrors == null ? Map.of() : fieldErrors.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    /** The same failure with one field named, for the validation responses that carry a field map. */
    public static KnowledgeApplicationException onField(KnowledgeErrorCodeEnum code, String traceId,
                                                        String field, String message, Throwable cause) {
        return new KnowledgeApplicationException(code, traceId, Map.of(field, List.of(message)), cause);
    }

    public KnowledgeErrorCodeEnum code() {
        return code;
    }

    public String traceId() {
        return traceId;
    }

    public Map<String, List<String>> fieldErrors() {
        return fieldErrors;
    }
}
