package top.egon.cola.archetype.source.light.adapter.handler;

import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.common.core.enums.ResultCode;
import top.egon.cola.component.common.core.pojo.ResultRecord;
import top.egon.cola.archetype.source.light.common.exception.TeachingUseCaseException;
import top.egon.cola.archetype.source.light.common.exception.UserUseCaseException;
import jakarta.validation.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(UserUseCaseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultRecord<Void> handleUserFailure(UserUseCaseException exception) {
        return ResultRecord.failure(exception);
    }

    @ExceptionHandler(TeachingUseCaseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultRecord<Void> handleTeachingFailure(TeachingUseCaseException exception) {
        return ResultRecord.failure(exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultRecord<Void> handleValidationFailure(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("request validation failed");
        return ResultRecord.failure(ResultCode.VALIDATION_ERROR.getCode(), ResultCode.VALIDATION_ERROR.getStatus(), message);
    }

    @ExceptionHandler({ValidationException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultRecord<Void> handleValidationFailure(RuntimeException exception) {
        return ResultRecord.failure(ResultCode.VALIDATION_ERROR.getCode(), ResultCode.VALIDATION_ERROR.getStatus(), exception.getMessage());
    }
}
