package top.egon.cola.component.ddc.admin.config;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.egon.cola.component.common.result.dto.ResultDto;
import top.egon.cola.component.common.result.factory.ResultDtos;

@RestControllerAdvice
public class DdcGlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResultDto<Void> handle(Exception exception) {
        return ResultDtos.failure(exception);
    }
}
