package top.egon.cola.component.ddc.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.egon.cola.component.common.core.exception.EgonException;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.result.factory.ResultDtos;
import top.egon.cola.component.ddc.common.DdcErrorStatus;

@RestControllerAdvice
public class DdcGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(
            DdcGlobalExceptionHandler.class
    );

    @ExceptionHandler(EgonException.class)
    public ResultDto<Void> handleEgon(EgonException exception) {
        return ResultDtos.failure(exception);
    }

    @ExceptionHandler(Exception.class)
    public ResultDto<Void> handleUnexpected(Exception exception) {
        log.error("Unexpected DDC request failure", exception);
        return ResultDtos.failure(DdcErrorStatus.INTERNAL_FAILURE);
    }
}
