package top.egon.cola.component.ddc.admin.config;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.result.factory.ResultDtos;
import top.egon.cola.component.ddc.common.DdcErrorStatus;
import top.egon.cola.component.ddc.common.DdcException;

@RestControllerAdvice
public class DdcGlobalExceptionHandler {

    @ExceptionHandler(DdcException.class)
    public ResultDto<Void> handleDdc(DdcException exception) {
        return ResultDtos.failure(exception);
    }

    @ExceptionHandler(Exception.class)
    public ResultDto<Void> handle(Exception exception) {
        return ResultDtos.failure(DdcErrorStatus.INTERNAL_FAILURE);
    }
}
