package top.egon.cola.archetype.source.agent.common.exception;

import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Safe application failure carrying only the public knowledge error vocabulary.
 *
 * <p>The message is the code's fixed summary rather than the underlying failure's text: these
 * exceptions reach a response body, and a component or driver message could carry an endpoint, a key
 * or a fragment of the document. The stable String code stays published through {@link #getStatus()}.
 */
public class KnowledgeApplicationException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

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
        super(ResultCode.BUSINESS_ERROR.getCode(), normalized(code).getStatus(),
                normalized(code).safeMessage(), cause);
        this.code = normalized(code);
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

    /** @return the typed vocabulary, so a caller keeps the retry flag instead of a String */
    public KnowledgeErrorCodeEnum code() {
        return code;
    }

    public String traceId() {
        return traceId;
    }

    public Map<String, List<String>> fieldErrors() {
        return fieldErrors;
    }

    private static KnowledgeErrorCodeEnum normalized(KnowledgeErrorCodeEnum code) {
        return code == null ? KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR : code;
    }
}
