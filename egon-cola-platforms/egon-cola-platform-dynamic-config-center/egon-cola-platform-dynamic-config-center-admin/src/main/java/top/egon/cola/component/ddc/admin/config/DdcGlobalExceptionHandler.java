package top.egon.cola.component.ddc.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.component.ddc.error.DdcErrorStatus;

@RestControllerAdvice
public class DdcGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(
            DdcGlobalExceptionHandler.class
    );

    @ExceptionHandler(CommonException.class)
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
        log.debug("Invalid DDC request", exception);
        return ResultRecord.failure(DdcErrorStatus.INVALID_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResultRecord<Void> handleUnexpected(Exception exception) {
        log.error("Unexpected DDC request failure", exception);
        return ResultRecord.failure(DdcErrorStatus.INTERNAL_FAILURE);
    }
}
