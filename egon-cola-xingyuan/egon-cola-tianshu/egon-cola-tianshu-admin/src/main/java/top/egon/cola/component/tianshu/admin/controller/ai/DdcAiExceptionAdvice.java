package top.egon.cola.component.tianshu.admin.controller.ai;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.tianshu.admin.config.DdcAiWebConfiguration;

@Slf4j
@RestControllerAdvice(assignableTypes = DdcAiResourceController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DdcAiExceptionAdvice {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ResultRecord<Void>> handleCommon(CommonException exception) {
        return response(httpStatus(exception.getCode()), ResultRecord.failure(exception));
    }

    @ExceptionHandler(DdcAiWebConfiguration.RequestBodyTooLargeException.class)
    public ResponseEntity<ResultRecord<Void>> handleBodyTooLarge(
            DdcAiWebConfiguration.RequestBodyTooLargeException exception) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE,
                ResultRecord.failure(56312, "TIANSHU_AI_REQUEST_TOO_LARGE", "AI request body is too large"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ResultRecord<Void>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ResultRecord.failure(56301, "TIANSHU_AI_VALIDATION_ERROR", "Unsupported AI request media type"));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class,
            MissingPathVariableException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            BindException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ResultRecord<Void>> handleInvalidRequest(Exception exception) {
        log.debug("Invalid Tianshu AI request type={}", exception.getClass().getSimpleName());
        return response(HttpStatus.BAD_REQUEST,
                ResultRecord.failure(56301, "TIANSHU_AI_VALIDATION_ERROR", "Invalid AI request"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultRecord<Void>> handleUnexpected(Exception exception) {
        log.error("Tianshu AI REST request failed type={}", exception.getClass().getSimpleName());
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                ResultRecord.failure(56999, "TIANSHU_INTERNAL_FAILURE", "Tianshu internal failure"));
    }

    private HttpStatus httpStatus(int code) {
        return switch (code) {
            case 56301, 56308, 56320, 56321 -> HttpStatus.BAD_REQUEST;
            case 56302, 56306, 56325 -> HttpStatus.NOT_FOUND;
            case 56307 -> HttpStatus.FORBIDDEN;
            case 56303, 56304, 56305, 56311, 56314, 56323, 56324 -> HttpStatus.CONFLICT;
            case 56312 -> HttpStatus.PAYLOAD_TOO_LARGE;
            case 56310, 56313, 56322 -> HttpStatus.SERVICE_UNAVAILABLE;
            case 56309, 56999 -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private <T> ResponseEntity<T> response(HttpStatus status, T body) {
        return ResponseEntity.status(status).header("Cache-Control", "no-store").body(body);
    }
}
