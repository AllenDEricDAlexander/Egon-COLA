package top.egon.cola.archetype.source.agent.adapter.knowledge.handler;

import top.egon.cola.component.common.core.exception.ExceptionLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.DefaultAuthorizationService.AuthorizationDeniedException;
import top.egon.cola.platform.tianquan.jianshen.starter.authorization.AuthorizationService.RuntimeUnavailableException;
import top.egon.cola.platform.tianquan.jianshen.starter.web.Rbac3AuthorizationExceptionHandler;
import top.egon.cola.platform.tianquan.shoubing.starter.security.IdpSecurityObservationFilter;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
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
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchTraceFilter;
import top.egon.cola.archetype.source.agent.adapter.handler.DeepResearchErrorResult;
import top.egon.cola.archetype.source.agent.common.exception.KnowledgeApplicationException;
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
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public class KnowledgeGlobalExceptionHandler {

    @Qualifier("rbac3AuthorizationExceptionHandler")
    private final Rbac3AuthorizationExceptionHandler securityErrors;

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthorization(AuthorizationDeniedException failure) {
        return confirmedDenial() ? securityErrors.denied(failure) : auditUnavailable();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleScopeOrFields(AccessDeniedException failure) {
        return confirmedDenial() ? securityErrors.fieldDenied(
                new org.springframework.security.authorization.AuthorizationDeniedException(failure.getMessage())) : auditUnavailable();
    }

    @ExceptionHandler(RuntimeUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityUnavailable(RuntimeUnavailableException failure) {
        return securityErrors.unavailable(failure);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException failure) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)
                || !IdpSecurityObservationFilter.beforeAuthenticationFailure(attributes.getRequest())) {
            return auditUnavailable();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header("Cache-Control", "no-store")
                .header("WWW-Authenticate", "Bearer").body(Map.of(
                        "code", "AUTHENTICATION_REQUIRED", "message", "Tianquan-Shoubing authentication failed"));
    }

    private static boolean confirmedDenial() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) { return false; }
        var marker = IdpSecurityObservationFilter.authorizationObservationContext(attributes.getRequest());
        return marker != null && marker.isActive() && IdpSecurityObservationFilter.beforeAuthorizationDenied(attributes.getRequest())
                && marker.isConfirmed();
    }

    private static ResponseEntity<Map<String, Object>> auditUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header("Cache-Control", "no-store").body(Map.of(
                "code", "OAUTH_AUDIT_DELIVERY_UNAVAILABLE", "message", "Tianquan-Jianshen authorization runtime unavailable"));
    }


    @Qualifier("agentClock")
    private final Clock clock;

    @ExceptionHandler(KnowledgeApplicationException.class)
    public ResponseEntity<DeepResearchErrorResult> handleApplication(
            KnowledgeApplicationException failure, HttpServletRequest request) {
        ExceptionLogger.log(log, failure);
        return response(failure.code(), failure.traceId(), failure.fieldErrors(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DeepResearchErrorResult> handleBodyValidation(
            MethodArgumentNotValidException failure, HttpServletRequest request) {
        Map<String, List<String>> fields = failure.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(error -> error.getField(), LinkedHashMap::new,
                        Collectors.mapping(error -> messageOf(error.getDefaultMessage()), Collectors.toList())));
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, null, fields, request);
    }

    /**
     * Controller parameter validation, which is what rejects a malformed path identifier or a page
     * size outside its range before the request reaches a use case.
     *
     * <p>Method validation also runs over a validated request body whenever the handler method
     * carries any parameter constraint of its own, and it reports a rejected body as one rejected
     * parameter. Such an error still names the property the caller sent, which is the vocabulary the
     * contract publishes, so it is preferred over the parameter name; a rejected simple value has no
     * property of its own and is reported under the wire name of the parameter that carried it.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<DeepResearchErrorResult> handleParameterValidation(
            HandlerMethodValidationException failure, HttpServletRequest request) {
        Map<String, List<String>> fields = new LinkedHashMap<>();
        for (ParameterValidationResult result : failure.getParameterValidationResults()) {
            String parameter = fieldName(result.getMethodParameter());
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                String field = error instanceof FieldError fieldError && !fieldError.getField().isBlank()
                        ? fieldError.getField()
                        : parameter;
                fields.computeIfAbsent(field, key -> new ArrayList<>())
                        .add(messageOf(error.getDefaultMessage()));
            }
        }
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, null, fields, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<DeepResearchErrorResult> handleConstraintValidation(
            ConstraintViolationException failure, HttpServletRequest request) {
        Map<String, List<String>> fields = failure.getConstraintViolations().stream()
                .collect(Collectors.groupingBy(violation -> lastSegment(violation.getPropertyPath().toString()),
                        LinkedHashMap::new,
                        Collectors.mapping(ConstraintViolation::getMessage, Collectors.toList())));
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, null, fields, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<DeepResearchErrorResult> handleTypeMismatch(
            MethodArgumentTypeMismatchException failure, HttpServletRequest request) {
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, null,
                Map.of(parameterName(failure.getParameter(), failure.getName()),
                        List.of("parameter has an unsupported value")), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<DeepResearchErrorResult> handleUnreadable(
            HttpMessageNotReadableException failure, HttpServletRequest request) {
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, null, Map.of(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<DeepResearchErrorResult> handleUnsupportedMedia(
            HttpMediaTypeNotSupportedException failure, HttpServletRequest request) {
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, null, Map.of(), request,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<DeepResearchErrorResult> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException failure, HttpServletRequest request) {
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_NOT_ACCEPTABLE, null, Map.of(), request);
    }

    /**
     * The container refusing a multipart body above its own threshold.
     *
     * <p>The use case owns the published limit and answers first for everything up to it; this mapping
     * only covers a body that is larger still, so that such an upload is still the contract's size
     * failure instead of an unexpected one. It reaches this advice only because the multipart request
     * is resolved lazily — an eagerly parsed one fails before a handler is known, and no knowledge
     * scoped advice can be selected for it.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<DeepResearchErrorResult> handleUploadTooLarge(
            MaxUploadSizeExceededException failure, HttpServletRequest request) {
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_FILE_TOO_LARGE, null,
                Map.of("file", List.of("the upload exceeds the size limit")), request);
    }

    /** A multipart body the container could not read at all, which is a malformed request. */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<DeepResearchErrorResult> handleMultipart(
            MultipartException failure, HttpServletRequest request) {
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_VALIDATION_ERROR, null, Map.of(), request);
    }

    /**
     * Last resort for this domain. Only the failure class is logged: a component message can carry an
     * endpoint, a key or a fragment of a document.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<DeepResearchErrorResult> handleUnexpected(
            Exception failure, HttpServletRequest request) {
        ExceptionLogger.log(log, failure);
        return response(KnowledgeErrorCodeEnum.KNOWLEDGE_INTERNAL_ERROR, null, Map.of(), request);
    }

    private ResponseEntity<DeepResearchErrorResult> response(KnowledgeErrorCodeEnum code, String traceId,
                                                               Map<String, List<String>> fieldErrors,
                                                               HttpServletRequest request) {
        return response(code, traceId, fieldErrors, request, status(code));
    }

    /**
     * Every knowledge failure is answered with the published JSON body, whatever the caller declared
     * it accepts.
     *
     * <p>The content type is set rather than negotiated: an answer endpoint that can only take an
     * event stream — which is exactly what the contract asks a caller of it to declare — would
     * otherwise be told its failure has no writable representation, and the failure decided before the
     * stream opened would be lost as a container error instead of the published code.
     */
    private ResponseEntity<DeepResearchErrorResult> response(KnowledgeErrorCodeEnum code, String traceId,
                                                               Map<String, List<String>> fieldErrors,
                                                               HttpServletRequest request, HttpStatus status) {
        String effectiveTrace = traceId == null || traceId.isBlank() ? traceId(request) : traceId;
        DeepResearchErrorResult body = new DeepResearchErrorResult(code.code(), code.safeMessage(),
                effectiveTrace, clock.instant(), fieldErrors);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
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
