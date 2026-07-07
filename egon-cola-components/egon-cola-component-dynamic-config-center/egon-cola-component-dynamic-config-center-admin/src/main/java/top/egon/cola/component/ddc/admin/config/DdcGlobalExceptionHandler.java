package top.egon.cola.component.ddc.admin.config;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.egon.cola.component.common.result.Result;

@RestControllerAdvice
public class DdcGlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result<Void> handle(Exception exception) {
        return Result.failure(exception);
    }
}
