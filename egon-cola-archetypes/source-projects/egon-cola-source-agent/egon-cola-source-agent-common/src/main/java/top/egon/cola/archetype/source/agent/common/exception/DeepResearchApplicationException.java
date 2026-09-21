package top.egon.cola.archetype.source.agent.common.exception;

import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.exception.BusinessException;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Safe application failure carrying only the public research error vocabulary.
 *
 * <p>The stable String code stays published through {@link #getStatus()}, so the wire error body is
 * unchanged by the common hierarchy.
 */
public class DeepResearchApplicationException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

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
        super(ResultCode.BUSINESS_ERROR.getCode(), normalized(code).getStatus(),
                normalized(code).safeMessage(), cause);
        this.code = normalized(code);
        this.traceId = traceId;
        this.fieldErrors = fieldErrors == null ? Map.of() : fieldErrors.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    /** @return the typed vocabulary, so a caller keeps the retry flag instead of a String */
    public ResearchErrorCodeEnum code() {
        return code;
    }

    public String traceId() {
        return traceId;
    }

    public Map<String, List<String>> fieldErrors() {
        return fieldErrors;
    }

    private static ResearchErrorCodeEnum normalized(ResearchErrorCodeEnum code) {
        return code == null ? ResearchErrorCodeEnum.RESEARCH_INTERNAL_ERROR : code;
    }
}
