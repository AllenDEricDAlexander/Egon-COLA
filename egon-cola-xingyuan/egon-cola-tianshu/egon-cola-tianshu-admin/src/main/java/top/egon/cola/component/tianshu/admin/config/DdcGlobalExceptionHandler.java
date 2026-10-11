package top.egon.cola.component.tianshu.admin.config;

import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.tianshu.error.DdcErrorStatus;

@Slf4j
@RestControllerAdvice(name = "ddcGlobalExceptionHandler")
public class DdcGlobalExceptionHandler {

    @ExceptionHandler(CommonException.class)
    public ResultRecord<Void> handleCommon(CommonException exception, HttpServletResponse response) {
        if (DdcErrorStatus.RESOURCE_ADMISSION_UNAVAILABLE.getStatus().equals(exception.getStatus())) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
        return handleCommon(exception);
    }

    /** Retains the original direct Java error projection. */
    public ResultRecord<Void> handleCommon(CommonException exception) {
        return ResultRecord.failure(exception);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            TypeMismatchException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class,
            BindException.class,
            IllegalArgumentException.class
    })
    public ResultRecord<Void> handleInvalidRequest(Exception exception) {
        log.debug("Invalid Tianshu request", exception);
        return ResultRecord.failure(DdcErrorStatus.INVALID_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResultRecord<Void> handleUnexpected(Exception exception) {
        log.error("Unexpected Tianshu request failure", exception);
        return ResultRecord.failure(DdcErrorStatus.INTERNAL_FAILURE);
    }
}
