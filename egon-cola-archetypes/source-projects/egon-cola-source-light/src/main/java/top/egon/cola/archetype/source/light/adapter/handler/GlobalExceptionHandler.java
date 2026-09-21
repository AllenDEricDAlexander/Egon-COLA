package top.egon.cola.archetype.source.light.adapter.handler;

import lombok.extern.slf4j.Slf4j;
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
    public ApiResponse<Void> handleUserFailure(UserUseCaseException exception) {
        return ApiResponse.failure(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler(TeachingUseCaseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleTeachingFailure(TeachingUseCaseException exception) {
        return ApiResponse.failure(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationFailure(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("request validation failed");
        return ApiResponse.failure("VALIDATION_ERROR", message);
    }

    @ExceptionHandler({ValidationException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationFailure(RuntimeException exception) {
        return ApiResponse.failure("VALIDATION_ERROR", exception.getMessage());
    }
}
