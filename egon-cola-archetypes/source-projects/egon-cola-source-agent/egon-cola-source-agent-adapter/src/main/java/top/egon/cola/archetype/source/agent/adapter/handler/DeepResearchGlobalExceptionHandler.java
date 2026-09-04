package top.egon.cola.archetype.source.agent.adapter.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.egon.cola.archetype.source.agent.adapter.filter.ResearchTraceFilter;
import top.egon.cola.archetype.source.agent.adapter.research.converter.DeepResearchErrorConverter;
import top.egon.cola.archetype.source.agent.application.research.exception.DeepResearchApplicationException;
import top.egon.cola.archetype.source.agent.common.error.ResearchErrorCodeEnum;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Maps only typed, pre-stream failures to the public status and error contract. */
@RestControllerAdvice(name = "deepResearchGlobalExceptionHandler")
@RequiredArgsConstructor
@Slf4j
public class DeepResearchGlobalExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(DeepResearchApplicationException.class)
    public ResponseEntity<DeepResearchErrorResponse> handleApplication(
            DeepResearchApplicationException failure, HttpServletRequest request) {
        return response(failure.code(), failure.traceId(), failure.fieldErrors(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DeepResearchErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException failure, HttpServletRequest request) {
        Map<String, List<String>> fields = failure.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(error -> error.getField(), LinkedHashMap::new,
                        Collectors.mapping(error -> error.getDefaultMessage() == null
                                        ? "invalid" : error.getDefaultMessage(), Collectors.toList())));
        return response(ResearchErrorCodeEnum.RESEARCH_VALIDATION_ERROR, null, fields, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<DeepResearchErrorResponse> handleConstraintValidation(
            ConstraintViolationException failure, HttpServletRequest request) {
        Map<String, List<String>> fields = failure.getConstraintViolations().stream()
                .collect(Collectors.groupingBy(this::fieldName, LinkedHashMap::new,
                        Collectors.mapping(ConstraintViolation::getMessage, Collectors.toList())));
        return response(ResearchErrorCodeEnum.RESEARCH_VALIDATION_ERROR, null, fields, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<DeepResearchErrorResponse> handleUnreadable(
            HttpMessageNotReadableException failure, HttpServletRequest request) {
        return response(ResearchErrorCodeEnum.RESEARCH_VALIDATION_ERROR, null, Map.of(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<DeepResearchErrorResponse> handleUnsupportedMedia(
            HttpMediaTypeNotSupportedException failure, HttpServletRequest request) {
        return response(ResearchErrorCodeEnum.RESEARCH_UNSUPPORTED_MEDIA_TYPE, null, Map.of(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<DeepResearchErrorResponse> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException failure, HttpServletRequest request) {
        return response(ResearchErrorCodeEnum.RESEARCH_NOT_ACCEPTABLE, null, Map.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DeepResearchErrorResponse> handleUnexpected(
            Exception failure, HttpServletRequest request) {
        return response(ResearchErrorCodeEnum.RESEARCH_INTERNAL_ERROR, null, Map.of(), request);
    }

    private ResponseEntity<DeepResearchErrorResponse> response(ResearchErrorCodeEnum code, String traceId,
                                                                Map<String, List<String>> fieldErrors,
                                                                HttpServletRequest request) {
        String effectiveTrace = traceId == null || traceId.isBlank() ? traceId(request) : traceId;
        DeepResearchApplicationException failure = new DeepResearchApplicationException(
                code, effectiveTrace, fieldErrors, null);
        DeepResearchErrorResponse body = DeepResearchErrorConverter.INSTANCE.toResponse(failure, clock.instant());
        HttpStatus status = status(code);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status)
                .header(ResearchTraceFilter.TRACE_HEADER, effectiveTrace);
        if (code == ResearchErrorCodeEnum.RESEARCH_UNAUTHORIZED) {
            builder.header(HttpHeaders.WWW_AUTHENTICATE, "ApiKey");
        } else if (code == ResearchErrorCodeEnum.RESEARCH_CAPACITY_EXHAUSTED) {
            builder.header(HttpHeaders.RETRY_AFTER, "5");
        }
        return builder.body(body);
    }

    private static HttpStatus status(ResearchErrorCodeEnum code) {
        return switch (code) {
            case RESEARCH_VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
            case RESEARCH_UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case RESEARCH_NOT_ACCEPTABLE -> HttpStatus.NOT_ACCEPTABLE;
            case RESEARCH_UNSUPPORTED_MEDIA_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case RESEARCH_CAPACITY_EXHAUSTED -> HttpStatus.TOO_MANY_REQUESTS;
            case RESEARCH_DEPENDENCY_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case RESEARCH_INTERNAL_ERROR, RESEARCH_TIMEOUT -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static String traceId(HttpServletRequest request) {
        Object attribute = request.getAttribute(ResearchTraceFilter.TRACE_ATTRIBUTE);
        if (attribute != null && !attribute.toString().isBlank()) {
            return attribute.toString();
        }
        String header = request.getHeader(ResearchTraceFilter.TRACE_HEADER);
        return header == null || header.isBlank() ? UUID.randomUUID().toString() : header.trim();
    }

    private String fieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int separator = path.lastIndexOf('.');
        return separator < 0 ? path : path.substring(separator + 1);
    }
}
