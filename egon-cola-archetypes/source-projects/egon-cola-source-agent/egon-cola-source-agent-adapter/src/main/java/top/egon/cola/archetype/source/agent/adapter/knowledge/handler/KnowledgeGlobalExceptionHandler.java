package top.egon.cola.archetype.source.agent.adapter.knowledge.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchTraceFilter;
import top.egon.cola.archetype.source.agent.adapter.handler.DeepResearchErrorResponse;
import top.egon.cola.archetype.source.agent.application.knowledge.exception.KnowledgeApplicationException;
import top.egon.cola.archetype.source.agent.common.error.KnowledgeErrorCodeEnum;

import java.lang.annotation.Annotation;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Maps knowledge boundary failures to the published status and error contract (Spec B §9.2).
 *
 * <p>It is scoped to the knowledge packages and ordered first, so the application-wide advice for the
 * research endpoints stays the fallback for everything else. The distinction matters for the
 * exceptions both domains raise — an unreadable body or an unexpected failure — because only this
 * advice knows which vocabulary the request belongs to.
 *
 * <p>The body reuses the shared error record: one shape for every knowledge and research endpoint,
 * which is what the contract asks for (Spec B §9.2, `REQ-014`).
 */
@RestControllerAdvice(name = "knowledgeGlobalExceptionHandler",
        basePackages = "top.egon.cola.archetype.source.agent.adapter.knowledge")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class KnowledgeGlobalExceptionHandler {

    @Qualifier("agentClock")
    private final Clock clock;

    @ExceptionHandler(KnowledgeApplicationException.class)
    public ResponseEntity<DeepResearchErrorResponse> handleApplication(
            KnowledgeApplicationException failure, HttpServletRequest request) {
        return response(failure.code(), failure.traceId(), failure.fieldErrors(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DeepResearchErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException failure, HttpServletRequest request) {
        Map<String, List<String>> fields = failure.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(error -> error.getField(), LinkedHashMap::new,
                        Collectors.mapping(error -> messageOf(error.getDefaultMessage()), Collectors.toList())));
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, null, fields, request);
    }

    /**
     * Controller parameter validation, which is what rejects a malformed path identifier or a page
     * size outside its range before the request reaches a use case.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<DeepResearchErrorResponse> handleParameterValidation(
            HandlerMethodValidationException failure, HttpServletRequest request) {
        Map<String, List<String>> fields = new LinkedHashMap<>();
        for (ParameterValidationResult result : failure.getParameterValidationResults()) {
            String field = fieldName(result.getMethodParameter());
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                fields.computeIfAbsent(field, key -> new ArrayList<>())
                        .add(messageOf(error.getDefaultMessage()));
            }
        }
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, null, fields, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<DeepResearchErrorResponse> handleConstraintValidation(
            ConstraintViolationException failure, HttpServletRequest request) {
        Map<String, List<String>> fields = failure.getConstraintViolations().stream()
                .collect(Collectors.groupingBy(violation -> lastSegment(violation.getPropertyPath().toString()),
                        LinkedHashMap::new,
                        Collectors.mapping(ConstraintViolation::getMessage, Collectors.toList())));
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, null, fields, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<DeepResearchErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException failure, HttpServletRequest request) {
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, null,
                Map.of(parameterName(failure.getParameter(), failure.getName()),
                        List.of("parameter has an unsupported value")), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<DeepResearchErrorResponse> handleUnreadable(
            HttpMessageNotReadableException failure, HttpServletRequest request) {
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, null, Map.of(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<DeepResearchErrorResponse> handleUnsupportedMedia(
            HttpMediaTypeNotSupportedException failure, HttpServletRequest request) {
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, null, Map.of(), request,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<DeepResearchErrorResponse> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException failure, HttpServletRequest request) {
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_NOT_ACCEPTABLE, null, Map.of(), request);
    }

    /**
     * Last resort for this domain. Only the failure class is logged: a component message can carry an
     * endpoint, a key or a fragment of a document.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<DeepResearchErrorResponse> handleUnexpected(
            Exception failure, HttpServletRequest request) {
        log.error("knowledge request failed outcome=REJECTED code={} exception={}",
                KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR.code(), failure.getClass().getSimpleName());
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, null, Map.of(), request);
    }

    private ResponseEntity<DeepResearchErrorResponse> response(KnowledgeErrorCodeEnum code, String traceId,
                                                               Map<String, List<String>> fieldErrors,
                                                               HttpServletRequest request) {
        return response(code, traceId, fieldErrors, request, status(code));
    }

    private ResponseEntity<DeepResearchErrorResponse> response(KnowledgeErrorCodeEnum code, String traceId,
                                                               Map<String, List<String>> fieldErrors,
                                                               HttpServletRequest request, HttpStatus status) {
        String effectiveTrace = traceId == null || traceId.isBlank() ? traceId(request) : traceId;
        DeepResearchErrorResponse body = new DeepResearchErrorResponse(code.code(), code.safeMessage(),
                effectiveTrace, clock.instant(), fieldErrors);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status)
                .header(ResearchTraceFilter.TRACE_HEADER, effectiveTrace);
        if (code == KnowledgeErrorCodeEnum.KNOWLEDGE_CAPACITY_EXHAUSTED) {
            // The published contract promises the caller a moment to wait before retrying.
            builder.header(HttpHeaders.RETRY_AFTER, "5");
        }
        return builder.body(body);
    }

    private static HttpStatus status(KnowledgeErrorCodeEnum code) {
        return switch (code) {
            case KNOWLEDGE_VALIDATION_ERROR, KNOWLEDGE_MODEL_NOT_REGISTERED, KNOWLEDGE_IMMUTABLE_FIELD,
                 KNOWLEDGE_EXTRACTOR_MISSING -> HttpStatus.BAD_REQUEST;
            case KNOWLEDGE_BASE_NOT_FOUND, KNOWLEDGE_DOCUMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case KNOWLEDGE_NOT_ACCEPTABLE -> HttpStatus.NOT_ACCEPTABLE;
            case KNOWLEDGE_BASE_CODE_CONFLICT, KNOWLEDGE_BASE_BUSY, KNOWLEDGE_DOCUMENT_BUSY,
                 KNOWLEDGE_CONTENT_MISSING -> HttpStatus.CONFLICT;
            case KNOWLEDGE_FILE_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case KNOWLEDGE_CAPACITY_EXHAUSTED -> HttpStatus.TOO_MANY_REQUESTS;
            case KNOWLEDGE_DEPENDENCY_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case KNOWLEDGE_EMBEDDING_FAILED, KNOWLEDGE_INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /** The field a rejected parameter belongs to: the wire name a caller can act on. */
    private static String fieldName(MethodParameter parameter) {
        return parameterName(parameter, parameter.getParameterName());
    }

    private static String parameterName(MethodParameter parameter, String fallback) {
        for (Annotation annotation : parameter.getParameterAnnotations()) {
            if (annotation instanceof PathVariable pathVariable && !pathVariable.value().isBlank()) {
                return pathVariable.value();
            }
            if (annotation instanceof RequestParam requestParam && !requestParam.value().isBlank()) {
                return requestParam.value();
            }
        }
        return fallback == null ? "arg" + parameter.getParameterIndex() : fallback;
    }

    private static String messageOf(String message) {
        return message == null ? "invalid" : message;
    }

    private static String lastSegment(String path) {
        int separator = path.lastIndexOf('.');
        return separator < 0 ? path : path.substring(separator + 1);
    }

    private static String traceId(HttpServletRequest request) {
        Object attribute = request.getAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE);
        if (attribute != null && !attribute.toString().isBlank()) {
            return attribute.toString();
        }
        String header = request.getHeader(ResearchTraceFilter.TRACE_HEADER);
        return header == null || header.isBlank() ? UUID.randomUUID().toString() : header.trim();
    }
}
