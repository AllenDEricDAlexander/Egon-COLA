package top.egon.fable.web.adapter.handler;

import top.egon.fable.web.common.constants.ErrorCodes;
import top.egon.fable.web.common.exceptions.BizException;
import top.egon.fable.web.common.response.Response;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(name = "globalExceptionHandler")
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public Response handleBizException(BizException exception) {
        return Response.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        return Response.fail(ErrorCodes.VALIDATION_ERROR, "validation error");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Response handleConstraintViolationException(ConstraintViolationException exception) {
        return Response.fail(ErrorCodes.VALIDATION_ERROR, "validation error");
    }
}
